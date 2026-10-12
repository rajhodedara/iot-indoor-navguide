package com.navguide.ui

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.navguide.R
import com.navguide.ble.BeaconBus
import com.navguide.ble.BleScannerService
import com.navguide.data.MapRepository
import com.navguide.data.Prefs
import com.navguide.guidance.HapticsImpl
import com.navguide.guidance.NavigationSession
import com.navguide.guidance.SpeakerTts
import com.navguide.positioning.StepDetector
import kotlinx.coroutines.launch

class NavigationActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_DEST = "extra_dest"
        private const val AUTO_WALK_INTERVAL_MS = 450L
    }

    private lateinit var session: NavigationSession
    private lateinit var speaker: SpeakerTts
    private val stepDetector = StepDetector()
    private lateinit var sensorManager: SensorManager
    private lateinit var tvInstruction: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvBluetooth: TextView

    private val autoWalkHandler = Handler(Looper.getMainLooper())
    private var autoWalking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val doc = MapRepository.load(this)
        val prefs = Prefs(this)
        speaker = SpeakerTts(this).also { it.speed = prefs.ttsSpeed }
        val haptics = HapticsImpl(this)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvLocation = findViewById(R.id.tvLocation)
        tvBluetooth = findViewById(R.id.tvBluetooth)

        session = NavigationSession(
            doc = doc,
            strideM = prefs.strideM.toDouble(),
            speaker = speaker,
            haptics = haptics,
            onStatus = { text ->
                runOnUiThread {
                    tvInstruction.text = text
                    tvLocation.text = "Near: " + (session.currentLocationName ?: "unknown")
                }
            },
            onArrived = {
                runOnUiThread { stopAutoWalk() }
            }
        )

        val destId = intent.getStringExtra(EXTRA_DEST)
        if (destId == null) {
            finish()
            return
        }
        (findViewById<TextView>(R.id.tvRouteTo)).text = "Destination: " + (doc.nodeById[destId]?.name ?: "")

        findViewById<Button>(R.id.btnRepeat).setOnClickListener { session.repeatLastInstruction() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopAutoWalk()
            session.stop()
            finish()
        }

        // Demo controls: simulate beacons and walking on one phone (no hardware needed).
        val simNodes = doc.nodes.sortedBy { it.name }
        val spinner = findViewById<Spinner>(R.id.spSimNode)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            simNodes.map { doc.displayName(it) }
        )
        findViewById<Button>(R.id.btnSimHop).setOnClickListener {
            val idx = spinner.selectedItemPosition
            if (idx in simNodes.indices) {
                session.onNodeHop(simNodes[idx].id)
            }
        }
        findViewById<Button>(R.id.btnSimSteps).setOnClickListener {
            repeat(10) { session.onStep() }
        }
        findViewById<Button>(R.id.btnAutoWalk).setOnClickListener {
            if (autoWalking) stopAutoWalk() else startAutoWalk()
        }

        lifecycleScope.launch { BeaconBus.readings.collect { r -> session.onReading(r) } }

        session.start(destId)
        if (session.currentLocationName == null) {
            tvInstruction.text = getString(R.string.nav_waiting_hint)
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        BleScannerService.start(this)
    }

    /** Demo: walks the whole route automatically, ticking real guidance logic at a human pace. */
    private fun startAutoWalk() {
        if (!session.startAutoTour()) return
        autoWalking = true
        val btn = findViewById<Button>(R.id.btnAutoWalk)
        btn.text = getString(R.string.btn_auto_walk_stop)
        autoWalkHandler.post(object : Runnable {
            override fun run() {
                if (!autoWalking) return
                if (!session.active) {
                    stopAutoWalk()
                    return
                }
                session.demoTick()
                autoWalkHandler.postDelayed(this, AUTO_WALK_INTERVAL_MS)
            }
        })
    }

    private fun stopAutoWalk() {
        autoWalking = false
        autoWalkHandler.removeCallbacksAndMessages(null)
        runOnUiThread {
            findViewById<Button>(R.id.btnAutoWalk).text = getString(R.string.btn_auto_walk)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        tvBluetooth.text = when {
            adapter == null -> "Bluetooth: not available on this device"
            !adapter.isEnabled -> "Bluetooth: OFF — turn it on to detect real beacons"
            else -> "Bluetooth: on — scanning for beacons"
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val stepped = stepDetector.onSample(
                event.values[0].toDouble(),
                event.values[1].toDouble(),
                event.values[2].toDouble(),
                System.currentTimeMillis()
            )
            if (stepped) session.onStep()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoWalk()
        session.stop()
        speaker.shutdown()
        BleScannerService.stop(this)
    }
}
