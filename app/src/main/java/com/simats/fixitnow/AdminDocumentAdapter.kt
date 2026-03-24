package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.TechnicianDocument

class AdminDocumentAdapter(
    private var documents: List<TechnicianDocument>
) : RecyclerView.Adapter<AdminDocumentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val docTypeTitle: TextView = view.findViewById(R.id.docTypeTitle)
        val docImageView: ImageView = view.findViewById(R.id.docImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_document, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = documents[position]
        holder.docTypeTitle.text = doc.doc_type

        val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
        val fileUrl = if (doc.file_url.startsWith("/")) doc.file_url else "/${doc.file_url}"
        val fullUrl = "$baseUrl$fileUrl"

        Glide.with(holder.itemView.context)
            .load(fullUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(R.drawable.baseline_image_not_supported_24)
            .into(holder.docImageView)

        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(holder.itemView.context, FullScreenImageActivity::class.java)
            intent.putExtra("IMAGE_URL", fullUrl)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = documents.size

    fun updateList(newList: List<TechnicianDocument>) {
        documents = newList
        notifyDataSetChanged()
    }
}
