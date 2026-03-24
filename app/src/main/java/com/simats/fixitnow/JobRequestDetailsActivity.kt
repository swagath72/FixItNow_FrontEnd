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

class JobRequestDetailsActivity : AppCompatActivity() {

    private var bookingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_request_details)

        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
        val customerEmail = intent.getStringExtra("CUSTOMER_EMAIL")
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val description = intent.getStringExtra("DESCRIPTION")
        val address = intent.getStringExtra("ADDRESS")
        val scheduledTime = intent.getStringExtra("SCHEDULED_TIME")
        val cost = intent.getStringExtra("COST")

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        findViewById<TextView>(R.id.customerNameText).text = customerName
        findViewById<TextView>(R.id.serviceNameText).text = serviceName
        findViewById<TextView>(R.id.descriptionText).text = description
        findViewById<TextView>(R.id.addressText).text = address
        findViewById<TextView>(R.id.scheduledTimeText).text = scheduledTime
        findViewById<TextView>(R.id.earningsText).text = "₹$cost"

        findViewById<MaterialButton>(R.id.acceptButton).setOnClickListener {
            updateStatus("Accepted", customerName, customerEmail, serviceName, description, address, scheduledTime, cost)
        }

        findViewById<MaterialButton>(R.id.rejectButton).setOnClickListener {
            updateStatus("Rejected", null, null, null, null, null, null, null)
        }
    }

    private fun updateStatus(status: String, customerName: String?, customerEmail: String?, serviceName: String?, description: String?, address: String?, time: String?, cost: String?) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = UpdateJobStatusRequest(bookingId, status)

        apiService.updateJobStatus("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@JobRequestDetailsActivity, "Job $status", Toast.LENGTH_SHORT).show()
                    if (status == "Accepted") {
                        val intent = Intent(this@JobRequestDetailsActivity, StartJobActivity::class.java)
                        intent.putExtra("BOOKING_ID", bookingId)
                        intent.putExtra("CUSTOMER_NAME", customerName)
                        intent.putExtra("CUSTOMER_EMAIL", customerEmail)
                        intent.putExtra("SERVICE_NAME", serviceName)
                        intent.putExtra("DESCRIPTION", description)
                        intent.putExtra("ADDRESS", address)
                        intent.putExtra("SCHEDULED_TIME", time)
                        intent.putExtra("COST", cost)
                        startActivity(intent)
                    }
                    finish()
                } else {
                    Toast.makeText(this@JobRequestDetailsActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@JobRequestDetailsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
