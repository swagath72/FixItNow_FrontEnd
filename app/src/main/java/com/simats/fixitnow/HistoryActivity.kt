package com.simats.fixitnow

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        val backButton = findViewById<View>(R.id.backButton)
        val tabCompleted = findViewById<TextView>(R.id.tabCompleted)
        val tabCancelled = findViewById<TextView>(R.id.tabCancelled)
        val completedList = findViewById<LinearLayout>(R.id.completedList)
        val cancelledList = findViewById<LinearLayout>(R.id.cancelledList)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Check if opened from Technician side
        val isTechnician = intent.getBooleanExtra("IS_TECHNICIAN", false)
        if (isTechnician) {
            bottomNavigation.menu.clear()
            bottomNavigation.inflateMenu(R.menu.technician_bottom_nav_menu)
        }

        backButton.setOnClickListener {
            finish()
        }

        tabCompleted.setOnClickListener {
            // Update Toggle UI
            tabCompleted.setBackgroundResource(R.drawable.history_toggle_selected)
            tabCompleted.setTextColor(Color.parseColor("#3C61FF"))

            tabCancelled.setBackground(null)
            tabCancelled.setTextColor(Color.WHITE)

            // Show Completed List
            completedList.visibility = View.VISIBLE
            cancelledList.visibility = View.GONE
        }

        tabCancelled.setOnClickListener {
            // Update Toggle UI
            tabCancelled.setBackgroundResource(R.drawable.history_toggle_selected)
            tabCancelled.setTextColor(Color.parseColor("#3C61FF"))

            tabCompleted.setBackground(null)
            tabCompleted.setTextColor(Color.WHITE)

            // Show Cancelled List
            completedList.visibility = View.GONE
            cancelledList.visibility = View.VISIBLE
        }

        // Set History as active in bottom nav
        bottomNavigation.selectedItemId = R.id.navigation_history

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home, R.id.navigation_dashboard -> {
                    finish() // Return to home/dashboard
                    true
                }
                R.id.navigation_history -> true
                R.id.navigation_chat -> {
                    finish()
                    true
                }
                R.id.navigation_profile -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("navigateTo", "profile")
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }
}
