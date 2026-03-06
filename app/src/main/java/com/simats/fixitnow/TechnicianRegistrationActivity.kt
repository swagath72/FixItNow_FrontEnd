package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TechnicianRegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_technician_registration)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val continueButton = findViewById<TextView>(R.id.continueButton)
        val experienceEditText = findViewById<EditText>(R.id.experienceEditText)
        val electricianLayout = findViewById<LinearLayout>(R.id.electricianLayout)
        val plumberLayout = findViewById<LinearLayout>(R.id.plumberLayout)

        var selectedSkill = ""

        backButton.setOnClickListener {
            finish()
        }

        // Wrapper to update button state
        fun updateButtonState() {
             val experience = experienceEditText.text.toString().trim()
             val isValid = experience.isNotEmpty() && selectedSkill.isNotEmpty()
             
             continueButton.isEnabled = isValid
             if (isValid) {
                 continueButton.setBackgroundColor(android.graphics.Color.parseColor("#3C61FF"))
             } else {
                 continueButton.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
             }
        }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonState()
            }
        }

        experienceEditText.addTextChangedListener(textWatcher)

        electricianLayout.setOnClickListener {
            selectedSkill = "Electrician"
            electricianLayout.setBackgroundResource(R.drawable.selected_skill_background)
            plumberLayout.setBackgroundResource(R.drawable.edit_text_background)
            electricianLayout.alpha = 1.0f 
            plumberLayout.alpha = 0.5f
            updateButtonState()
        }

        plumberLayout.setOnClickListener {
            selectedSkill = "Plumber"
            plumberLayout.setBackgroundResource(R.drawable.selected_skill_background)
            electricianLayout.setBackgroundResource(R.drawable.edit_text_background)
            plumberLayout.alpha = 1.0f
            electricianLayout.alpha = 0.5f
            updateButtonState()
        }

        continueButton.setOnClickListener {
            val experience = experienceEditText.text.toString().trim()
            val onboardingRequest = com.simats.fixitnow.network.TechnicianOnboardingRequest(selectedSkill, experience)
            
            val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
            val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
            
            continueButton.isEnabled = false
            continueButton.text = "Saving..."
            
            val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
            apiService.updateTechnicianOnboarding("Bearer $token", onboardingRequest).enqueue(object : retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                    if (response.isSuccessful) {
                        val intent = android.content.Intent(this@TechnicianRegistrationActivity, DocumentVerificationActivity::class.java)
                        startActivity(intent)
                    } else {
                        continueButton.isEnabled = true
                        continueButton.text = "Continue"
                        Toast.makeText(this@TechnicianRegistrationActivity, "Failed to save details", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    continueButton.isEnabled = true
                    continueButton.text = "Continue"
                    Toast.makeText(this@TechnicianRegistrationActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
