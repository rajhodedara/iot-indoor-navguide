package com.navguide.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.ble.BlePermissions
import com.navguide.core.MapDoc
import com.navguide.data.MapRepository
import com.navguide.data.Prefs
import com.navguide.guidance.HapticsImpl
import com.navguide.guidance.SpeakerTts
import com.navguide.sos.SosController

class MainActivity : AppCompatActivity() {

    private lateinit var doc: MapDoc
    private lateinit var prefs: Prefs
    private lateinit var speaker: SpeakerTts
    private lateinit var sos: SosController

    private val volumeDownTimes = mutableListOf<Long>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doc = MapRepository.load(this)
        prefs = Prefs(this)
        speaker = SpeakerTts(this).also { it.speed = prefs.ttsSpeed }
        sos = SosController(this, speaker, HapticsImpl(this))

        buildDestinations()

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnBeaconMode).setOnClickListener {
            startActivity(Intent(this, BeaconModeActivity::class.java))
        }
        findViewById<Button>(R.id.btnSos).setOnLongClickListener {
            sos.trigger(prefs.emergencyContact, null)
            true
        }

        ensurePermissions()
    }

    private fun ensurePermissions() {
        if (BlePermissions.hasAll(this)) return
        AlertDialog.Builder(this)
            .setTitle(R.string.perm_title)
            .setMessage(R.string.perm_rationale)
            .setPositiveButton(R.string.btn_grant) { _, _ ->
                permissionLauncher.launch(BlePermissions.required())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildDestinations() {
        val container = findViewById<LinearLayout>(R.id.llDestinations)
        doc.destinationNodes.sortedBy { it.name }.forEach { node ->
            val b = Button(this)
            b.text = node.name
            b.textSize = 22f
            b.contentDescription = "Navigate to ${node.name}"
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72))
            lp.bottomMargin = dp(12)
            b.layoutParams = lp
            b.setOnClickListener {
                startActivity(
                    Intent(this, NavigationActivity::class.java)
                        .putExtra(NavigationActivity.EXTRA_DEST, node.id)
                )
            }
            container.addView(b)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event?.repeatCount == 0) {
            val now = System.currentTimeMillis()
            volumeDownTimes.add(now)
            volumeDownTimes.removeAll { now - it > 1500 }
            if (volumeDownTimes.size >= 3) {
                volumeDownTimes.clear()
                sos.trigger(prefs.emergencyContact, null)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        sos.stopAlarm()
        speaker.shutdown()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
