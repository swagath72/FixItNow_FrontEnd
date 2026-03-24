package com.simats.fixitnow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.simats.fixitnow.databinding.ActivityEditProfileBinding
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.UpdateProfileRequest
import com.simats.fixitnow.network.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var profileImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                profileImageUri = imageUri
                binding.profileImage.setImageURI(imageUri)
                uploadProfilePhoto(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.manageAddressesCard.setOnClickListener {
            startActivity(Intent(this, ManageAddressActivity::class.java))
        }

        binding.editProfilePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImage.launch(intent)
        }

        binding.saveChangesButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        // Initial load from cache or placeholders
        val cachedName = sharedPref.getString("USER_FULL_NAME", "")
        val cachedEmail = sharedPref.getString("USER_EMAIL", "")
        val cachedPhone = sharedPref.getString("USER_PHONE", "")
        val cachedPhoto = sharedPref.getString("PROFILE_PIC_URL", "")

        if (!cachedName.isNullOrEmpty()) binding.editName.setText(cachedName)
        if (!cachedEmail.isNullOrEmpty()) binding.editEmail.setText(cachedEmail)
        if (!cachedPhone.isNullOrEmpty()) binding.editPhone.setText(cachedPhone)
        
        if (!cachedPhoto.isNullOrEmpty()) {
            val cleanedPath = cachedPhoto.removePrefix("/")
            Glide.with(this)
                .load("${RetrofitClient.BASE_URL}$cleanedPath")
                .placeholder(R.drawable.ic_person)
                .into(binding.profileImage)
        }

        val role = sharedPref.getString("USER_ROLE", "customer")
        if (role == "technician") {
            apiService.getTechnicianProfile("Bearer $token").enqueue(object : Callback<com.simats.fixitnow.network.TechnicianProfileResponse> {
                override fun onResponse(call: Call<com.simats.fixitnow.network.TechnicianProfileResponse>, response: Response<com.simats.fixitnow.network.TechnicianProfileResponse>) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        binding.editName.setText(profile?.fullName)
                        binding.editPhone.setText(profile?.phone)
                        binding.editEmail.setText(profile?.email)
                        binding.textAddress.text = profile?.address ?: "No address set"
                        
                        profile?.profilePicUrl?.let { url ->
                            val cleanedPath = url.removePrefix("/")
                            Glide.with(this@EditProfileActivity)
                                .load("${RetrofitClient.BASE_URL}$cleanedPath")
                                .placeholder(R.drawable.ic_person)
                                .into(binding.profileImage)
                        }
                    }
                }
                override fun onFailure(call: Call<com.simats.fixitnow.network.TechnicianProfileResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            apiService.getCustomerProfile("Bearer $token").enqueue(object : Callback<com.simats.fixitnow.network.CustomerProfileResponse> {
                override fun onResponse(call: Call<com.simats.fixitnow.network.CustomerProfileResponse>, response: Response<com.simats.fixitnow.network.CustomerProfileResponse>) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        binding.editName.setText(profile?.fullName)
                        binding.editPhone.setText(profile?.phone)
                        binding.editEmail.setText(profile?.email)
                        binding.textAddress.text = profile?.address ?: "No address set"
                        
                        profile?.profilePicUrl?.let { url ->
                            val cleanedPath = url.removePrefix("/")
                            Glide.with(this@EditProfileActivity)
                                .load("${RetrofitClient.BASE_URL}$cleanedPath")
                                .placeholder(R.drawable.ic_person)
                                .into(binding.profileImage)
                        }
                    }
                }
                override fun onFailure(call: Call<com.simats.fixitnow.network.CustomerProfileResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveChanges() {
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.length != 10) {
            binding.editPhone.error = "Phone number must be 10 digits"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.error = "Please enter a valid email address"
            return
        }

        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        if (token.isEmpty()) return

        binding.saveChangesButton.isEnabled = false
        binding.saveChangesButton.text = "Saving..."

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = UpdateProfileRequest(name, phone, email)

        apiService.updateProfile("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                binding.saveChangesButton.isEnabled = true
                binding.saveChangesButton.text = "Save Changes"
                
                if (response.isSuccessful) {
                    sharedPref.edit().apply {
                        putString("USER_FULL_NAME", name)
                        putString("USER_NAME", name)
                        putString("USER_PHONE", phone)
                        putString("USER_EMAIL", email)
                        apply()
                    }
                    Toast.makeText(this@EditProfileActivity, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                binding.saveChangesButton.isEnabled = true
                binding.saveChangesButton.text = "Save Changes"
                Toast.makeText(this@EditProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadProfilePhoto(uri: Uri) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
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
                    if (url != null) {
                        sharedPref.edit().putString("PROFILE_PIC_URL", url).apply()
                    }
                    Toast.makeText(this@EditProfileActivity, "Profile photo updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Photo upload failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = contentResolver
        val filePath = cacheDir.absolutePath + "/temp_edit_profile_" + System.currentTimeMillis() + ".jpg"
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
}
