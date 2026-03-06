package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.simats.fixitnow.network.UploadResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private var selectedImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadProfilePhoto()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        loadUserData(view)
        setupRows(view)
        return view
    }

    private fun loadUserData(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        if (token.isEmpty()) return
        
        val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
        
        // Load cached profile pic immediately for better UX
        val profileImage = view.findViewById<ImageView>(R.id.profileImage)
        val cachedUrl = sharedPref.getString("PROFILE_PIC_URL", null)
        if (!cachedUrl.isNullOrEmpty()) {
            val cleanedPath = cachedUrl.removePrefix("/")
            val fullUrl = "${com.simats.fixitnow.network.RetrofitClient.BASE_URL}$cleanedPath"
            Glide.with(this@ProfileFragment)
                .load(fullUrl)
                .placeholder(R.drawable.ic_person)
                .into(profileImage)
        }

        apiService.getCustomerProfile("Bearer $token").enqueue(object : retrofit2.Callback<com.simats.fixitnow.network.CustomerProfileResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.fixitnow.network.CustomerProfileResponse>, response: retrofit2.Response<com.simats.fixitnow.network.CustomerProfileResponse>) {
                if (response.isSuccessful) {
                    val profile = response.body()
                    view.findViewById<TextView>(R.id.userName)?.text = profile?.fullName ?: "Name not set"
                    view.findViewById<TextView>(R.id.userEmail)?.text = profile?.email ?: "Email not set"
                    val backendAddress = profile?.address
                    val displayAddress = if (!backendAddress.isNullOrBlank()) {
                        backendAddress
                    } else {
                        sharedPref.getString("USER_LOCATION", "No address set")
                    }
                    view.findViewById<TextView>(R.id.userAddress)?.text = displayAddress
                    view.findViewById<TextView>(R.id.userPhone)?.text = profile?.phone ?: "Phone not set"
                    
                    val profileImage = view.findViewById<ImageView>(R.id.profileImage)
                    if (!profile?.profilePicUrl.isNullOrEmpty()) {
                        val cleanedPath = profile?.profilePicUrl?.removePrefix("/")
                        val fullUrl = "${com.simats.fixitnow.network.RetrofitClient.BASE_URL}$cleanedPath"
                        Glide.with(this@ProfileFragment)
                            .load(fullUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(profileImage)
                    } else {
                        profileImage?.setImageResource(R.drawable.ic_person)
                    }

                    profile?.language?.let { setupRow(view.findViewById(R.id.rowLanguage), "Language", it, R.drawable.ic_language) }
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.fixitnow.network.CustomerProfileResponse>, t: Throwable) {
                 view.findViewById<TextView>(R.id.userName)?.text = "Error Loading"
            }
        })
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
        
        val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
        
        apiService.uploadProfilePic("Bearer $token", body).enqueue(object : retrofit2.Callback<UploadResponse> {
            override fun onResponse(call: retrofit2.Call<UploadResponse>, response: retrofit2.Response<UploadResponse>) {
                if (response.isSuccessful) {
                    val url = response.body()?.url
                    val profileImage = view?.findViewById<ImageView>(R.id.profileImage)
                    if (profileImage != null && url != null) {
                        val cleanedPath = url.removePrefix("/")
                        Glide.with(this@ProfileFragment)
                            .load("${com.simats.fixitnow.network.RetrofitClient.BASE_URL}$cleanedPath")
                            .placeholder(R.drawable.ic_person)
                            .into(profileImage)
                    }
                    Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<UploadResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context?.contentResolver ?: return null
        val filePath = context?.cacheDir?.absolutePath + "/temp_profile_" + System.currentTimeMillis() + ".jpg"
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

    private fun setupRows(view: View) {
        // Set click listeners for settings rows
        view.findViewById<View>(R.id.editProfileButton)?.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        view.findViewById<View>(R.id.profileImage)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<View>(R.id.rowPayment)?.setOnClickListener {
            startActivity(Intent(requireContext(), PaymentMethodsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowHelp)?.setOnClickListener {
            startActivity(Intent(requireContext(), AiChatActivity::class.java))
        }

        view.findViewById<View>(R.id.rowNotifications)?.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowPrivacy)?.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacySecurityActivity::class.java))
        }

        view.findViewById<View>(R.id.rowFavorites)?.setOnClickListener {
            startActivity(Intent(requireContext(), FavoritesActivity::class.java))
        }

        view.findViewById<View>(R.id.rowReviews)?.setOnClickListener {
            startActivity(Intent(requireContext(), MyReviewsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowTerms)?.setOnClickListener {
            startActivity(Intent(requireContext(), TermsConditionsActivity::class.java))
        }

        view.findViewById<View>(R.id.rowSettings)?.setOnClickListener {
            startActivity(Intent(requireContext(), AppSettingsActivity::class.java))
        }
        
        // Account Settings
        setupRow(view.findViewById(R.id.rowPayment), "Payment Options", "Manage your cards", R.drawable.ic_wallet)
        setupRow(view.findViewById(R.id.rowNotifications), "Notifications", "Alerts & preferences", R.drawable.ic_notifications)
        setupRow(view.findViewById(R.id.rowPrivacy), "Privacy & Security", "Password, 2FA, data", R.drawable.ic_privacy)
        setupRow(view.findViewById(R.id.rowLanguage), "Language", "English (US)", R.drawable.ic_language)

        // More
        setupRow(view.findViewById(R.id.rowFavorites), "Favorite Technicians", "Your saved pros", R.drawable.ic_favorite)
        setupRow(view.findViewById(R.id.rowReviews), "My Reviews", "View all ratings", R.drawable.ic_star)

        // Support
        setupRow(view.findViewById(R.id.rowHelp), "Help Center", "FAQs & support", R.drawable.ic_help)
        setupRow(view.findViewById(R.id.rowTerms), "Terms & Conditions", "Legal information", R.drawable.ic_description_white)
        setupRow(view.findViewById(R.id.rowSettings), "App Settings", "Preferences & cache", R.drawable.ic_wrench)

        view.findViewById<View>(R.id.logoutButton).setOnClickListener {
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
