package com.navguide.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.ble.BeaconEmitter
import com.navguide.core.MapNode
import com.navguide.data.MapRepository

class BeaconModeActivity : AppCompatActivity() {

    private lateinit var emitter: BeaconEmitter
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private var selected: MapNode? = null
    private var selectedRow: View? = null
    private var broadcasting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_mode)
        emitter = BeaconEmitter(this)
        status = findViewById(R.id.tvBeaconStatus)
        toggle = findViewById(R.id.btnToggleBroadcast)

        val doc = MapRepository.load(this)
        val container = findViewById<LinearLayout>(R.id.llBeaconList)

        if (!emitter.isSupported()) {
            status.text = getString(R.string.beacon_not_supported)
            toggle.isEnabled = false
        } else {
            status.text = getString(R.string.beacon_status_idle)
        }

        doc.nodes.filter { it.beaconMinor != null }.sortedBy { it.beaconMinor }.forEach { node ->
            val row = layoutInflater.inflate(R.layout.row_beacon, container, false)
            row.findViewById<TextView>(R.id.tvBeaconName).text = doc.displayName(node)
            row.findViewById<TextView>(R.id.tvBeaconMinor).text = "Beacon minor ${node.beaconMinor}"
            row.contentDescription = "Broadcast as ${doc.displayName(node)}"
            row.setOnClickListener {
                if (broadcasting) return@setOnClickListener
                selectedRow?.setBackgroundResource(R.drawable.bg_row)
                row.setBackgroundResource(R.drawable.bg_row_selected)
                selectedRow = row
                selected = node
                status.text = "Selected: ${doc.displayName(node)}"
            }
            container.addView(row)
        }

        toggle.setOnClickListener {
            if (broadcasting) {
                emitter.stop()
                broadcasting = false
                toggle.text = getString(R.string.btn_start_broadcast)
                status.text = getString(R.string.beacon_status_idle)
            } else {
                val node = selected
                if (node == null || node.beaconMinor == null) {
                    status.text = getString(R.string.beacon_pick)
                    return@setOnClickListener
                }
                val minor: Int = node.beaconMinor
                emitter.start(doc.uuid, 1, minor, -59) { ok, err ->
                    runOnUiThread {
                        if (ok) {
                            broadcasting = true
                            toggle.text = getString(R.string.btn_stop_broadcast)
                            status.text = "Broadcasting as ${doc.displayName(node)} (minor $minor)"
                        } else {
                            status.text = err ?: "Failed to start broadcast."
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        emitter.stop()
    }
}
