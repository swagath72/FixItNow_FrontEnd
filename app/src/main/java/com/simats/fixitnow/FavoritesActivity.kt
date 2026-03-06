package com.simats.fixitnow

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        fetchFavorites()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchFavorites() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = com.simats.fixitnow.network.RetrofitClient.createService(com.simats.fixitnow.network.ApiService::class.java)
        apiService.getFavorites("Bearer $token").enqueue(object : retrofit2.Callback<List<com.simats.fixitnow.network.FavoriteTechnicianResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.simats.fixitnow.network.FavoriteTechnicianResponse>>,
                response: retrofit2.Response<List<com.simats.fixitnow.network.FavoriteTechnicianResponse>>
            ) {
                if (response.isSuccessful) {
                    val favorites = response.body() ?: emptyList()
                    displayFavorites(favorites)
                }
            }

            override fun onFailure(call: retrofit2.Call<List<com.simats.fixitnow.network.FavoriteTechnicianResponse>>, t: Throwable) {
                // Handle error
            }
        })
    }

    private fun displayFavorites(favorites: List<com.simats.fixitnow.network.FavoriteTechnicianResponse>) {
        val container = findViewById<android.widget.LinearLayout>(R.id.favoritesContainer)
        container.removeAllViews()

        if (favorites.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No favorite technicians yet. Rate someone 5 stars to see them here!"
                android.view.Gravity.CENTER
                setPadding(32, 64, 32, 64)
                setTextColor(android.graphics.Color.GRAY)
            }
            container.addView(emptyText)
            return
        }

        val inflater = android.view.LayoutInflater.from(this)
        for (tech in favorites) {
            val cardView = inflater.inflate(R.layout.item_favorite_technician, container, false)
            
            cardView.findViewById<TextView>(R.id.techName).text = tech.fullName
            cardView.findViewById<TextView>(R.id.techRole).text = tech.role
            cardView.findViewById<TextView>(R.id.ratingText).text = "${tech.rating} (New)"
            cardView.findViewById<TextView>(R.id.distanceText).text = tech.distance

            val techImage = cardView.findViewById<ImageView>(R.id.techImage)
            if (!tech.profilePicUrl.isNullOrEmpty()) {
                val cleanedPath = tech.profilePicUrl?.removePrefix("/")
                com.bumptech.glide.Glide.with(this)
                    .load("${com.simats.fixitnow.network.RetrofitClient.BASE_URL}$cleanedPath")
                    .placeholder(R.drawable.ic_person)
                    .into(techImage)
            }

            cardView.findViewById<android.view.View>(R.id.bookNowButton).setOnClickListener {
                val intent = android.content.Intent(this, BookingDetailsActivity::class.java)
                intent.putExtra("TECH_ID", tech.id)
                intent.putExtra("TECH_NAME", tech.fullName)
                intent.putExtra("TECH_EMAIL", tech.email)
                startActivity(intent)
            }

            container.addView(cardView)
        }
    }
}
