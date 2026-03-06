package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TechnicianJobHistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var totalJobsText: TextView
    private lateinit var totalEarnedText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_technician_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.historyRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        totalJobsText = view.findViewById(R.id.totalJobsText)
        totalEarnedText = view.findViewById(R.id.totalEarnedText)

        setupRecyclerView()
        fetchJobHistory()
    }

    private fun setupRecyclerView() {
        adapter = TechnicianJobHistoryAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun fetchJobHistory() {
        if (!isAdded) return
        
        progressBar.visibility = View.VISIBLE
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        val allHistoryList = mutableListOf<BookingResponse>()
        val checkCompletion = {
            progressBar.visibility = View.GONE
            val finalCompletedList = allHistoryList.distinctBy { it.id }
                .filter { it.status?.trim()?.lowercase() == "completed" }
            
            if (finalCompletedList.isEmpty()) {
                emptyStateText.visibility = View.VISIBLE
                updateSummary(0, 0)
                adapter.updateJobs(emptyList())
            } else {
                emptyStateText.visibility = View.GONE
                adapter.updateJobs(finalCompletedList)
                calculateSummary(finalCompletedList)
            }
        }

        apiService.getTechnicianHistory("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) response.body()?.let { allHistoryList.addAll(it) }
                checkCompletion()
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) { checkCompletion() }
        })
    }

    private fun calculateSummary(jobs: List<BookingResponse>) {
        val totalJobs = jobs.size
        var totalEarned = 0
        for (job in jobs) {
            val costStr = job.cost?.replace(Regex("[^0-9]"), "") ?: ""
            totalEarned += costStr.toIntOrNull() ?: 0
        }
        updateSummary(totalJobs, totalEarned)
    }

    private fun updateSummary(totalJobs: Int, totalEarned: Int) {
        totalJobsText.text = totalJobs.toString()
        totalEarnedText.text = "₹$totalEarned"
    }
}
