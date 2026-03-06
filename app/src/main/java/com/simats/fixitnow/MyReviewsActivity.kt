package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MyReviewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_reviews)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        fetchReviews()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchReviews() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
        apiService.getMyReviews("Bearer $token").enqueue(object : retrofit2.Callback<List<com.simats.fixitnow.network.ReviewResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.simats.fixitnow.network.ReviewResponse>>,
                response: retrofit2.Response<List<com.simats.fixitnow.network.ReviewResponse>>
            ) {
                if (response.isSuccessful) {
                    val reviews = response.body() ?: emptyList()
                    displayReviews(reviews)
                    updateSummary(reviews)
                }
            }

            override fun onFailure(call: retrofit2.Call<List<com.simats.fixitnow.network.ReviewResponse>>, t: Throwable) {
                // Handle error
            }
        })
    }

    private fun updateSummary(reviews: List<com.simats.fixitnow.network.ReviewResponse>) {
        if (reviews.isEmpty()) return
        
        val totalReviews = reviews.size
        val avgRating = reviews.map { it.rating ?: 0f }.average()
        
        findViewById<TextView>(R.id.avgRatingText).text = String.format("%.1f", avgRating)
        findViewById<TextView>(R.id.totalReviewsText).text = totalReviews.toString()
        findViewById<TextView>(R.id.totalVotesText).text = "0" // For now
    }

    private fun displayReviews(reviews: List<com.simats.fixitnow.network.ReviewResponse>) {
        val container = findViewById<android.widget.LinearLayout>(R.id.reviewsContainer)
        container.removeAllViews()

        if (reviews.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "You haven't submitted any reviews yet."
                android.view.Gravity.CENTER
                setPadding(32, 64, 32, 64)
                setTextColor(android.graphics.Color.GRAY)
            }
            container.addView(emptyText)
            return
        }

        val inflater = android.view.LayoutInflater.from(this)
        for (review in reviews) {
            val itemView = inflater.inflate(R.layout.item_review, container, false)
            
            val reviewerName = review.technicianName ?: "Technician"
            val serviceName = review.serviceName ?: "Service"
            val comment = if (review.comment.isNullOrBlank()) "No comment provided" else review.comment
            
            android.util.Log.d("FixItNow", "Displaying Review: ID=${review.id}, Tech=$reviewerName, Comment='$comment'")

            itemView.findViewById<TextView>(R.id.reviewerName).text = reviewerName
            itemView.findViewById<TextView>(R.id.serviceName).text = serviceName
            itemView.findViewById<TextView>(R.id.reviewText).text = comment
            itemView.findViewById<TextView>(R.id.reviewDate).text = review.date
            itemView.findViewById<android.widget.RatingBar>(R.id.reviewRating).rating = review.rating ?: 0f
            itemView.findViewById<TextView>(R.id.helpfulCount).text = "${review.helpfulCount ?: 0} found this helpful"

            container.addView(itemView)
        }
    }
}
