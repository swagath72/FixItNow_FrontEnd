package com.simats.fixitnow

import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.ChatListItem

class ChatAdapter(private var chats: List<ChatListItem>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<ChatListItem>) {
        chats = newChats
        notifyDataSetChanged()
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.chatName)
        private val roleText: TextView = view.findViewById(R.id.chatRole)
        private val previewText: TextView = view.findViewById(R.id.chatPreview)
        private val timeText: TextView = view.findViewById(R.id.chatTime)
        private val unreadCountText: TextView = view.findViewById(R.id.unreadCount)
        private val avatarImage: ImageView = view.findViewById(R.id.chatAvatar)
        private val newMessageDot: View = view.findViewById(R.id.newMessageDot)

        fun bind(chat: ChatListItem) {
            nameText.text = chat.name
            roleText.text = chat.role
            previewText.text = chat.lastMessage

            timeText.text = try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val formatter = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                val date = parser.parse(chat.time)
                if (date != null) formatter.format(date) else chat.time
            } catch (e: Exception) {
                chat.time
            }

            if (chat.unreadCount > 0) {
                // Show green dot on avatar
                newMessageDot.visibility = View.VISIBLE
                // Show numeric badge
                unreadCountText.visibility = View.VISIBLE
                unreadCountText.text = chat.unreadCount.toString()
                // Bold preview text to indicate unread
                previewText.setTypeface(null, Typeface.BOLD)
                previewText.setTextColor(android.graphics.Color.parseColor("#212121"))
                // Highlight time in green
                timeText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                // Hide green dot
                newMessageDot.visibility = View.GONE
                // Hide numeric badge
                unreadCountText.visibility = View.GONE
                // Normal preview text
                previewText.setTypeface(null, Typeface.NORMAL)
                previewText.setTextColor(android.graphics.Color.parseColor("#757575"))
                // Normal time color
                timeText.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, ChatDetailActivity::class.java).apply {
                    putExtra("OTHER_USER_EMAIL", chat.email)
                    putExtra("OTHER_USER_NAME", chat.name)
                }
                itemView.context.startActivity(intent)
            }
        }
    }
}
