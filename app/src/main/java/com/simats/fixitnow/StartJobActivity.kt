package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.UpdateJobStatusRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StartJobActivity : AppCompatActivity() {

    private var bookingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_job)

        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
        val address = intent.getStringExtra("ADDRESS")
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val cost = intent.getStringExtra("COST")

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        // findViewById<TextView>(R.id.customerNameText).text = customerName // This was causing crash because customerNameText is not in the layout
        findViewById<TextView>(R.id.addressText).text = address

        findViewById<MaterialButton>(R.id.startJobButton).setOnClickListener {
            updateStatus("Started", customerName, serviceName, cost)
        }
    }

    private fun updateStatus(status: String, customerName: String?, serviceName: String?, cost: String?) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = UpdateJobStatusRequest(bookingId, status)

        apiService.updateJobStatus("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val intent = Intent(this@StartJobActivity, CompleteJobActivity::class.java)
                    intent.putExtra("BOOKING_ID", bookingId)
                    intent.putExtra("CUSTOMER_NAME", customerName)
                    intent.putExtra("SERVICE_NAME", serviceName)
                    intent.putExtra("COST", cost)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@StartJobActivity, "Failed to start job", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@StartJobActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
