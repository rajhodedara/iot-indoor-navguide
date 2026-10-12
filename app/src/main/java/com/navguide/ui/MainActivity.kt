package com.navguide.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.ble.BlePermissions
import com.navguide.core.MapDoc
import com.navguide.core.MapNode
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

        findViewById<TextView>(R.id.tvMapName).text = doc.name
        buildDestinations()

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
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
        val rooms = findViewById<LinearLayout>(R.id.llRooms)
        val access = findViewById<LinearLayout>(R.id.llAccess)
        doc.destinationNodes.sortedBy { it.name }.forEach { node ->
            addDestinationRow(if (node.type == "room") rooms else access, node)
        }
    }

    private fun addDestinationRow(parent: LinearLayout, node: MapNode) {
        val row = layoutInflater.inflate(R.layout.row_destination, parent, false)
        row.findViewById<ImageView>(R.id.ivIcon).setImageResource(iconFor(node.type))
        row.findViewById<TextView>(R.id.tvName).text = doc.displayName(node)
        row.findViewById<TextView>(R.id.tvType).text = typeLabel(node.type)
        row.contentDescription = "Navigate to ${doc.displayName(node)}"
        row.setOnClickListener {
            startActivity(
                Intent(this, NavigationActivity::class.java)
                    .putExtra(NavigationActivity.EXTRA_DEST, node.id)
            )
        }
        parent.addView(row)
    }

    private fun iconFor(type: String): Int = when (type) {
        "entrance" -> R.drawable.ic_entrance
        "exit" -> R.drawable.ic_exit
        "stairs" -> R.drawable.ic_stairs
        "elevator" -> R.drawable.ic_elevator
        else -> R.drawable.ic_room
    }

    private fun typeLabel(type: String): String = when (type) {
        "entrance" -> getString(R.string.type_entrance)
        "exit" -> getString(R.string.type_exit)
        "stairs" -> getString(R.string.type_stairs)
        "elevator" -> getString(R.string.type_elevator)
        else -> getString(R.string.type_room)
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
}
