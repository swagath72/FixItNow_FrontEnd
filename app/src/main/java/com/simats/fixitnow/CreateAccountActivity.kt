package com.simats.fixitnow

import android.content.Context
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
import com.simats.fixitnow.network.RegisterRequest
import com.simats.fixitnow.network.RegisterResponse
import com.simats.fixitnow.network.RetrofitClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)

        val fullNameEditText = findViewById<EditText>(R.id.fullNameEditText)
        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val createAccountButton = findViewById<TextView>(R.id.createAccountButton)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val fullName = fullNameEditText.text.toString().trim()
                val phone = phoneNumberEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()

                val allFilled = fullName.isNotEmpty() &&
                        phone.isNotEmpty() &&
                        email.isNotEmpty() &&
                        password.isNotEmpty() &&
                        confirmPassword.isNotEmpty() &&
                        password == confirmPassword

                createAccountButton.isEnabled = allFilled
            }
        }

        createAccountButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString().trim()
            val phone = phoneNumberEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            val registerRequest = RegisterRequest(fullName, email, password, phone)

            val apiService = RetrofitClient.createService(ApiService::class.java)
            val call = apiService.register(registerRequest)

            call.enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(
                    call: Call<RegisterResponse>,
                    response: Response<RegisterResponse>
                ) {
                    if (response.isSuccessful) {
                        // Save name to shared preferences upon successful registration
                        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("USER_NAME", fullName)
                            apply()
                        }

                        Toast.makeText(this@CreateAccountActivity, response.body()?.message ?: "User registered successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@CreateAccountActivity, MainActivity::class.java)
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
                        Toast.makeText(this@CreateAccountActivity, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@CreateAccountActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        fullNameEditText.addTextChangedListener(textWatcher)
        phoneNumberEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
