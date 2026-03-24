package com.simats.fixitnow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.simats.fixitnow.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianHomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: CurvedBottomNavigationView
    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_technician_home)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(TechnicianHomeFragment(), 0)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.navigation_dashboard -> 0
                R.id.navigation_history -> 1
                R.id.navigation_chat -> 2
                R.id.navigation_profile -> 3
                else -> 0
            }

            if (index != currentNavIndex) {
                bottomNavigation.setSelectedItem(index)
                val fragment = when (item.itemId) {
                    R.id.navigation_dashboard -> TechnicianHomeFragment()
                    R.id.navigation_history -> TechnicianHistoryFragment()
                    R.id.navigation_chat -> TechnicianChatFragment()
                    R.id.navigation_profile -> TechnicianProfileFragment()
                    else -> TechnicianHomeFragment()
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
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", android.content.Context.MODE_PRIVATE)
        val location = sharedPref.getString("USER_DISPLAY_LOCATION", "")
        if (location.isNullOrEmpty()) {
            val intent = android.content.Intent(this, AddressSelectionActivity::class.java)
            intent.putExtra("IS_MANDATORY", true)
            startActivity(intent)
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        val intent = android.content.Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopLocationUpdates() {
        val intent = android.content.Intent(this, LocationService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // We might want to stop the service when the activity is destroyed, 
        // OR let it run if the technician is supposed to stay online.
        // For now, let's stop it for simplicity, but in a real case we'd check availability status.
        stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
}
