package com.navguide.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.navguide.feedback.AudioManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BleScanner : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Using a singleton or dependency injection would be better for AudioManager,
    // but we'll instantiate it or assume it's passed via some locator pattern.
    private lateinit var audioManager: AudioManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "BleScannerChannel"
        
        private val _beaconFlow = MutableSharedFlow<BeaconData>(extraBufferCapacity = 10)
        val beaconFlow = _beaconFlow.asSharedFlow()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            parseIBeacon(result)?.let { beacon ->
                _beaconFlow.tryEmit(beacon)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        audioManager = AudioManager(this)
        
        acquireWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startScanning()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        wakeLock?.release()
        audioManager.speak("Navigation service was stopped unexpectedly. Please restart the app.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NavGuide::BleScannerWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun startScanning() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bluetoothLeScanner?.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            // Handle missing permissions
            audioManager.speak("Bluetooth permissions are missing.")
        }
    }

    private fun stopScanning() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // Handle missing permissions
        }
    }

    private fun parseIBeacon(result: ScanResult): BeaconData? {
        val scanRecord = result.scanRecord?.bytes ?: return null
        var startByte = 2
        var patternFound = false
        while (startByte <= 5) {
            if ((scanRecord[startByte + 2].toInt() and 0xff) == 0x02 &&
                (scanRecord[startByte + 3].toInt() and 0xff) == 0x15) {
                patternFound = true
                break
            }
            startByte++
        }

        if (!patternFound) return null

        val uuidBytes = scanRecord.copyOfRange(startByte + 4, startByte + 20)
        val hexString = uuidBytes.joinToString("") { String.format("%02X", it) }
        val uuid = "${hexString.substring(0,8)}-${hexString.substring(8,12)}-${hexString.substring(12,16)}-${hexString.substring(16,20)}-${hexString.substring(20,32)}"
        
        val major = (scanRecord[startByte + 20].toInt() and 0xff) * 0x100 + (scanRecord[startByte + 21].toInt() and 0xff)
        val minor = (scanRecord[startByte + 22].toInt() and 0xff) * 0x100 + (scanRecord[startByte + 23].toInt() and 0xff)
        val txPower = scanRecord[startByte + 24].toInt()
        val rssi = result.rssi
        
        val distance = calculateDistance(txPower, rssi)

        return BeaconData(uuid, major, minor, rssi, distance)
    }

    private fun calculateDistance(txPower: Int, rssi: Int): Double {
        if (rssi == 0) return -1.0
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            (0.89976) * Math.pow(ratio, 7.7095) + 0.111
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Navigation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Navigation Active")
            .setContentText("Scanning for beacons to guide you")
            // .setSmallIcon(R.mipmap.ic_launcher) // Need to define R
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
