package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.BookingResponse

class BookingPagerAdapter(
    private val bookings: List<BookingResponse>,
    private val onItemClick: (BookingResponse) -> Unit
) : RecyclerView.Adapter<BookingPagerAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceName: TextView = view.findViewById(R.id.activeServiceName)
        val techName: TextView = view.findViewById(R.id.activeTechName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_active_booking_card, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.serviceName.text = "Service" // In a real app, you'd save the service type (e.g., Electrician) in the DB
        holder.techName.text = booking.technicianName ?: "Technician"
        
        holder.itemView.setOnClickListener {
            onItemClick(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size
}
