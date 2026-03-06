package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WorkingHoursActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_working_hours)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
