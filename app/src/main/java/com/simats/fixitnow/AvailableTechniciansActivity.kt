package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.TechnicianResponse
import com.simats.fixitnow.network.CreateBookingRequest
import com.simats.fixitnow.network.CreateBookingResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

class AvailableTechniciansActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TechnicianAdapter
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var technicianList: List<TechnicianResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_available_technicians)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.techniciansRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)

        val finalBookNowButton = findViewById<MaterialButton>(R.id.finalBookNowButton)
        
        finalBookNowButton.setOnClickListener {
            if (technicianList.isNotEmpty()) {
                val randomTech = technicianList[Random.nextInt(technicianList.size)]
                
                val address = intent.getStringExtra("BOOKING_ADDRESS") ?: ""
                val date = intent.getStringExtra("BOOKING_DATE") ?: ""
                val time = intent.getStringExtra("BOOKING_TIME") ?: ""
                val description = intent.getStringExtra("BOOKING_DESCRIPTION") ?: ""
                val serviceName = intent.getStringExtra("BOOKING_SERVICE_NAME") ?: "Service"
                val cost = intent.getStringExtra("BOOKING_COST") ?: "0"
                
                createBooking(address, date, time, description, serviceName, cost, randomTech)
            } else {
                Toast.makeText(this, "No technicians available to book", Toast.LENGTH_SHORT).show()
            }
        }

        fetchTechnicians()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchTechnicians() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // If no permission, fetch all (or skip)
            callGetTechnicians(null, null)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callGetTechnicians(location.latitude, location.longitude)
            } else {
                callGetTechnicians(null, null)
            }
        }.addOnFailureListener {
            callGetTechnicians(null, null)
        }
    }

    private fun callGetTechnicians(lat: Double?, lng: Double?) {
        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getTechnicians(lat, lng).enqueue(object : Callback<List<TechnicianResponse>> {
            override fun onResponse(call: Call<List<TechnicianResponse>>, response: Response<List<TechnicianResponse>>) {
                if (response.isSuccessful) {
                    technicianList = response.body() ?: emptyList()
                    adapter = TechnicianAdapter(technicianList)
                    recyclerView.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<TechnicianResponse>>, t: Throwable) {
                Toast.makeText(this@AvailableTechniciansActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createBooking(address: String, date: String, time: String, description: String, serviceName: String, cost: String, tech: TechnicianResponse) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        val userName = sharedPref.getString("USER_NAME", "Customer") ?: "Customer"
        val userEmail = sharedPref.getString("USER_EMAIL", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        // Aligning with CreateBookingRequest definition in ApiService.kt
        val request = CreateBookingRequest(
            customerEmail = userEmail,
            technicianId = tech.id,
            technicianEmail = tech.email ?: "",
            technicianName = tech.fullName ?: "Technician",
            address = address,
            date = date,
            time = time,
            description = description,
            cost = cost,
            serviceName = serviceName,
            customerName = userName
        )

        apiService.createBooking("Bearer $token", request).enqueue(object : Callback<CreateBookingResponse> {
            override fun onResponse(call: Call<CreateBookingResponse>, response: Response<CreateBookingResponse>) {
                if (response.isSuccessful) {
                    with(sharedPref.edit()) {
                        putBoolean("IS_WORK_ACTIVE", true)
                        putString("BOOKED_TECH_NAME", tech.fullName ?: "Technician")
                        putString("BOOKING_STATUS", "On the way")
                        apply()
                    }
                    val bookingId = response.body()?.booking_id ?: -1
                    val intent = Intent(this@AvailableTechniciansActivity, TrackTechnicianActivity::class.java)
                    intent.putExtra("BOOKING_ID", bookingId)
                    intent.putExtra("TECH_NAME", tech.fullName)
                    intent.putExtra("TECH_EMAIL", tech.email)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = try {
                        val errorObj = JSONObject(response.errorBody()?.string() ?: "{}")
                        errorObj.optString("detail", "Server Error: ${response.code()}")
                    } catch (e: Exception) {
                        "Booking failed: ${response.code()}"
                    }
                    Toast.makeText(this@AvailableTechniciansActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<CreateBookingResponse>, t: Throwable) {
                Toast.makeText(this@AvailableTechniciansActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

class TechnicianAdapter(private val technicians: List<TechnicianResponse>) : 
    RecyclerView.Adapter<TechnicianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.technicianName)
        val rating: TextView = view.findViewById(R.id.technicianRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_technician_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tech = technicians[position]
        holder.name.text = tech.fullName ?: "Unknown"
        holder.rating.text = if (tech.distance != null) "${tech.distance} away" else "4.8 (New)"
    }

    override fun getItemCount() = technicians.size
}
