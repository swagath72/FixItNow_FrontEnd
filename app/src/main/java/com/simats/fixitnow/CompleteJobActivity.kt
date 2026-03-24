package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.UpdateJobStatusRequest
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

class CompleteJobActivity : AppCompatActivity() {

    private var bookingId: Int = -1
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            val preview = findViewById<ImageView>(R.id.imagePreview)
            preview.setImageURI(uri)
            preview.visibility = View.VISIBLE
            findViewById<View>(R.id.uploadIcon).visibility = View.GONE
            findViewById<View>(R.id.uploadText).visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_job)

        bookingId = intent.getIntExtra("BOOKING_ID", -1)
        Log.d("FixItNow", "CompleteJobActivity: Received bookingId = $bookingId")
        if (bookingId == -1) {
            Toast.makeText(this, "Error: Invalid booking ID received", Toast.LENGTH_LONG).show()
        }
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val cost = intent.getStringExtra("COST")

        findViewById<TextView>(R.id.subtitleText).text = "$serviceName for $customerName"
        findViewById<TextView>(R.id.paymentText).text = "₹$cost"

        findViewById<View>(R.id.uploadCard).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<MaterialButton>(R.id.completeJobButton).setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please upload a work photo first", Toast.LENGTH_SHORT).show()
            } else {
                uploadImageAndComplete()
            }
        }

        findViewById<MaterialButton>(R.id.homeButton).setOnClickListener {
            val intent = Intent(this, TechnicianHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun uploadImageAndComplete() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        val uri = selectedImageUri ?: return
        val file = getFileFromUri(uri) ?: return
        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile = RequestBody.create(mediaType, file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        
        apiService.uploadWorkPhoto("Bearer $token", bookingId, body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful) {
                    updateStatus("Completed")
                } else {
                    val errorMsg = "Failed to upload photo (Code: ${response.code()})"
                    Log.e("FixItNow", "CompleteJobActivity: $errorMsg")
                    Toast.makeText(this@CompleteJobActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@CompleteJobActivity, "Upload Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = contentResolver
        val filePath = cacheDir.absolutePath + "/temp_image_" + System.currentTimeMillis() + ".jpg"
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

    private fun updateStatus(status: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = UpdateJobStatusRequest(bookingId, status)

        apiService.updateJobStatus("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CompleteJobActivity, "Job completed successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CompleteJobActivity, TechnicianHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = "Failed to complete job (Code: ${response.code()})"
                    Log.e("FixItNow", "CompleteJobActivity: $errorMsg")
                    Toast.makeText(this@CompleteJobActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@CompleteJobActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
