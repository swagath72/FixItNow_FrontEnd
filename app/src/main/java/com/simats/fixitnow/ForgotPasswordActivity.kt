package com.simats.fixitnow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.ForgotPasswordRequest
import com.simats.fixitnow.network.ForgotPasswordResponse
import com.simats.fixitnow.network.RetrofitClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val sendOtpButton = findViewById<TextView>(R.id.sendOtpButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                sendOtpButton.isEnabled = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            }
        })

        sendOtpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            
            // Show buffering state
            sendOtpButton.text = ""
            sendOtpButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val request = ForgotPasswordRequest(email)
            val apiService = RetrofitClient.createService(ApiService::class.java)

            apiService.forgotPassword(request).enqueue(object : Callback<ForgotPasswordResponse> {
                override fun onResponse(call: Call<ForgotPasswordResponse>, response: Response<ForgotPasswordResponse>) {
                    // Hide buffering state
                    progressBar.visibility = View.GONE
                    sendOtpButton.text = "Send OTP"
                    sendOtpButton.isEnabled = true

                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "OTP sent to your email", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ForgotPasswordActivity, OtpActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (errorBody != null) {
                            try {
                                val errorJson = JSONObject(errorBody)
                                errorJson.getString("detail")
                            } catch (e: JSONException) {
                                response.message()
                            }
                        } else {
                            response.message()
                        }
                        Toast.makeText(this@ForgotPasswordActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                    // Hide buffering state
                    progressBar.visibility = View.GONE
                    sendOtpButton.text = "Send OTP"
                    sendOtpButton.isEnabled = true

                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
