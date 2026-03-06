package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.databinding.ItemSavedAddressBinding

data class SavedAddress(
    val name: String,
    val details: String,
    val shortAddress: String,
    val isDefault: Boolean,
    val iconRes: Int
)

class SavedAddressAdapter(private val addresses: List<SavedAddress>) :
    RecyclerView.Adapter<SavedAddressAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSavedAddressBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val address = addresses[position]
        holder.binding.addressTitle.text = address.name
        holder.binding.addressDetails.text = address.details
        holder.binding.addressIcon.setImageResource(address.iconRes)
        holder.binding.defaultChip.visibility = if (address.isDefault) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = addresses.size
}
