package com.simats.fixitnow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.ResetPasswordRequest
import com.simats.fixitnow.network.ResetPasswordResponse
import com.simats.fixitnow.network.RetrofitClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_password)

        val userEmail = intent.getStringExtra("EMAIL") ?: ""
        val newPasswordEditText = findViewById<EditText>(R.id.newPasswordEditText)
        val confirmNewPasswordEditText = findViewById<EditText>(R.id.confirmNewPasswordEditText)
        val changePasswordButton = findViewById<TextView>(R.id.changePasswordButton)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = newPasswordEditText.text.toString().trim()
                val confirmPassword = confirmNewPasswordEditText.text.toString().trim()
                val isValid = password.isNotEmpty() && password == confirmPassword
                
                changePasswordButton.isEnabled = isValid
            }
        }

        newPasswordEditText.addTextChangedListener(textWatcher)
        confirmNewPasswordEditText.addTextChangedListener(textWatcher)

        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmNewPasswordEditText.text.toString().trim()
            
            val request = ResetPasswordRequest(userEmail, newPassword, confirmPassword)
            val apiService = RetrofitClient.createService(ApiService::class.java)

            apiService.resetPassword(request).enqueue(object : Callback<ResetPasswordResponse> {
                override fun onResponse(call: Call<ResetPasswordResponse>, response: Response<ResetPasswordResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@NewPasswordActivity, response.body()?.message ?: "Password Changed Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@NewPasswordActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
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
                        Toast.makeText(this@NewPasswordActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                    Toast.makeText(this@NewPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
