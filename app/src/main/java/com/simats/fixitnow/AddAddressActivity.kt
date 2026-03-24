package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import org.json.JSONArray
import android.text.Editable
import android.text.TextWatcher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simats.fixitnow.network.AddAddressRequest
import com.simats.fixitnow.network.AddAddressResponse
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddAddressActivity : AppCompatActivity() {
    private var isEditMode = false
    private var addressPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_address)

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        addressPosition = intent.getIntExtra("ADDRESS_POSITION", -1)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        
        val houseNoEditText = findViewById<TextInputEditText>(R.id.houseNoEditText)
        val streetEditText = findViewById<TextInputEditText>(R.id.streetEditText)
        val areaEditText = findViewById<TextInputEditText>(R.id.areaEditText)
        val cityEditText = findViewById<TextInputEditText>(R.id.cityEditText)
        val stateEditText = findViewById<AutoCompleteTextView>(R.id.stateEditText)
        val pincodeEditText = findViewById<TextInputEditText>(R.id.pincodeEditText)
        val landmarkEditText = findViewById<TextInputEditText>(R.id.landmarkEditText)

        val states = arrayOf("Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Lakshadweep", "Puducherry")
        val stateAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        stateEditText.setAdapter(stateAdapter)

        pincodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    fetchLocationDetails(s.toString(), cityEditText, stateEditText)
                }
            }
        })

        if (isEditMode) {
            headerTitle.text = "Edit Address"
            saveButton.text = "Update Address"
            houseNoEditText.setText(intent.getStringExtra("ADDRESS_DETAILS"))
        }

        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val houseNo = houseNoEditText.text.toString().trim()
            val street = streetEditText.text.toString().trim()
            val area = areaEditText.text.toString().trim()
            val city = cityEditText.text.toString().trim()
            val state = stateEditText.text.toString().trim()
            val pincode = pincodeEditText.text.toString().trim()
            val landmark = landmarkEditText.text.toString().trim()

            if (houseNo.isEmpty() || street.isEmpty() || area.isEmpty() || city.isEmpty() || state.isEmpty() || pincode.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullAddress = if (landmark.isNotEmpty()) {
                "$houseNo, $street, $area, $landmark, $city, $state - $pincode"
            } else {
                "$houseNo, $street, $area, $city, $state - $pincode"
            }
            
            // Short version: House number and Area
            val shortAddress = "$houseNo, $area"

            // Save to Database via API
            saveAddressToDatabase(houseNo, street, area, city, state, pincode, landmark, fullAddress, shortAddress)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveAddressToDatabase(
        houseNo: String, street: String, area: String, city: String, 
        state: String, pincode: String, landmark: String,
        fullAddress: String, shortAddress: String
    ) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = AddAddressRequest(houseNo, street, area, city, state, pincode, if (landmark.isEmpty()) null else landmark)

        apiService.addAddress("Bearer $token", request).enqueue(object : Callback<AddAddressResponse> {
            override fun onResponse(call: Call<AddAddressResponse>, response: Response<AddAddressResponse>) {
                if (response.isSuccessful) {
                    // Also save locally for immediate UI update
                    saveAddressLocally(fullAddress, shortAddress)
                    
                    // Update current display location
                    with(sharedPref.edit()) {
                        putString("USER_LOCATION", fullAddress)
                        putString("USER_DISPLAY_LOCATION", shortAddress)
                        apply()
                    }

                    Toast.makeText(this@AddAddressActivity, response.body()?.message ?: "Address saved", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddAddressActivity, "Failed to save address: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddAddressResponse>, t: Throwable) {
                Toast.makeText(this@AddAddressActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAddressLocally(addressDetails: String, shortAddress: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val gson = Gson()
        
        val json = sharedPref.getString("SAVED_ADDRESSES_LIST", null)
        val type = object : TypeToken<MutableList<SavedAddress>>() {}.type
        val addressList: MutableList<SavedAddress> = if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, type)
        }

        if (isEditMode && addressPosition != -1 && addressPosition < addressList.size) {
            val existing = addressList[addressPosition]
            addressList[addressPosition] = SavedAddress(existing.name, addressDetails, shortAddress, existing.isDefault, existing.iconRes)
        } else {
            val newAddress = SavedAddress("Saved Location", addressDetails, shortAddress, addressList.isEmpty(), R.drawable.ic_location_pin)
            addressList.add(newAddress)
        }

        val updatedJson = gson.toJson(addressList)
        sharedPref.edit().putString("SAVED_ADDRESSES_LIST", updatedJson).apply()
    }

    private fun fetchLocationDetails(pincode: String, cityEditText: TextInputEditText, stateEditText: AutoCompleteTextView) {
        Thread {
            try {
                val url = java.net.URL("https://api.postalpincode.in/pincode/$pincode")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val status = jsonArray.getJSONObject(0).getString("Status")
                
                if (status == "Success") {
                    val postOfficeArray = jsonArray.getJSONObject(0).getJSONArray("PostOffice")
                    val firstBranch = postOfficeArray.getJSONObject(0)
                    
                    val district = if (firstBranch.has("District")) firstBranch.getString("District") else firstBranch.getString("Block")
                    val state = firstBranch.getString("State")
                    
                    runOnUiThread {
                        cityEditText.setText(district)
                        stateEditText.setText(state, false)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AddAddressActivity, "Invalid Pincode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
