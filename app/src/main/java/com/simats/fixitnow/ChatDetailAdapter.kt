package com.simats.fixitnow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatDetailAdapter(private val messages: List<Message>) : RecyclerView.Adapter<ChatDetailAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageTextView)
        val timeText: TextView = itemView.findViewById(R.id.timeTextView)
        val statusIcon: android.widget.ImageView? = itemView.findViewById(R.id.statusIcon)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) R.layout.item_chat_sent else R.layout.item_chat_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text
        
        // Format the time safely
        holder.timeText.text = try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val formatter = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
            val date = parser.parse(message.time)
            if (date != null) formatter.format(date) else message.time
        } catch (e: Exception) {
            message.time
        }
        
        if (message.isSent && holder.statusIcon != null) {
            when (message.status) {
                "read" -> holder.statusIcon.setImageResource(R.drawable.ic_tick_double_blue)
                "delivered" -> holder.statusIcon.setImageResource(R.drawable.ic_tick_double)
                else -> holder.statusIcon.setImageResource(R.drawable.ic_tick_single)
            }
        }
    }

    override fun getItemCount() = messages.size
}
