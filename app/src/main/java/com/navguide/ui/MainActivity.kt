package com.navguide.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var selectedFloor: Int? = 7 // Default to Floor 7
    private var selectedCategory: String = "ALL" // ALL, ROOMS, STAIRS, WASHROOM, EXITS

    private lateinit var chipFloorAll: TextView
    private lateinit var chipFloor1: TextView
    private lateinit var chipFloor7: TextView
    private lateinit var chipFloor8: TextView

    private lateinit var chipCatAll: TextView
    private lateinit var chipCatRooms: TextView
    private lateinit var chipCatStairs: TextView
    private lateinit var chipCatWashroom: TextView
    private lateinit var chipCatExits: TextView

    private lateinit var llDestinations: LinearLayout
    private lateinit var tvResultsCount: TextView
    private lateinit var tvEmptyNotice: TextView

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

        // Find floor chips
        chipFloorAll = findViewById(R.id.chipFloorAll)
        chipFloor1 = findViewById(R.id.chipFloor1)
        chipFloor7 = findViewById(R.id.chipFloor7)
        chipFloor8 = findViewById(R.id.chipFloor8)

        // Find category chips
        chipCatAll = findViewById(R.id.chipCatAll)
        chipCatRooms = findViewById(R.id.chipCatRooms)
        chipCatStairs = findViewById(R.id.chipCatStairs)
        chipCatWashroom = findViewById(R.id.chipCatWashroom)
        chipCatExits = findViewById(R.id.chipCatExits)

        llDestinations = findViewById(R.id.llDestinations)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        tvEmptyNotice = findViewById(R.id.tvEmptyNotice)

        setupFloorFilters()
        setupCategoryFilters()

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

        renderDestinations()
        ensurePermissions()
    }

    private fun setupFloorFilters() {
        chipFloorAll.setOnClickListener {
            selectedFloor = null
            updateFloorChipsUI()
            renderDestinations()
            announceFilter("Showing all floors")
        }
        chipFloor1.setOnClickListener {
            selectedFloor = 1
            updateFloorChipsUI()
            renderDestinations()
            announceFilter("Ground Floor selected")
        }
        chipFloor7.setOnClickListener {
            selectedFloor = 7
            updateFloorChipsUI()
            renderDestinations()
            announceFilter("Seventh Floor selected")
        }
        chipFloor8.setOnClickListener {
            selectedFloor = 8
            updateFloorChipsUI()
            renderDestinations()
            announceFilter("Eighth Floor selected")
        }
        updateFloorChipsUI()
    }

    private fun updateFloorChipsUI() {
        setChipState(chipFloorAll, selectedFloor == null)
        setChipState(chipFloor1, selectedFloor == 1)
        setChipState(chipFloor7, selectedFloor == 7)
        setChipState(chipFloor8, selectedFloor == 8)
    }

    private fun setupCategoryFilters() {
        chipCatAll.setOnClickListener {
            selectedCategory = "ALL"
            updateCategoryChipsUI()
            renderDestinations()
            announceFilter("All categories")
        }
        chipCatRooms.setOnClickListener {
            selectedCategory = "ROOMS"
            updateCategoryChipsUI()
            renderDestinations()
            announceFilter("Rooms and Labs")
        }
        chipCatStairs.setOnClickListener {
            selectedCategory = "STAIRS"
            updateCategoryChipsUI()
            renderDestinations()
            announceFilter("Staircases and Elevators")
        }
        chipCatWashroom.setOnClickListener {
            selectedCategory = "WASHROOM"
            updateCategoryChipsUI()
            renderDestinations()
            announceFilter("Washrooms")
        }
        chipCatExits.setOnClickListener {
            selectedCategory = "EXITS"
            updateCategoryChipsUI()
            renderDestinations()
            announceFilter("Emergency Exits")
        }
        updateCategoryChipsUI()
    }

    private fun updateCategoryChipsUI() {
        setChipState(chipCatAll, selectedCategory == "ALL")
        setChipState(chipCatRooms, selectedCategory == "ROOMS")
        setChipState(chipCatStairs, selectedCategory == "STAIRS")
        setChipState(chipCatWashroom, selectedCategory == "WASHROOM")
        setChipState(chipCatExits, selectedCategory == "EXITS")
    }

    private fun setChipState(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected)
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_on_accent))
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }

    private fun announceFilter(message: String) {
        speaker.speak(message)
    }

    private fun renderDestinations() {
        llDestinations.removeAllViews()

        var list = doc.destinationNodes

        // Filter by floor
        if (selectedFloor != null) {
            list = list.filter { it.floor == selectedFloor }
        }

        // Filter by category
        list = when (selectedCategory) {
            "ROOMS" -> list.filter {
                it.type == "room" && !isWashroom(it)
            }
            "STAIRS" -> list.filter {
                it.type == "stairs" || it.type == "elevator"
            }
            "WASHROOM" -> list.filter {
                isWashroom(it)
            }
            "EXITS" -> list.filter {
                it.type == "exit" || it.type == "entrance"
            }
            else -> list
        }

        val floorLabel = when (selectedFloor) {
            1 -> "Ground Floor (F1)"
            7 -> "7th Floor (F7)"
            8 -> "8th Floor (F8)"
            else -> "All Floors"
        }

        tvResultsCount.text = "Showing ${list.size} places · $floorLabel"

        if (list.isEmpty()) {
            tvEmptyNotice.visibility = View.VISIBLE
        } else {
            tvEmptyNotice.visibility = View.GONE
            list.sortedWith(compareBy({ it.floor }, { it.name })).forEach { node ->
                addDestinationRow(llDestinations, node)
            }
        }
    }

    private fun isWashroom(node: MapNode): Boolean =
        node.name.contains("toilet", ignoreCase = true) ||
            node.name.contains("washroom", ignoreCase = true) ||
            node.name.contains("restroom", ignoreCase = true)

    private fun addDestinationRow(parent: LinearLayout, node: MapNode) {
        val row = layoutInflater.inflate(R.layout.row_destination, parent, false)
        row.findViewById<ImageView>(R.id.ivIcon).setImageResource(iconFor(node))
        row.findViewById<TextView>(R.id.tvName).text = node.name

        val typeText = if (isWashroom(node)) "Washroom" else typeLabel(node.type)
        row.findViewById<TextView>(R.id.tvType).text = "Floor ${node.floor} · $typeText"

        val spokenDescription = "${node.name}, Floor ${node.floor}, $typeText. Tap to navigate."
        row.contentDescription = spokenDescription

        row.setOnClickListener {
            startActivity(
                Intent(this, NavigationActivity::class.java)
                    .putExtra(NavigationActivity.EXTRA_DEST, node.id)
            )
        }
        parent.addView(row)
    }

    private fun iconFor(node: MapNode): Int = when {
        isWashroom(node) -> R.drawable.ic_wc
        node.type == "entrance" -> R.drawable.ic_entrance
        node.type == "exit" -> R.drawable.ic_exit
        node.type == "stairs" -> R.drawable.ic_stairs
        node.type == "elevator" -> R.drawable.ic_elevator
        else -> R.drawable.ic_room
    }

    private fun typeLabel(type: String): String = when (type) {
        "entrance" -> getString(R.string.type_entrance)
        "exit" -> getString(R.string.type_exit)
        "stairs" -> getString(R.string.type_stairs)
        "elevator" -> getString(R.string.type_elevator)
        else -> getString(R.string.type_room)
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
