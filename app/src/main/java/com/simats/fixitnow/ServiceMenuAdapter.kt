package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class ServiceListItem {
    data class Header(val title: String, val serviceCount: Int, val icon: Int, var isExpanded: Boolean = false) : ServiceListItem()
    data class Service(val name: String, val price: String) : ServiceListItem()
}

class ServiceMenuAdapter(private val items: MutableList<ServiceListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SERVICE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ServiceListItem.Header -> TYPE_HEADER
            is ServiceListItem.Service -> TYPE_SERVICE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_category, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_list_item, parent, false)
            ServiceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val header = items[position] as ServiceListItem.Header
            holder.title.text = header.title
            holder.count.text = "${header.serviceCount} services"
            holder.icon.setImageResource(header.icon)
            holder.expandIcon.rotation = if (header.isExpanded) 180f else 0f

            holder.itemView.setOnClickListener {
                header.isExpanded = !header.isExpanded
                notifyItemChanged(position)
            }
        } else if (holder is ServiceViewHolder) {
            val service = items[position] as ServiceListItem.Service
            holder.name.text = service.name
            holder.price.text = service.price
        }
    }

    override fun getItemCount() = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.category_title)
        val count: TextView = view.findViewById(R.id.category_service_count)
        val icon: ImageView = view.findViewById(R.id.category_icon)
        val expandIcon: ImageView = view.findViewById(R.id.expand_icon)
    }

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.service_name)
        val price: TextView = view.findViewById(R.id.service_price)
    }
}
