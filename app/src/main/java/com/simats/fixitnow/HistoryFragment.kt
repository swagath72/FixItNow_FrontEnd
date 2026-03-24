package com.simats.fixitnow

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.BookingResponse
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment() {

    private lateinit var completedList: LinearLayout
    private lateinit var cancelledList: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<View>(R.id.backButton)
        val tabCompleted = view.findViewById<TextView>(R.id.tabCompleted)
        val tabCancelled = view.findViewById<TextView>(R.id.tabCancelled)
        completedList = view.findViewById(R.id.completedList)
        cancelledList = view.findViewById(R.id.cancelledList)

        fetchHistory()

        tabCompleted.setOnClickListener {
            tabCompleted.setBackgroundResource(R.drawable.history_toggle_selected)
            tabCompleted.setTextColor(Color.parseColor("#3C61FF"))
            tabCancelled.setBackground(null)
            tabCancelled.setTextColor(Color.WHITE)
            completedList.visibility = View.VISIBLE
            cancelledList.visibility = View.GONE
        }

        tabCancelled.setOnClickListener {
            tabCancelled.setBackgroundResource(R.drawable.history_toggle_selected)
            tabCancelled.setTextColor(Color.parseColor("#3C61FF"))
            tabCompleted.setBackground(null)
            tabCompleted.setTextColor(Color.WHITE)
            completedList.visibility = View.GONE
            cancelledList.visibility = View.VISIBLE
        }
    }

    private fun fetchHistory() {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        if (token.isEmpty()) return

        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getBookingHistory("Bearer $token").enqueue(object : Callback<List<BookingResponse>> {
            override fun onResponse(call: Call<List<BookingResponse>>, response: Response<List<BookingResponse>>) {
                if (response.isSuccessful) {
                    val bookings = response.body() ?: emptyList()
                    Log.d("FixItNow", "History Found: ${bookings.size}")
                    displayHistory(bookings)
                }
            }
            override fun onFailure(call: Call<List<BookingResponse>>, t: Throwable) {
                Log.e("FixItNow", "History Error: ${t.message}")
            }
        })
    }

    private fun displayHistory(bookings: List<BookingResponse>) {
        if (!isAdded) return
        completedList.removeAllViews()
        cancelledList.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        for (booking in bookings) {
            val statusStr = booking.status?.trim()?.lowercase() ?: ""
            val isCompleted = statusStr == "completed"
            val isCancelled = statusStr == "cancelled" || statusStr == "cancel" || statusStr == "rejected"

            if (!isCompleted && !isCancelled) continue // Only show completed and cancelled in History

            val layoutId = if (isCancelled) R.layout.item_history_cancelled else R.layout.item_history_completed
            val parent = if (isCancelled) cancelledList else completedList
            
            val cardView = inflater.inflate(layoutId, parent, false)
            
            // Show the specific Service Name (e.g. Switch Repair) instead of generic "Service"
            cardView.findViewById<TextView>(R.id.serviceTitle)?.text = booking.serviceName ?: booking.description ?: "Service"
            cardView.findViewById<TextView>(R.id.serviceDate)?.text = booking.date ?: ""
            cardView.findViewById<TextView>(R.id.technicianName)?.text = booking.technicianName ?: "Professional"
            
            // Set the actual cost from database
            val rawCost = booking.cost ?: "0"
            val displayCost = rawCost.replace("From ", "").replace("$", "").replace("₹", "").trim()
            cardView.findViewById<TextView>(R.id.servicePrice)?.text = "₹$displayCost"
            
            // Map the status text if it exists in the layout
            cardView.findViewById<TextView>(R.id.bookingStatus)?.let {
                var finalStatus = booking.status ?: "Completed"
                if (isCompleted && booking.paymentStatus == "Paid") {
                    finalStatus += " - Paid"
                } else if (isCompleted) {
                    finalStatus += " - Pending Payment"
                }
                it.text = finalStatus
                if (isCancelled) it.setTextColor(Color.RED) else it.setTextColor(Color.parseColor("#4CAF50"))
            }

            // Handle rating visibility and text
            val ratingLayout = cardView.findViewById<View>(R.id.ratingLayout)
            val ratingText = cardView.findViewById<TextView>(R.id.ratingText)
            val ratingCommentText = cardView.findViewById<TextView>(R.id.ratingComment)
            
            if (isCompleted && booking.ratingValue != null && booking.ratingValue > 0) {
                ratingLayout?.visibility = View.VISIBLE
                ratingText?.text = booking.ratingValue.toString()
                
                if (!booking.ratingComment.isNullOrEmpty()) {
                    ratingCommentText?.visibility = View.VISIBLE
                    ratingCommentText?.text = "\"${booking.ratingComment}\""
                } else {
                    ratingCommentText?.visibility = View.GONE
                }
            } else {
                ratingLayout?.visibility = View.GONE
                ratingCommentText?.visibility = View.GONE
            }

            cardView.findViewById<View>(R.id.bookAgainButton)?.setOnClickListener {
                val intent = Intent(requireContext(), BookingDetailsActivity::class.java)
                intent.putExtra("SERVICE_NAME", booking.serviceName ?: booking.description ?: "Service")
                intent.putExtra("SERVICE_PRICE", displayCost)
                startActivity(intent)
            }

            parent.addView(cardView)
        }
    }
}
