package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.PendingTechnician
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: LinearLayout
    private lateinit var logoutButton: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: AdminTechnicianAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.techniciansRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        logoutButton = findViewById(R.id.logoutButton)
        tabLayout = findViewById(R.id.tabLayout)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> fetchPendingTechnicians()
                    1 -> fetchApprovedTechnicians()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminTechnicianAdapter(emptyList()) { technician ->
            val intent = Intent(this, AdminVerificationActivity::class.java)
            intent.putExtra("TECHNICIAN_ID", technician.id)
            intent.putExtra("FULL_NAME", technician.full_name)
            intent.putExtra("EMAIL", technician.email)
            intent.putExtra("PHONE", technician.phone)
            intent.putExtra("SKILLS", technician.skills)
            intent.putExtra("EXPERIENCE", technician.experience)
            intent.putExtra("PROFILE_PIC_URL", technician.profile_pic_url)
            intent.putExtra("STATUS", technician.verification_status)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        logoutButton.setOnClickListener {
            val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (tabLayout.selectedTabPosition == 0) {
            fetchPendingTechnicians()
        } else {
            fetchApprovedTechnicians()
        }
    }

    private fun fetchPendingTechnicians() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null) ?: return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getPendingTechnicians("Bearer $token").enqueue(object : Callback<List<PendingTechnician>> {
            override fun onResponse(
                call: Call<List<PendingTechnician>>,
                response: Response<List<PendingTechnician>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val techs = response.body()!!
                    if (techs.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        emptyView.findViewById<TextView>(R.id.emptyTitleText)?.text = "All technicians verified!"
                        emptyView.findViewById<TextView>(R.id.emptySubtitleText)?.text = "No pending requests at the moment."
                    } else {
                        adapter.updateList(techs)
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Failed to load pending technicians", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PendingTechnician>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchApprovedTechnicians() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null) ?: return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getApprovedTechnicians("Bearer $token").enqueue(object : Callback<List<PendingTechnician>> {
            override fun onResponse(
                call: Call<List<PendingTechnician>>,
                response: Response<List<PendingTechnician>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val techs = response.body()!!
                    if (techs.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        emptyView.findViewById<TextView>(R.id.emptyTitleText)?.text = "No approved technicians"
                        emptyView.findViewById<TextView>(R.id.emptySubtitleText)?.text = "Start verifying technicians to see them here."
                    } else {
                        adapter.updateList(techs)
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Failed to load approved technicians", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PendingTechnician>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
