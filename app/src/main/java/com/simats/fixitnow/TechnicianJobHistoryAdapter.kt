package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.BookingResponse

class TechnicianJobHistoryAdapter(private var jobs: List<BookingResponse>) : RecyclerView.Adapter<TechnicianJobHistoryAdapter.JobViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_technician_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.bind(job)
    }

    override fun getItemCount(): Int = jobs.size

    fun updateJobs(newJobs: List<BookingResponse>) {
        jobs = newJobs
        notifyDataSetChanged()
    }

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val customerName: TextView = itemView.findViewById(R.id.customerName)
        private val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        private val jobDate: TextView = itemView.findViewById(R.id.jobDate)
        private val jobAmount: TextView = itemView.findViewById(R.id.jobAmount)
        private val jobRating: TextView = itemView.findViewById(R.id.jobRating)

        fun bind(job: BookingResponse) {
            // Use customerEmail if customerName is null, or "Valued Customer" as fallback
            customerName.text = job.customerName ?: job.customerEmail ?: "Valued Customer"
            
            // Use description or default if serviceName is null
            serviceName.text = job.serviceName ?: job.description ?: "Home Maintenance"
            
            jobDate.text = job.date ?: "Recently"
            
            // Handle different cost formats
            val costValue = job.cost ?: "0"
            jobAmount.text = if (costValue.contains("₹")) costValue else "₹$costValue"
            
            jobRating.text = "5.0"
        }
    }
}
