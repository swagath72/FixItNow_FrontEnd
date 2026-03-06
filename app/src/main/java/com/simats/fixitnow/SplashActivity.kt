package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val dots = mutableListOf<View>()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        dots.add(findViewById(R.id.dot1))
        dots.add(findViewById(R.id.dot2))
        dots.add(findViewById(R.id.dot3))

        startDotAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            stopDotAnimation()
            navigateToNextScreen()
        }, 3000)
    }

    private fun navigateToNextScreen() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null)
        val role = sharedPref.getString("USER_ROLE", null)
        val hasCompletedOnboarding = sharedPref.getBoolean("HAS_COMPLETED_ONBOARDING", false)

        val intent = if (!token.isNullOrEmpty() && !role.isNullOrEmpty()) {
            // Already logged in — go directly to the right home screen
            when (role) {
                "Customer" -> Intent(this, HomeActivity::class.java)
                "Technician" -> {
                    if (hasCompletedOnboarding) {
                        Intent(this, TechnicianHomeActivity::class.java)
                    } else {
                        Intent(this, TechnicianRegistrationActivity::class.java)
                    }
                }
                else -> Intent(this, MainActivity::class.java)
            }
        } else {
            // Not logged in — show login screen
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()
    }

    private fun startDotAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                dots.forEachIndexed { index, dot ->
                    if (index == currentIndex) {
                        dot.setBackgroundResource(R.drawable.dot_active)
                        dot.startAnimation(AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in))
                    } else {
                        dot.setBackgroundResource(R.drawable.dot_inactive)
                        dot.startAnimation(AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_out))
                    }
                }
                currentIndex = (currentIndex + 1) % dots.size
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun stopDotAnimation() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        stopDotAnimation()
        super.onDestroy()
    }
}
