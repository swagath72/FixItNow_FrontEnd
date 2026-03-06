package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.simats.fixitnow.network.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList
import androidx.core.content.ContextCompat

class TrackTechnicianActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private var bookingId: Int = -1
    private var cost: Double = 0.0
    private lateinit var payNowButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var map: MapView
    private var customerPoint: GeoPoint? = null
    private var technicianPoint: GeoPoint? = null
    private var technicianEmail: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 10000L // 10 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_technician)

        Checkout.preload(applicationContext)

        val techNameText = findViewById<TextView>(R.id.technicianName)
        statusText = findViewById(R.id.statusText)
        payNowButton = findViewById(R.id.payNowButton)
        
        // Initialize OSM
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        
        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        val bookedTechName = intent.getStringExtra("TECH_NAME") ?: sharedPref.getString("BOOKED_TECH_NAME", "Technician")
        technicianEmail = intent.getStringExtra("TECH_EMAIL") ?: ""

        techNameText.text = bookedTechName

        if (bookingId != -1) {
            fetchBookingDetails()
            startLocationPolling()
        }

        payNowButton.setOnClickListener {
            startPayment()
        }

        payNowButton.setOnLongClickListener {
            performMockPayment()
            true
        }

        findViewById<MaterialButton>(R.id.bottomHomeButton).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<MaterialButton>(R.id.callButton).setOnClickListener {
            Toast.makeText(this, "Calling $bookedTechName...", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.chatButton).setOnClickListener {
            technicianEmail?.let { email ->
                if (email.isNotEmpty()) {
                    val intent = Intent(this, ChatDetailActivity::class.java).apply {
                        putExtra("OTHER_USER_EMAIL", email)
                        putExtra("OTHER_USER_NAME", bookedTechName)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Technician contact info not available", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Technician contact info not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBookingDetails() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        val apiService = RetrofitClient.createService(ApiService::class.java)

        apiService.getActiveBookings("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val booking = response.body()?.find { it.id == bookingId }
                    booking?.let {
                        updateUI(it)
                    }
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "Error fetching booking details: ${t.message}")
            }
        })
    }

    private fun updateUI(booking: BookingResponse) {
        statusText.text = booking.status
        
        // Extract cost as Double
        val rawCost = booking.cost ?: "0"
        cost = rawCost.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0

        if (booking.status == "Completed") {
            payNowButton.visibility = View.VISIBLE
            statusText.text = "Job Completed - Pending Payment"
        } else {
            payNowButton.visibility = View.GONE
        }

        // Geocode customer address
        booking.address?.let { addr ->
            geocodeAddress(addr) { geoPoint ->
                customerPoint = geoPoint
                updateMarkers()
            }
        }
    }

    private fun startLocationPolling() {
        handler.post(object : Runnable {
            override fun run() {
                technicianEmail?.let { email ->
                    if (email.isNotEmpty()) {
                        fetchTechnicianLocation(email)
                    }
                }
                handler.postDelayed(this, pollingInterval)
            }
        })
    }

    private fun fetchTechnicianLocation(email: String) {
        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getTechnicianLocation(email).enqueue(object : Callback<LocationResponse> {
            override fun onResponse(call: Call<LocationResponse>, response: Response<LocationResponse>) {
                if (response.isSuccessful) {
                    val loc = response.body()
                    if (loc?.latitude != null && loc.longitude != null) {
                        technicianPoint = GeoPoint(loc.latitude.toDouble(), loc.longitude.toDouble())
                        updateMarkers()
                    }
                }
            }
            override fun onFailure(call: Call<LocationResponse>, t: Throwable) {
                Log.e("FixItNow", "Error polling technician location: ${t.message}")
            }
        })
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

    private fun updateMarkers() {
        if (!::map.isInitialized) return
        map.overlays.clear()

        customerPoint?.let {
            val marker = Marker(map)
            marker.position = it
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Your Location"
            map.overlays.add(marker)
        }

        technicianPoint?.let {
            val marker = Marker(map)
            marker.position = it
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Technician"
            marker.icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default)
            map.overlays.add(marker)
        }

        // Zoom to show both
        if (customerPoint != null && technicianPoint != null) {
            val points = ArrayList<GeoPoint>()
            points.add(customerPoint!!)
            points.add(technicianPoint!!)
            map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(points), true, 150)
        } else if (customerPoint != null) {
            map.controller.setZoom(15.0)
            map.controller.setCenter(customerPoint)
        }
        
        map.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    private fun startPayment() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        val apiService = RetrofitClient.createService(ApiService::class.java)

        val request = RazorpayOrderRequest(bookingId, cost)
        apiService.createRazorpayOrder("Bearer $token", request).enqueue(object : Callback<RazorpayOrderResponse> {
            override fun onResponse(call: Call<RazorpayOrderResponse>, response: Response<RazorpayOrderResponse>) {
                if (response.isSuccessful) {
                    val order = response.body()
                    order?.let { openRazorpayCheckout(it) }
                } else {
                    Toast.makeText(this@TrackTechnicianActivity, "Failed to create order", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<RazorpayOrderResponse>, t: Throwable) {
                Toast.makeText(this@TrackTechnicianActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openRazorpayCheckout(order: RazorpayOrderResponse) {
        val checkout = Checkout()
        checkout.setKeyID(order.keyId)

        try {
            val options = JSONObject()
            options.put("name", "FIXIT NOW")
            options.put("description", "Service Payment")
            options.put("order_id", order.orderId)
            options.put("theme.color", "#2196F3")
            options.put("currency", order.currency)
            options.put("amount", order.amount)

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("FixItNow", "Error in Razorpay Checkout", e)
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        val apiService = RetrofitClient.createService(ApiService::class.java)

        val verificationRequest = PaymentVerificationRequest(
            bookingId,
            razorpayPaymentId ?: "",
            paymentData?.orderId ?: "",
            paymentData?.signature ?: ""
        )

        apiService.verifyPayment("Bearer $token", verificationRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@TrackTechnicianActivity, "Payment Successful!", Toast.LENGTH_LONG).show()
                    finish() // Close activity on success
                } else {
                    Toast.makeText(this@TrackTechnicianActivity, "Payment verification failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@TrackTechnicianActivity, "Verification error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_LONG).show()
    }

    private fun performMockPayment() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        val apiService = RetrofitClient.createService(ApiService::class.java)

        apiService.mockPay("Bearer $token", bookingId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@TrackTechnicianActivity, "Mock Payment Successful!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@TrackTechnicianActivity, "Mock payment failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@TrackTechnicianActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
