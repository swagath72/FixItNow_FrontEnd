package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.LoginRequest
import com.simats.fixitnow.network.LoginResponse
import com.simats.fixitnow.network.RetrofitClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<TextView>(R.id.loginButton)
        val createAccountButton = findViewById<TextView>(R.id.createAccountButton)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                loginButton.isEnabled = email.isNotEmpty() && 
                                       Patterns.EMAIL_ADDRESS.matcher(email).matches() && 
                                       password.isNotEmpty()
            }
        }

        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)

        val loginProgressBar = findViewById<android.widget.ProgressBar>(R.id.loginProgressBar)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            // Show loading state
            loginButton.text = ""
            loginButton.isEnabled = false
            loginProgressBar.visibility = android.view.View.VISIBLE
            
            val loginRequest = LoginRequest(email, password)
            val apiService = RetrofitClient.createService(ApiService::class.java)
 
            apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    // Reset loading state
                    loginButton.text = "Login"
                    loginButton.isEnabled = true
                    loginProgressBar.visibility = android.view.View.GONE
                    
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("AUTH_TOKEN", loginResponse.token)
                                putString("USER_EMAIL", email) // Save email for booking
                                putString("USER_ROLE", loginResponse.role) // Save role for auto-login
                                
                                // Ensure USER_NAME is saved, fallback to email name if fullName is null
                                val nameToSave = if (!loginResponse.fullName.isNullOrBlank()) loginResponse.fullName else email.split("@")[0]
                                putString("USER_NAME", nameToSave)
                                
                                putString("USER_PHONE", loginResponse.phone)
                                putString("BOOKED_TECH_NAME", loginResponse.bookedTechnicianName)
                                putString("BOOKING_STATUS", loginResponse.bookingStatus)
                                putBoolean("HAS_COMPLETED_ONBOARDING", loginResponse.hasCompletedOnboarding)
                                putString("PROFILE_PIC_URL", loginResponse.profilePicUrl)
                                
                                if (!loginResponse.houseNumber.isNullOrEmpty() && !loginResponse.area.isNullOrEmpty()) {
                                    val displayLoc = "${loginResponse.houseNumber}, ${loginResponse.area}"
                                    putString("USER_DISPLAY_LOCATION", displayLoc)
                                    putString("house_number", loginResponse.houseNumber)
                                    putString("area", loginResponse.area)
                                    putString("street", loginResponse.street)
                                    putString("city", loginResponse.city)
                                    putString("state", loginResponse.state)
                                }
                                apply()
                            }
 
                            val intent = when (loginResponse.role) {
                                "Customer" -> Intent(this@MainActivity, HomeActivity::class.java)
                                "Technician" -> {
                                    if (loginResponse.hasCompletedOnboarding) {
                                        Intent(this@MainActivity, TechnicianHomeActivity::class.java)
                                    } else {
                                        // Resume onboarding from registration
                                        Intent(this@MainActivity, TechnicianRegistrationActivity::class.java)
                                    }
                                }
                                else -> Intent(this@MainActivity, RoleSelectionActivity::class.java).apply {
                                    putExtra("AUTH_TOKEN", loginResponse.token)
                                }
                            }
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (errorBody != null) {
                            try {
                                val errorJson = JSONObject(errorBody)
                                errorJson.getString("detail")
                            } catch (e: JSONException) {
                                "Server error (${response.code()})"
                            }
                        } else {
                            "Login failed (${response.code()})"
                        }
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // Reset loading state
                    loginButton.text = "Login"
                    loginButton.isEnabled = true
                    loginProgressBar.visibility = android.view.View.GONE
                    val msg = if (t.message?.contains("Unable to resolve host") == true || 
                                  t.message?.contains("Connection refused") == true ||
                                  t.message?.contains("failed to connect") == true) {
                        "Cannot reach server. Make sure you're on the same WiFi network as the backend."
                    } else {
                        "Network error: ${t.message}"
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            })
        }

        createAccountButton.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }
        forgotPasswordText.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
