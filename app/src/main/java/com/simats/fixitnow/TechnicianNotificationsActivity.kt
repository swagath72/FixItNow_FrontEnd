package com.simats.fixitnow

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TechnicianNotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_technician_notifications)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        setupNotificationSettings()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNotificationSettings() {
        findViewById<View>(R.id.notifJobRequests).apply {
            findViewById<TextView>(R.id.settingTitle).text = "New Job Requests"
            findViewById<TextView>(R.id.settingSubtitle).text = "Instant alerts for nearby jobs"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_flash)
        }
        
        findViewById<View>(R.id.notifEarnings).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Earnings Reports"
            findViewById<TextView>(R.id.settingSubtitle).text = "Weekly & monthly payout summaries"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_wallet)
        }
        
        findViewById<View>(R.id.notifChat).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Chat Messages"
            findViewById<TextView>(R.id.settingSubtitle).text = "Direct messages from customers"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_chat)
        }
    }
}
