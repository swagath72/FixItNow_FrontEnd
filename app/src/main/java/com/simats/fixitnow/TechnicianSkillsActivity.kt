package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TechnicianSkillsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technician_skills)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            Toast.makeText(this, "Skills updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
