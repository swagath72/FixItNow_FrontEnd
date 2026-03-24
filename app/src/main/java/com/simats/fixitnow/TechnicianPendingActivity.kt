package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.LoginRequest
import com.simats.fixitnow.network.LoginResponse
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianPendingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_technician_pending)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.refreshButton).setOnClickListener {
            checkStatus()
        }

        findViewById<TextView>(R.id.logoutText).setOnClickListener {
            logout()
        }
    }

    private fun checkStatus() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val email = sharedPref.getString("USER_EMAIL", null) ?: return
        val password = sharedPref.getString("USER_PASSWORD", null) ?: return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val loginRequest = LoginRequest(email, password)

        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.verificationStatus.equals("approved", ignoreCase = true)) {
                        Toast.makeText(this@TechnicianPendingActivity, "Account Approved!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@TechnicianPendingActivity, TechnicianHomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@TechnicianPendingActivity, "Still under review", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@TechnicianPendingActivity, "Failed to check status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@TechnicianPendingActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
