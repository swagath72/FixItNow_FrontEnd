package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.PendingTechnician

class AdminTechnicianAdapter(
    private var technicians: List<PendingTechnician>,
    private val onItemClick: (PendingTechnician) -> Unit
) : RecyclerView.Adapter<AdminTechnicianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.techNameText)
        val emailText: TextView = view.findViewById(R.id.techEmailText)
        val skillsText: TextView = view.findViewById(R.id.techSkillsText)
        val profileImage: ImageView = view.findViewById(R.id.techProfileImage)
        val statusText: TextView = view.findViewById(R.id.techStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_technician, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tech = technicians[position]
        holder.nameText.text = tech.full_name
        holder.emailText.text = tech.email
        holder.skillsText.text = tech.skills ?: "No skills listed"
        holder.statusText.text = tech.verification_status.replace("_", " ").uppercase()

        if (!tech.profile_pic_url.isNullOrEmpty()) {
            val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
            Glide.with(holder.itemView.context)
                .load("$baseUrl${tech.profile_pic_url}")
                .circleCrop()
                .placeholder(R.drawable.profile_placeholder)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick(tech)
        }
    }

    override fun getItemCount() = technicians.size

    fun updateList(newList: List<PendingTechnician>) {
        technicians = newList
        notifyDataSetChanged()
    }
}
