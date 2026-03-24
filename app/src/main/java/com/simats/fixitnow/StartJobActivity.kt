package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.UpdateJobStatusRequest
import com.simats.fixitnow.network.UserPhoneResponse
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StartJobActivity : AppCompatActivity() {

    private var bookingId: Int = -1
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSM
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_start_job)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        Log.d("FixItNow", "StartJobActivity: Received bookingId = $bookingId")
        if (bookingId == -1) {
            Toast.makeText(this, "Error: Invalid booking ID received", Toast.LENGTH_LONG).show()
        }
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
        val customerEmail = intent.getStringExtra("CUSTOMER_EMAIL")
        val address = intent.getStringExtra("ADDRESS") ?: ""
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val cost = intent.getStringExtra("COST")

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        findViewById<TextView>(R.id.addressText).text = address

        if (address.isNotEmpty()) {
            geocodeAddress(address) { geoPoint ->
                if (geoPoint != null) {
                    setupMap(geoPoint)
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Could not find location on map", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        findViewById<MaterialButton>(R.id.startJobButton).setOnClickListener {
            updateStatus("Started", customerName, serviceName, cost)
        }
        
        findViewById<MaterialButton>(R.id.callButton).setOnClickListener {
            if (!customerEmail.isNullOrEmpty()) {
                val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
                val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
                val apiService = RetrofitClient.createService(ApiService::class.java)

                apiService.getUserPhone("Bearer $token", customerEmail).enqueue(object : Callback<UserPhoneResponse> {
                    override fun onResponse(call: Call<UserPhoneResponse>, response: Response<UserPhoneResponse>) {
                        if (response.isSuccessful) {
                            val phone = response.body()?.phone ?: ""
                            if (phone.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_DIAL)
                                intent.data = android.net.Uri.parse("tel:$phone")
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@StartJobActivity, "Phone number not available", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@StartJobActivity, "Failed to fetch phone number", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UserPhoneResponse>, t: Throwable) {
                        Toast.makeText(this@StartJobActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Customer contact info not available", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.messageButton).setOnClickListener {
            if (!customerEmail.isNullOrEmpty()) {
                val intent = Intent(this, ChatDetailActivity::class.java).apply {
                    putExtra("OTHER_USER_EMAIL", customerEmail)
                    putExtra("OTHER_USER_NAME", customerName ?: "Customer")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Customer contact info not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun geocodeAddress(addressStr: String, callback: (GeoPoint?) -> Unit) {
        Thread {
            try {
                val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocationName(addressStr, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val geoPoint = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                    runOnUiThread { callback(geoPoint) }
                } else {
                    runOnUiThread { callback(null) }
                }
            } catch (e: Exception) {
                runOnUiThread { callback(null) }
            }
        }.start()
    }

    private fun setupMap(geoPoint: GeoPoint) {
        map.overlays.clear()
        val marker = Marker(map)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Customer Location"
        map.overlays.add(marker)

        map.controller.setZoom(16.0)
        map.controller.setCenter(geoPoint)
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    private fun updateStatus(status: String, customerName: String?, serviceName: String?, cost: String?) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = UpdateJobStatusRequest(bookingId, status)

        apiService.updateJobStatus("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("FixItNow", "StartJobActivity: Status updated to Started for ID $bookingId")
                    val intent = Intent(this@StartJobActivity, CompleteJobActivity::class.java)
                    intent.putExtra("BOOKING_ID", bookingId) // bookingId is Int, so this is safe
                    intent.putExtra("CUSTOMER_NAME", customerName)
                    intent.putExtra("SERVICE_NAME", serviceName)
                    intent.putExtra("COST", cost)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = "Failed to start job (Code: ${response.code()})"
                    Log.e("FixItNow", "StartJobActivity: $errorMsg")
                    Toast.makeText(this@StartJobActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("FixItNow", "StartJobActivity: Network error: ${t.message}")
                Toast.makeText(this@StartJobActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
