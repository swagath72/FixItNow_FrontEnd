package com.simats.fixitnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AppSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)

        findViewById<android.view.View>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
