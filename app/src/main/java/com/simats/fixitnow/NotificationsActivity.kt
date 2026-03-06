package com.simats.fixitnow

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

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
        // Notification Types
        findViewById<View>(R.id.notifBooking).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Booking Updates"
            findViewById<TextView>(R.id.settingSubtitle).text = "Status changes for your bookings"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_notifications)
        }
        findViewById<View>(R.id.notifPromo).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Promotions"
            findViewById<TextView>(R.id.settingSubtitle).text = "Offers & special deals"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_rewards)
        }
        findViewById<View>(R.id.notifChat).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Chat Messages"
            findViewById<TextView>(R.id.settingSubtitle).text = "New messages from technicians"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_chat)
        }

        // Delivery Methods
        findViewById<View>(R.id.notifPush).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Push Notifications"
            findViewById<TextView>(R.id.settingSubtitle).text = "In-app alerts on your device"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_notifications)
        }
        findViewById<View>(R.id.notifSMS).apply {
            findViewById<TextView>(R.id.settingTitle).text = "SMS Alerts"
            findViewById<TextView>(R.id.settingSubtitle).text = "Text messages to your phone"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_chat)
        }
        findViewById<View>(R.id.notifEmail).apply {
            findViewById<TextView>(R.id.settingTitle).text = "Email Notifications"
            findViewById<TextView>(R.id.settingSubtitle).text = "Updates to your inbox"
            findViewById<ImageView>(R.id.settingIcon).setImageResource(R.drawable.ic_description_white)
        }
    }
}
