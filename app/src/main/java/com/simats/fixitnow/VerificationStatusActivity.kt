package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class VerificationStatusActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_status)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.uploadButton).setOnClickListener {
            Toast.makeText(this, "Document Upload Feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
