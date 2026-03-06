package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HelpSupportTechnicianActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support_technician)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.chatSupportButton).setOnClickListener {
            Toast.makeText(this, "Starting support chat...", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.emailSupportButton).setOnClickListener {
            Toast.makeText(this, "Opening email client...", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.callSupportButton).setOnClickListener {
            Toast.makeText(this, "Dialing support...", Toast.LENGTH_SHORT).show()
        }
    }
}
