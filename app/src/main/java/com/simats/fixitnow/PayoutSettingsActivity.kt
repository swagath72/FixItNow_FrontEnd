package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PayoutSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payout_settings)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.addMethodButton).setOnClickListener {
            Toast.makeText(this, "Add Payout Method", Toast.LENGTH_SHORT).show()
        }
    }
}
