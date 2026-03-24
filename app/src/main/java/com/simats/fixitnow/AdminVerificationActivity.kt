package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.TechnicianDocument
import com.simats.fixitnow.network.VerifyRequest
import com.simats.fixitnow.network.VerifyResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminVerificationActivity : AppCompatActivity() {
    private var technicianId: Int = -1
    private lateinit var documentsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AdminDocumentAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_verification)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        technicianId = intent.getIntExtra("TECHNICIAN_ID", -1)
        if (technicianId == -1) {
            Toast.makeText(this, "Error: Unknown Technician", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        fetchDocuments()
    }

    private fun setupUI() {
        // Headers & Back
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        
        // Data from intent
        val fullName = intent.getStringExtra("FULL_NAME") ?: "Unknown"
        val email = intent.getStringExtra("EMAIL") ?: ""
        val phone = intent.getStringExtra("PHONE") ?: "No phone"
        val skills = intent.getStringExtra("SKILLS") ?: "No skills provided"
        val experience = intent.getStringExtra("EXPERIENCE") ?: "No experience provided"
        val profileUrl = intent.getStringExtra("PROFILE_PIC_URL")
        val status = intent.getStringExtra("STATUS") ?: ""
        
        if (status.equals("approved", ignoreCase = true)) {
            findViewById<View>(R.id.bottomActions).visibility = View.GONE
        }
        
        findViewById<TextView>(R.id.techNameText).text = fullName
        findViewById<TextView>(R.id.techEmailText).text = email
        findViewById<TextView>(R.id.techPhoneText).text = phone
        findViewById<TextView>(R.id.techSkillsText).text = skills
        findViewById<TextView>(R.id.techExperienceText).text = experience
        
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
        if (!profileUrl.isNullOrEmpty()) {
            val fileUrl = if (profileUrl.startsWith("/")) profileUrl else "/$profileUrl"
            val fullUrl = "$baseUrl$fileUrl"
            
            Glide.with(this)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_placeholder)
                .into(profileImage)

            profileImage.setOnClickListener {
                val intent = android.content.Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("IMAGE_URL", fullUrl)
                startActivity(intent)
            }
        }
        
        // Documents Recycler
        documentsRecyclerView = findViewById(R.id.documentsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        
        documentsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = AdminDocumentAdapter(emptyList())
        documentsRecyclerView.adapter = adapter
        
        // Actions
        findViewById<Button>(R.id.approveBtn).setOnClickListener { verifyTechnician("approved") }
        findViewById<Button>(R.id.rejectBtn).setOnClickListener { verifyTechnician("rejected") }
    }

    private fun fetchDocuments() {
        progressBar.visibility = View.VISIBLE
        documentsRecyclerView.visibility = View.GONE
        
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null) ?: return
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getTechnicianDocuments("Bearer $token", technicianId).enqueue(object : Callback<List<TechnicianDocument>> {
            override fun onResponse(
                call: Call<List<TechnicianDocument>>,
                response: Response<List<TechnicianDocument>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val docs = response.body()!!
                    if (docs.isEmpty()) {
                        findViewById<LinearLayout>(R.id.emptyDocsView).visibility = View.VISIBLE
                    } else {
                        adapter.updateList(docs)
                        documentsRecyclerView.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@AdminVerificationActivity, "Failed to load documents", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<TechnicianDocument>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminVerificationActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verifyTechnician(status: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null) ?: return
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val req = VerifyRequest(status)
        
        findViewById<ProgressBar>(R.id.actionProgressBar).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.actionsLayout).visibility = View.GONE
        
        apiService.verifyTechnician("Bearer $token", technicianId, req).enqueue(object : Callback<VerifyResponse> {
            override fun onResponse(call: Call<VerifyResponse>, response: Response<VerifyResponse>) {
                findViewById<ProgressBar>(R.id.actionProgressBar).visibility = View.GONE
                findViewById<LinearLayout>(R.id.actionsLayout).visibility = View.VISIBLE
                
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminVerificationActivity, "Technician $status", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@AdminVerificationActivity, "Failed to $status technician", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<VerifyResponse>, t: Throwable) {
                findViewById<ProgressBar>(R.id.actionProgressBar).visibility = View.GONE
                findViewById<LinearLayout>(R.id.actionsLayout).visibility = View.VISIBLE
                Toast.makeText(this@AdminVerificationActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
