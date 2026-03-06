package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.SubmitRatingRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicator: TabLayout
    private lateinit var recentBookingsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.activeBookingsViewPager)
        indicator = view.findViewById(R.id.bookingIndicator)
        recentBookingsContainer = view.findViewById(R.id.recentBookingsContainer)

        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)

        val greetingText = view.findViewById<TextView>(R.id.greetingText)
        val userName = sharedPref.getString("USER_NAME", "User")
        greetingText.text = "Hello, $userName! 👋"

        val locationText = view.findViewById<TextView>(R.id.locationText)
        val savedLocation = sharedPref.getString("USER_DISPLAY_LOCATION", 
                           sharedPref.getString("USER_LOCATION", "Set your location"))
        locationText.text = savedLocation

        fetchActiveBookings()
        fetchRecentBookings()

        val openAddressSelection = View.OnClickListener {
            startActivity(Intent(requireContext(), AddressSelectionActivity::class.java))
        }
        
        view.findViewById<View>(R.id.locationIcon).setOnClickListener(openAddressSelection)
        locationText.setOnClickListener(openAddressSelection)

        // Set images for Electrician Card
        view.findViewById<View>(R.id.serviceElectrician)?.let { card ->
            card.findViewById<ImageView>(R.id.serviceImage)?.setImageResource(R.drawable.electrician_image)
            card.findViewById<ImageView>(R.id.serviceIcon)?.setImageResource(R.drawable.ic_flash)
            card.findViewById<TextView>(R.id.serviceName)?.text = "Electrical"
            card.setOnClickListener {
                startActivity(Intent(requireContext(), ElectricianMenuActivity::class.java))
            }
        }

        // Set images for Plumber Card
        view.findViewById<View>(R.id.servicePlumber)?.let { card ->
            card.findViewById<ImageView>(R.id.serviceImage)?.setImageResource(R.drawable.plumbing_image)
            card.findViewById<ImageView>(R.id.serviceIcon)?.setImageResource(R.drawable.ic_water_drop)
            card.findViewById<TextView>(R.id.serviceName)?.text = "Plumbing"
            card.setOnClickListener {
                startActivity(Intent(requireContext(), PlumberMenuActivity::class.java))
            }
        }
    }

    private fun fetchActiveBookings() {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getActiveBookings("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val bookings = response.body() ?: emptyList()
                    Log.d("FixItNow", "Active Bookings Found: ${bookings.size}")
                    bookings.forEach { Log.d("FixItNow", "Active Booking ID: ${it.id}, Status: ${it.status}") }
                    setupViewPager(bookings)
                    
                    // Check for bookings that need rating (status is "Completed" AND rating is not yet submitted)
                    bookings.find { it.status == "Completed" && (it.ratingValue == null || it.ratingValue <= 0) }?.let { completedBooking ->
                        Log.d("FixItNow", "Unrated completed booking Found in active! Showing dialog for ID: ${completedBooking.id}")
                        showRatingDialog(completedBooking)
                    }
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "Active Bookings Error: ${t.message}")
            }
        })
    }

    private fun fetchRecentBookings() {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getRecentBookings("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val bookings = response.body() ?: emptyList()
                    Log.d("FixItNow", "Recent Bookings Found: ${bookings.size}")
                    bookings.forEach { Log.d("FixItNow", "Recent Booking ID: ${it.id}, Status: ${it.status}") }
                    displayRecentBookings(bookings)
                    
                    // Also check recent bookings for jobs that need rating
                    if (isAdded) {
                        bookings.find { it.status == "Completed" && (it.ratingValue == null || it.ratingValue <= 0) }?.let { completedBooking ->
                            Log.d("FixItNow", "Unrated completed booking Found in recent! Showing dialog for ID: ${completedBooking.id}")
                            showRatingDialog(completedBooking)
                        }
                    }
                } else {
                    Log.e("FixItNow", "Recent Bookings failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "Recent Bookings Error: ${t.message}")
            }
        })
    }

    private fun displayRecentBookings(bookings: List<BookingResponse>) {
        if (!isAdded) return
        recentBookingsContainer.removeAllViews()
        
        val titleView = view?.findViewById<View>(R.id.recentBookingsTitle)
        if (bookings.isEmpty()) {
            titleView?.visibility = View.GONE
            return
        }
        titleView?.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(requireContext())
        for (booking in bookings) {
            val cardView = inflater.inflate(R.layout.booking_card, recentBookingsContainer, false)
            cardView.findViewById<TextView>(R.id.bookingTitle).text = booking.description ?: "Service" 
            cardView.findViewById<TextView>(R.id.bookingDate).text = booking.date ?: "Recently"
            cardView.findViewById<TextView>(R.id.bookingStatus).text = booking.status ?: "Completed"
            recentBookingsContainer.addView(cardView)
        }
    }

    private fun setupViewPager(bookings: List<BookingResponse>) {
        if (bookings.isEmpty()) {
            viewPager.visibility = View.GONE
            indicator.visibility = View.GONE
            return
        }

        viewPager.visibility = View.VISIBLE
        indicator.visibility = View.VISIBLE

        viewPager.adapter = BookingPagerAdapter(bookings) { booking ->
            val intent = Intent(requireContext(), TrackTechnicianActivity::class.java)
            intent.putExtra("BOOKING_ID", booking.id ?: -1)
            intent.putExtra("TECH_NAME", booking.technicianName)
            intent.putExtra("TECH_EMAIL", booking.technicianEmail)
            startActivity(intent)
        }
        
        TabLayoutMediator(indicator, viewPager) { _, _ -> }.attach()
    }

    private fun showRatingDialog(booking: BookingResponse) {
        if (!isAdded) return
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_rating, null)
        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.dialogRatingBar)
        val commentEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.ratingComment)
        val submitButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.submitRatingButton)
        val skipText = dialogView.findViewById<android.widget.TextView>(R.id.skipRating)

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            if (rating == 0f) {
                Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = commentEditText.text.toString().trim()
            submitRating(booking.id ?: -1, rating, comment, dialog)
        }

        skipText.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun submitRating(bookingId: Int, rating: Float, comment: String, dialog: androidx.appcompat.app.AlertDialog) {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = com.simats.fixitnow.network.SubmitRatingRequest(bookingId, rating, comment)
        
        apiService.submitRating("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchActiveBookings() // Refresh to remove the completed job from list
                } else {
                    Toast.makeText(requireContext(), "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val locationText = view?.findViewById<TextView>(R.id.locationText)
        val savedLocation = sharedPref.getString("USER_DISPLAY_LOCATION", 
                           sharedPref.getString("USER_LOCATION", "Set your location"))
        locationText?.text = savedLocation
    }
}
