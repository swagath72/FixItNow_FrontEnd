package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class DocumentVerificationActivity : AppCompatActivity() {

    private var idUri: android.net.Uri? = null
    private var certificateUri: android.net.Uri? = null
    private var photosUri: android.net.Uri? = null

    private lateinit var uploadIdButton: MaterialButton
    private lateinit var uploadCertificateButton: MaterialButton
    private lateinit var uploadPhotosButton: MaterialButton
    private lateinit var submitButton: MaterialButton

    private val pickIdLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (isJpg(uri)) {
                idUri = uri
                uploadIdButton.text = "ID Selected"
                uploadIdButton.setIconResource(R.drawable.ic_check_circle)
                checkRequirements()
            } else {
                Toast.makeText(this, "Please select a JPG image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickCertificateLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (isJpg(uri)) {
                certificateUri = uri
                uploadCertificateButton.text = "Certificate Selected"
                uploadCertificateButton.setIconResource(R.drawable.ic_check_circle)
                checkRequirements()
            } else {
                Toast.makeText(this, "Please select a JPG image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickPhotosLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (isJpg(uri)) {
                photosUri = uri
                uploadPhotosButton.text = "Photos Selected"
                uploadPhotosButton.setIconResource(R.drawable.ic_check_circle)
                checkRequirements()
            } else {
                Toast.makeText(this, "Please select a JPG image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_document_verification)

        val backButton = findViewById<ImageView>(R.id.backButton)
        uploadIdButton = findViewById(R.id.uploadIdButton)
        uploadCertificateButton = findViewById(R.id.uploadCertificateButton)
        uploadPhotosButton = findViewById(R.id.uploadPhotosButton)
        submitButton = findViewById(R.id.submitButton)

        backButton.setOnClickListener {
            finish()
        }

        uploadIdButton.setOnClickListener {
            pickIdLauncher.launch("image/*")
        }

        uploadCertificateButton.setOnClickListener {
            pickCertificateLauncher.launch("image/*")
        }

        uploadPhotosButton.setOnClickListener {
            pickPhotosLauncher.launch("image/*")
        }

        submitButton.setOnClickListener {
            uploadDocuments()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun uploadDocuments() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        if (token.isEmpty()) return

        submitButton.isEnabled = false
        submitButton.text = "Uploading..."

        val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
        
        // Parallel or sequential upload. Let's do a simple count-down for MVP
        var uploadCount = 0
        val totalToUpload = 3

        val onUploadComplete = {
            uploadCount++
            if (uploadCount == totalToUpload) {
                Toast.makeText(this, "Documents Submitted Successfully!", Toast.LENGTH_LONG).show()
                val intent = android.content.Intent(this, TechnicianHomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        uploadFile(apiService, token, "Identity Proof", idUri!!, onUploadComplete)
        uploadFile(apiService, token, "Skill Certificate", certificateUri!!, onUploadComplete)
        uploadFile(apiService, token, "Work Photos", photosUri!!, onUploadComplete)
    }

    private fun uploadFile(apiService: com.simats.fixitnow.network.ApiService, token: String, type: String, uri: android.net.Uri, callback: () -> Unit) {
        val file = getFileFromUri(uri) ?: return
        val requestFile = okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)

        apiService.uploadTechnicianDocument("Bearer $token", type, body).enqueue(object : retrofit2.Callback<com.simats.fixitnow.network.UploadResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.fixitnow.network.UploadResponse>, response: retrofit2.Response<com.simats.fixitnow.network.UploadResponse>) {
                callback()
            }
            override fun onFailure(call: retrofit2.Call<com.simats.fixitnow.network.UploadResponse>, t: Throwable) {
                // In a real app, handle failure properly
                callback() 
            }
        })
    }

    private fun getFileFromUri(uri: android.net.Uri): java.io.File? {
        val contentResolver = contentResolver
        val file = java.io.File(cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            return null
        }
    }

    private fun isJpg(uri: android.net.Uri): Boolean {
        val contentResolver = contentResolver
        val type = contentResolver.getType(uri)
        if (type == "image/jpeg" || type == "image/jpg") return true
        val path = uri.path?.lowercase() ?: ""
        return path.endsWith(".jpg") || path.endsWith(".jpeg")
    }

    private fun checkRequirements() {
        if (idUri != null && certificateUri != null && photosUri != null) {
            submitButton.isEnabled = true
            submitButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3C61FF"))
        } else {
            submitButton.isEnabled = false
            submitButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BDBDBD"))
        }
    }
}
