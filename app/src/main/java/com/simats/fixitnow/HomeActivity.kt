package com.simats.fixitnow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: CurvedBottomNavigationView

    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), 0)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.navigation_home -> 0
                R.id.navigation_history -> 1
                R.id.navigation_chat -> 2
                R.id.navigation_profile -> 3
                else -> 0
            }

            if (index != currentNavIndex) {
                bottomNavigation.setSelectedItem(index)
                val fragment = when (item.itemId) {
                    R.id.navigation_home -> HomeFragment()
                    R.id.navigation_history -> HistoryFragment()
                    R.id.navigation_chat -> ChatFragment()
                    R.id.navigation_profile -> ProfileFragment()
                    else -> HomeFragment()
                }
                loadFragment(fragment, index)
                true
            } else {
                false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun loadFragment(fragment: Fragment, newIndex: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        
        if (newIndex > currentNavIndex) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else if (newIndex < currentNavIndex) {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        }
        
        currentNavIndex = newIndex
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }

    override fun onResume() {
        super.onResume()
        // Selection is now handled by fragment transactions
    }
}
