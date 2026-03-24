package com.simats.fixitnow

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class LocationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var streetAddressInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var cityInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var stateInput: AutoCompleteTextView
    private lateinit var zipCodeInput: com.google.android.material.textfield.TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        streetAddressInput = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.streetAddressLayout).editText as com.google.android.material.textfield.TextInputEditText
        cityInput = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.cityLayout).editText as com.google.android.material.textfield.TextInputEditText
        stateInput = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.stateLayout).editText as AutoCompleteTextView
        zipCodeInput = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.zipCodeLayout).editText as com.google.android.material.textfield.TextInputEditText
        val useCurrentLocationButton = findViewById<TextView>(R.id.useCurrentLocationButton)
        val continueButton = findViewById<TextView>(R.id.continueButton)

        val states = arrayOf("Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Lakshadweep", "Puducherry")
        val stateAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        stateInput.setAdapter(stateAdapter)

        zipCodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 6) {
                    fetchLocationDetails(s.toString())
                }
            }
        })

        useCurrentLocationButton.setOnClickListener {
            requestLocationPermission()
        }

        continueButton.setOnClickListener {
            val city = cityInput.text.toString().trim()
            val state = stateInput.text.toString().trim()
            
            if (city.isNotEmpty()) {
                val locationDisplay = if (state.isNotEmpty()) "$city, $state" else city
                
                val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("USER_LOCATION", locationDisplay)
                    apply()
                }
                
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter at least the city", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = locationResult.lastLocation
                if (location != null) {
                    val geocoder = Geocoder(this@LocationActivity, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                runOnUiThread {
                                    streetAddressInput.setText(address.getAddressLine(0))
                                    cityInput.setText(address.locality)
                                    stateInput.setText(address.adminArea, false)
                                    zipCodeInput.setText(address.postalCode)
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            streetAddressInput.setText(address.getAddressLine(0))
                            cityInput.setText(address.locality)
                            stateInput.setText(address.adminArea, false)
                            zipCodeInput.setText(address.postalCode)
                        }
                    }
                } else {
                    Toast.makeText(this@LocationActivity, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    private fun fetchLocationDetails(pincode: String) {
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
                    
                    // District or Block can be used for City
                    val district = if (firstBranch.has("District")) firstBranch.getString("District") else firstBranch.getString("Block")
                    val state = firstBranch.getString("State")
                    
                    runOnUiThread {
                        cityInput.setText(district)
                        stateInput.setText(state, false)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LocationActivity, "Invalid Pincode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
