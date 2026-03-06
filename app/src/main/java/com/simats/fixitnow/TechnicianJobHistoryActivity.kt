package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianJobHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TechnicianJobHistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technician_job_history)

        recyclerView = findViewById(R.id.jobHistoryRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        setupRecyclerView()
        fetchJobHistory()
    }

    private fun setupRecyclerView() {
        adapter = TechnicianJobHistoryAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun fetchJobHistory() {
        progressBar.visibility = View.VISIBLE
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getTechnicianHistory("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val jobs = response.body() ?: emptyList()
                    if (jobs.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                    } else {
                        emptyStateText.visibility = View.GONE
                        adapter.updateJobs(jobs)
                    }
                } else {
                    Toast.makeText(this@TechnicianJobHistoryActivity, "Failed to fetch history", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@TechnicianJobHistoryActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
