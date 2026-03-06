package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.RoleSelectionRequest
import com.simats.fixitnow.network.RoleSelectionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RoleSelectionActivity : AppCompatActivity() {
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_selection)

        // Try getting token from intent first, then from shared prefs if missing
        authToken = intent.getStringExtra("AUTH_TOKEN")
        if (authToken == null) {
            val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
            authToken = sharedPref.getString("AUTH_TOKEN", null)
        }

        val customerCard = findViewById<MaterialCardView>(R.id.customerCard)
        val technicianCard = findViewById<MaterialCardView>(R.id.technicianCard)

        customerCard.setOnClickListener {
            selectRole("Customer")
        }

        technicianCard.setOnClickListener {
            selectRole("Technician")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun selectRole(role: String) {
        if (authToken == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = RoleSelectionRequest(role)

        apiService.selectRole("Bearer $authToken", request).enqueue(object : Callback<RoleSelectionResponse> {
            override fun onResponse(call: Call<RoleSelectionResponse>, response: Response<RoleSelectionResponse>) {
                if (response.isSuccessful) {
                    // Update role in local storage
                    val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("USER_ROLE", role).apply()

                    Toast.makeText(this@RoleSelectionActivity, "Role selected: $role", Toast.LENGTH_SHORT).show()
                    
                    when (role) {
                        "Customer" -> {
                            startActivity(Intent(this@RoleSelectionActivity, LocationActivity::class.java))
                        }
                        "Technician" -> {
                            startActivity(Intent(this@RoleSelectionActivity, TechnicianRegistrationActivity::class.java))
                        }
                    }
                    finish()
                } else {
                    Toast.makeText(this@RoleSelectionActivity, "Server error. Try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RoleSelectionResponse>, t: Throwable) {
                Toast.makeText(this@RoleSelectionActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
