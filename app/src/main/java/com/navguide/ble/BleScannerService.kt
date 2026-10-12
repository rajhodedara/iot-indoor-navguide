package com.navguide.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that performs ONE long-lived BLE scan while navigation is active.
 * Avoids Android 14+ scan-throttling caused by start/stop churn.
 */
class BleScannerService : Service() {

    private var scanner: BluetoothLeScanner? = null
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord ?: return
            var reading = record.getManufacturerSpecificData(IBeacon.COMPANY_ID)
                ?.let { IBeacon.parseManufacturerData(it, result.rssi) }
            if (reading == null) {
                val arr = record.manufacturerSpecificData
                if (arr != null) {
                    for (i in 0 until arr.size()) {
                        reading = IBeacon.parseManufacturerData(arr.valueAt(i), result.rssi)
                        if (reading != null) break
                    }
                }
            }
            if (reading != null) BeaconBus.readings.tryEmit(reading)
        }

        override fun onScanFailed(errorCode: Int) {
            // Leave the service running; a scan failure usually clears itself.
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasScanPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }
            )
        } catch (_: Exception) {
            // Foreground promotion rejected (background start restrictions). Non-fatal.
            stopSelf()
            return START_NOT_STICKY
        }
        startScan()
        return START_NOT_STICKY
    }

    private fun hasScanPermission(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        return needed
    }

    private fun startScan() {
        try {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter ?: run { stopSelf(); return }
            if (!adapter.isEnabled) return
            scanner = adapter.bluetoothLeScanner ?: run { stopSelf(); return }
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner?.startScan(null, settings, callback)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { scanner?.stopScan(callback) } catch (_: SecurityException) { }
    }

    private fun buildNotification(): Notification {
        val channelId = "navguide_scan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Beacon scanning", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(com.navguide.R.string.app_name))
            .setContentText("Scanning for beacons")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 41

        fun start(context: Context) {
            val intent = Intent(context, BleScannerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Foreground service start rejected (e.g. background start) — navigation still
                // works with the simulator; the failure is non-fatal.
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, BleScannerService::class.java))
            } catch (_: Exception) { }
        }
    }
}
