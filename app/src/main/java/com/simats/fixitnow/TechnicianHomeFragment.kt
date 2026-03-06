package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.TechnicianEarningsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianHomeFragment : Fragment() {

    private lateinit var todayEarnings: TextView
    private lateinit var weekEarnings: TextView
    private lateinit var monthEarnings: TextView
    private lateinit var requestCountText: TextView
    private lateinit var jobRequestsContainer: LinearLayout
    private lateinit var userNameText: TextView
    private lateinit var locationText: TextView
    private lateinit var activeJobsViewPager: ViewPager2
    private lateinit var jobIndicator: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_technician_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        todayEarnings = view.findViewById(R.id.todayEarnings)
        weekEarnings = view.findViewById(R.id.weekEarnings)
        monthEarnings = view.findViewById(R.id.monthEarnings)
        requestCountText = view.findViewById(R.id.requestCountText)
        jobRequestsContainer = view.findViewById(R.id.jobRequestsContainer)
        userNameText = view.findViewById(R.id.userNameText)
        locationText = view.findViewById(R.id.locationText)
        activeJobsViewPager = view.findViewById(R.id.activeJobsViewPager)
        jobIndicator = view.findViewById(R.id.jobIndicator)

        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        userNameText.text = sharedPref.getString("USER_NAME", "Technician")
        locationText.text = sharedPref.getString("USER_DISPLAY_LOCATION", "Set your location")

        val onlineSwitch = view.findViewById<SwitchMaterial>(R.id.onlineSwitch)
        val statusText = view.findViewById<TextView>(R.id.statusText)

        // Load saved state
        val isOnline = sharedPref.getBoolean("TECH_IS_ONLINE", true)
        onlineSwitch.isChecked = isOnline
        updateOnlineStatusUI(isOnline, statusText)

        onlineSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("TECH_IS_ONLINE", isChecked).apply()
            updateOnlineStatusUI(isChecked, statusText)
            
            // Sync with backend
            val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
            if (token.isNotEmpty()) {
                val apiService = RetrofitClient.createService(ApiService::class.java)
                val request = com.simats.fixitnow.network.UpdateStatusRequest(isChecked)
                apiService.updateTechnicianStatus("Bearer $token", request).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (!response.isSuccessful) {
                            Log.e("FixItNow", "Status sync failed")
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("FixItNow", "Status sync error: ${t.message}")
                    }
                })
            }
        }

        fetchData()
    }

    private fun updateOnlineStatusUI(isOnline: Boolean, statusText: TextView) {
        if (isOnline) {
            statusText.text = "Accepting jobs"
            statusText.setTextColor(Color.WHITE)
            statusText.alpha = 1.0f
        } else {
            statusText.text = "Not accepting jobs"
            statusText.setTextColor(Color.WHITE)
            statusText.alpha = 0.7f
        }
    }

    private fun fetchData() {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)

        // Fetch Earnings
        apiService.getTechnicianEarnings("Bearer $token").enqueue(object : Callback<TechnicianEarningsResponse> {
            override fun onResponse(call: Call<TechnicianEarningsResponse>, response: Response<TechnicianEarningsResponse>) {
                if (response.isSuccessful) {
                    val earnings = response.body()
                    todayEarnings.text = "₹${earnings?.today ?: "0"}"
                    weekEarnings.text = "₹${earnings?.week ?: "0"}"
                    monthEarnings.text = "₹${earnings?.month ?: "0"}"
                }
            }
            override fun onFailure(call: Call<TechnicianEarningsResponse>, t: Throwable) {
                Log.e("FixItNow", "Earnings Error: ${t.message}")
            }
        })

        // Fetch New Job Requests
        apiService.getTechnicianJobs("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val jobs = response.body() ?: emptyList()
                    displayJobs(jobs)
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "Jobs Error: ${t.message}")
            }
        })

        // Fetch Active (Accepted/Started) Jobs
        apiService.getTechnicianActiveJobs("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val activeJobs = response.body() ?: emptyList()
                    setupActiveJobsViewPager(activeJobs)
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "Active Jobs Error: ${t.message}")
            }
        })
    }

    private fun displayJobs(jobs: List<BookingResponse>) {
        if (!isAdded) return
        jobRequestsContainer.removeAllViews()
        
        if (jobs.isEmpty()) {
            requestCountText.visibility = View.GONE
            return
        }

        requestCountText.visibility = View.VISIBLE
        requestCountText.text = jobs.size.toString()

        val inflater = LayoutInflater.from(requireContext())
        for (job in jobs) {
            val cardView = inflater.inflate(R.layout.booking_card, jobRequestsContainer, false)
            
            cardView.findViewById<TextView>(R.id.bookingTitle).text = job.serviceName ?: "Service Request"
            cardView.findViewById<TextView>(R.id.bookingDate).text = "${job.date} at ${job.time}"
            
            val statusView = cardView.findViewById<TextView>(R.id.bookingStatus)
            statusView.text = "₹${job.cost ?: "0"}"
            statusView.setBackgroundResource(R.drawable.ic_location_background)
            statusView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
            statusView.setTextColor(Color.parseColor("#2E7D32"))

            cardView.setOnClickListener {
                val intent = Intent(requireContext(), JobRequestDetailsActivity::class.java)
                intent.putExtra("BOOKING_ID", job.id)
                intent.putExtra("CUSTOMER_NAME", job.customerName)
                intent.putExtra("SERVICE_NAME", job.serviceName)
                intent.putExtra("DESCRIPTION", job.description)
                intent.putExtra("ADDRESS", job.address)
                intent.putExtra("SCHEDULED_TIME", "${job.date} ${job.time}")
                intent.putExtra("COST", job.cost)
                startActivity(intent)
            }

            jobRequestsContainer.addView(cardView)
        }
    }

    private fun setupActiveJobsViewPager(jobs: List<BookingResponse>) {
        if (jobs.isEmpty()) {
            activeJobsViewPager.visibility = View.GONE
            jobIndicator.visibility = View.GONE
            return
        }

        activeJobsViewPager.visibility = View.VISIBLE
        jobIndicator.visibility = View.VISIBLE

        val adapter = BookingPagerAdapter(jobs) { job ->
            val nextActivity = if (job.status == "Accepted") {
                StartJobActivity::class.java
            } else {
                CompleteJobActivity::class.java
            }
            val intent = Intent(requireContext(), nextActivity)
            intent.putExtra("BOOKING_ID", job.id)
            intent.putExtra("CUSTOMER_NAME", job.customerName)
            intent.putExtra("SERVICE_NAME", job.serviceName)
            intent.putExtra("DESCRIPTION", job.description)
            intent.putExtra("ADDRESS", job.address)
            intent.putExtra("COST", job.cost)
            startActivity(intent)
        }
        
        activeJobsViewPager.adapter = adapter
        TabLayoutMediator(jobIndicator, activeJobsViewPager) { _, _ -> }.attach()
    }
}
