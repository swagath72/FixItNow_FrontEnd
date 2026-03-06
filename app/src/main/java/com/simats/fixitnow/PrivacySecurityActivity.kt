package com.simats.fixitnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PrivacySecurityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_security)

        findViewById<android.view.View>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
