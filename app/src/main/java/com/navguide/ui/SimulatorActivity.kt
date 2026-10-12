package com.navguide.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.ble.BeaconBus
import com.navguide.data.MapRepository

class SimulatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulator)
        val doc = MapRepository.load(this)
        val container = findViewById<LinearLayout>(R.id.llSimNodes)
        val status = findViewById<TextView>(R.id.tvSimStatus)

        doc.nodes.sortedBy { it.beaconMinor ?: 99 }.forEach { node ->
            val b = Button(this)
            b.text = "Hop to ${node.name}"
            b.textSize = 18f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60))
            lp.bottomMargin = dp(8)
            b.layoutParams = lp
            b.setOnClickListener {
                BeaconBus.nodeHops.tryEmit(node.id)
                status.text = "User is now near ${node.name}"
            }
            container.addView(b)
        }

        findViewById<Button>(R.id.btnSteps5).setOnClickListener {
            BeaconBus.stepPulses.tryEmit(5)
            status.text = "Added 5 steps"
        }
        findViewById<Button>(R.id.btnSteps10).setOnClickListener {
            BeaconBus.stepPulses.tryEmit(10)
            status.text = "Added 10 steps"
        }
        findViewById<Button>(R.id.btnSimReset).setOnClickListener {
            status.text = getString(R.string.sim_status_hint)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
