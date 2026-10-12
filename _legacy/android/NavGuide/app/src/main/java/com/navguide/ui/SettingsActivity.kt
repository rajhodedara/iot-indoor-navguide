package com.navguide.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.data.UserRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userRepository = UserRepository(this)

        val etEmergencyContact = findViewById<EditText>(R.id.etEmergencyContact)
        etEmergencyContact.setText(userRepository.getEmergencyContact() ?: "")

        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        btnSave.setOnClickListener {
            userRepository.saveEmergencyContact(etEmergencyContact.text.toString())
            finish()
        }
    }
}
