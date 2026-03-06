package com.simats.fixitnow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.VerifyOtpRequest
import com.simats.fixitnow.network.VerifyOtpResponse
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp)

        val userEmail = intent.getStringExtra("EMAIL") ?: ""
        val backButton = findViewById<ImageView>(R.id.backButton)
        val otpEditText = findViewById<EditText>(R.id.otpEditText)
        val verifyButton = findViewById<TextView>(R.id.verifyButton)

        backButton.setOnClickListener {
            // Navigate back to Forgot Password page
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val otp = otpEditText.text.toString().trim()
                val isValid = otp.isNotEmpty()
                
                verifyButton.isEnabled = isValid
            }
        }
        otpEditText.addTextChangedListener(textWatcher)

        verifyButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()
            val request = VerifyOtpRequest(userEmail, otp)
            val apiService = RetrofitClient.createService(ApiService::class.java)

            apiService.verifyOtp(request).enqueue(object : Callback<VerifyOtpResponse> {
                override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@OtpActivity, response.body()?.message ?: "OTP Verified", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@OtpActivity, NewPasswordActivity::class.java)
                        intent.putExtra("EMAIL", userEmail)
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
                        Toast.makeText(this@OtpActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                    Toast.makeText(this@OtpActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
