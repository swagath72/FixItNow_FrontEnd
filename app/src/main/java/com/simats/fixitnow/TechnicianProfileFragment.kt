package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.switchmaterial.SwitchMaterial
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.TechnicianEarningsResponse
import com.simats.fixitnow.network.TechnicianProfileResponse
import com.simats.fixitnow.network.UploadResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class TechnicianProfileFragment : Fragment() {

    private var selectedImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadProfilePhoto()
        }
    }

    private lateinit var statEarnings: TextView
    private lateinit var statRating: TextView
    private lateinit var statJobs: TextView

    private lateinit var profileName: TextView
    private lateinit var profilePhone: TextView
    private lateinit var profileEmail: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_technician_profile, container, false)
        
        statEarnings = view.findViewById(R.id.statEarnings)
        statRating = view.findViewById(R.id.statRating)
        statJobs = view.findViewById(R.id.statJobs)

        profileName = view.findViewById(R.id.profileName)
        profilePhone = view.findViewById(R.id.profilePhone)
        profileEmail = view.findViewById(R.id.profileEmail)

        setupRows(view)
        setupAvailability(view)
        fetchTechnicianStats(view)
        
        view.findViewById<View>(R.id.profileImage)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        view.findViewById<View>(R.id.editProfilePhotoButton)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        return view
    }

    private fun uploadProfilePhoto() {
        val uri = selectedImageUri ?: return
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        val file = getFileFromUri(uri) ?: return
        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile = RequestBody.create(mediaType, file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        apiService.uploadProfilePic("Bearer $token", body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful) {
                    val url = response.body()?.url
                    val profileImage = view?.findViewById<ImageView>(R.id.profileImage)
                    if (profileImage != null && url != null) {
                        val cleanedPath = url.removePrefix("/")
                        Glide.with(this@TechnicianProfileFragment)
                            .load("${RetrofitClient.BASE_URL}$cleanedPath")
                            .placeholder(R.drawable.ic_person)
                            .into(profileImage)
                    }
                    Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context?.contentResolver ?: return null
        val filePath = context?.cacheDir?.absolutePath + "/temp_tech_profile_" + System.currentTimeMillis() + ".jpg"
        val file = File(filePath)
        
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                outputStream.write(buffer, 0, len)
            }
            outputStream.close()
            inputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun fetchTechnicianStats(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        // Load cached profile pic immediately for better UX
        val profileImage = view.findViewById<ImageView>(R.id.profileImage)
        val cachedUrl = sharedPref.getString("PROFILE_PIC_URL", null)
        if (!cachedUrl.isNullOrEmpty()) {
            val cleanedPath = cachedUrl.removePrefix("/")
            val fullUrl = "${RetrofitClient.BASE_URL}$cleanedPath"
            Glide.with(this@TechnicianProfileFragment)
                .load(fullUrl)
                .placeholder(R.drawable.ic_person)
                .into(profileImage)
        }

        // Fetch Earnings
        apiService.getTechnicianEarnings("Bearer $token").enqueue(object : Callback<TechnicianEarningsResponse> {
            override fun onResponse(call: Call<TechnicianEarningsResponse>, response: Response<TechnicianEarningsResponse>) {
                if (response.isSuccessful) {
                    val earnings = response.body()
                    // Assuming total earnings is desired here, or using week earnings as placeholder
                    statEarnings.text = "₹${earnings?.week ?: "0"}"
                }
            }
            override fun onFailure(call: Call<TechnicianEarningsResponse>, t: Throwable) {}
        })

        // Fetch Total Jobs Done (from history)
        apiService.getTechnicianHistory("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val history = response.body() ?: emptyList()
                    val completedCount = history.filter { it.status?.lowercase() == "completed" }.size
                    statJobs.text = completedCount.toString()
                    
                    // Update total earnings based on completed jobs if earnings API is restricted
                    if (statEarnings.text == "₹0" || statEarnings.text == "$0") {
                        var total = 0
                        for (job in history) {
                            if (job.status?.lowercase() == "completed") {
                                val costStr = job.cost?.replace(Regex("[^0-9]"), "") ?: ""
                                total += costStr.toIntOrNull() ?: 0
                            }
                        }
                        statEarnings.text = "₹$total"
                    }
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {}
        })
        // Fetch Technician Profile
        apiService.getTechnicianProfile("Bearer $token").enqueue(object : Callback<TechnicianProfileResponse> {
            override fun onResponse(call: Call<TechnicianProfileResponse>, response: Response<TechnicianProfileResponse>) {
                if (response.isSuccessful) {
                    val profile = response.body()
                    profileName.text = profile?.fullName ?: "Name not set"
                    profilePhone.text = profile?.phone ?: "Phone not set"
                    profileEmail.text = profile?.email ?: "Email not set"

                    val profileImage = view.findViewById<ImageView>(R.id.profileImage)
                    if (!profile?.profilePicUrl.isNullOrEmpty()) {
                        val cleanedPath = profile?.profilePicUrl?.removePrefix("/")
                        val fullUrl = "${com.simats.fixitnow.network.RetrofitClient.BASE_URL}$cleanedPath"
                        Glide.with(this@TechnicianProfileFragment)
                            .load(fullUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(profileImage)
                    } else {
                        profileImage?.setImageResource(R.drawable.ic_person)
                    }

                    // Update dynamic rows
                    profile?.skills?.let { setupRow(view.findViewById(R.id.rowSkills), "My Skills", it, R.drawable.ic_flash) }
                    profile?.serviceRadius?.let { setupRow(view.findViewById(R.id.rowServiceRadius), "Service Radius", it, R.drawable.ic_location_pin) }
                    profile?.workingHours?.let { setupRow(view.findViewById(R.id.rowWorkingHours), "Working Hours", it, R.drawable.ic_calendar) }
                    profile?.verificationStatus?.let { setupRow(view.findViewById(R.id.rowVerification), "Verification Status", it, R.drawable.ic_check_circle) }
                    profile?.payoutSettings?.let { setupRow(view.findViewById(R.id.rowPayouts), "Payout Settings", it, R.drawable.ic_wallet) }
                }
            }
            override fun onFailure(call: Call<TechnicianProfileResponse>, t: Throwable) {
                profileName.text = "Error loading"
            }
        })

        // Static rating for now as backend doesn't provide it yet
        statRating.text = "5.0"
    }

    private fun setupAvailability(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val switch = view.findViewById<SwitchMaterial>(R.id.availabilitySwitch)
        val subtitle = view.findViewById<TextView>(R.id.statusSubtitle)
        
        val isOnline = sharedPref.getBoolean("TECH_IS_ONLINE", true)
        switch?.isChecked = isOnline
        updateProfileOnlineStatusUI(isOnline, subtitle)

        switch?.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("TECH_IS_ONLINE", isChecked).apply()
            updateProfileOnlineStatusUI(isChecked, subtitle)
            val msg = if (isChecked) "You are now ONLINE" else "You are now OFFLINE"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            
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
    }

    private fun updateProfileOnlineStatusUI(isOnline: Boolean, subtitle: TextView?) {
        if (isOnline) {
            subtitle?.text = "Currently accepting new jobs"
            subtitle?.setTextColor(android.graphics.Color.parseColor("#757575"))
        } else {
            subtitle?.text = "Not accepting jobs right now"
            subtitle?.setTextColor(android.graphics.Color.RED)
        }
    }

    private fun setupRows(view: View) {
        // Professional Section
        setupRow(view.findViewById(R.id.rowSkills), "My Skills", "Primary: Electrician (+4 others)", R.drawable.ic_flash)
        setupRow(view.findViewById(R.id.rowServiceRadius), "Service Radius", "Current Range: 15 miles", R.drawable.ic_location_pin)
        setupRow(view.findViewById(R.id.rowWorkingHours), "Working Hours", "9:00 AM - 6:00 PM (Mon-Sat)", R.drawable.ic_calendar)
        setupRow(view.findViewById(R.id.rowVerification), "Verification Status", "Identity & License: VERIFIED", R.drawable.ic_check_circle)

        // Account Section
        setupRow(view.findViewById(R.id.rowPayouts), "Payout Settings", "Next Payout: Feb 20, 2026", R.drawable.ic_wallet)
        setupRow(view.findViewById(R.id.rowHistory), "Job History", "View all completed services", R.drawable.ic_history)
        setupRow(view.findViewById(R.id.rowNotifications), "Job Alerts", "Manage sound & vibration", R.drawable.ic_notifications)
        setupRow(view.findViewById(R.id.rowSupport), "Help & Support", "Contact technician assistance", R.drawable.ic_help)

        // Row Click Listeners
        view.findViewById<View>(R.id.rowSkills)?.setOnClickListener {
            startActivity(Intent(requireContext(), TechnicianSkillsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowServiceRadius)?.setOnClickListener {
            startActivity(Intent(requireContext(), ServiceRadiusActivity::class.java))
        }

        view.findViewById<View>(R.id.rowWorkingHours)?.setOnClickListener {
            startActivity(Intent(requireContext(), WorkingHoursActivity::class.java))
        }

        view.findViewById<View>(R.id.rowVerification)?.setOnClickListener {
            startActivity(Intent(requireContext(), VerificationStatusActivity::class.java))
        }
        
        view.findViewById<View>(R.id.rowPayouts)?.setOnClickListener {
             startActivity(Intent(requireContext(), PayoutSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), TechnicianJobHistoryActivity::class.java))
        }

        view.findViewById<View>(R.id.rowSupport)?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpSupportTechnicianActivity::class.java))
        }

        view.findViewById<View>(R.id.rowNotifications)?.setOnClickListener {
            startActivity(Intent(requireContext(), TechnicianNotificationsActivity::class.java))
        }

        view.findViewById<View>(R.id.logoutButton)?.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            Toast.makeText(context, "Logging out...", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupRow(view: View?, title: String, subtitle: String, iconRes: Int) {
        if (view == null) return
        view.findViewById<TextView>(R.id.rowTitle)?.text = title
        view.findViewById<TextView>(R.id.rowSubtitle)?.text = subtitle
        view.findViewById<ImageView>(R.id.rowIcon)?.setImageResource(iconRes)
    }
}
