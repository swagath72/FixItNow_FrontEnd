package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.fixitnow.databinding.ActivityChatDetailBinding
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.ChatMessage
import com.simats.fixitnow.network.RetrofitClient
import com.simats.fixitnow.network.SendMessageRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class Message(val text: String, val time: String, val isSent: Boolean, val status: String = "sent")

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatDetailAdapter
    private var otherUserEmail: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val pollingRunnable = object : Runnable {
        override fun run() {
            fetchMessages()
            handler.postDelayed(this, 3000) // Poll every 3 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Push input bar above keyboard when it appears
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                maxOf(imeInsets.bottom, navigationBarInsets.bottom)
            )
            insets
        }

        otherUserEmail = intent.getStringExtra("OTHER_USER_EMAIL") ?: ""
        val otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "Chat"
        
        binding.chatName.text = otherUserName

        binding.backButton.setOnClickListener {
            finish()
        }

        adapter = ChatDetailAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        fetchMessages()
    }

    override fun onResume() {
        super.onResume()
        handler.post(pollingRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(pollingRunnable)
    }

    private fun fetchMessages() {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getMessages("Bearer $token", otherUserEmail).enqueue(object : Callback<com.simats.fixitnow.network.ChatResponse> {
            override fun onResponse(call: Call<com.simats.fixitnow.network.ChatResponse>, response: Response<com.simats.fixitnow.network.ChatResponse>) {
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val newChatMessages = chatResponse?.messages ?: emptyList()
                    val isActive = chatResponse?.isActive ?: true

                    updateChatInput(isActive)

                    val newMessages = newChatMessages.map { 
                        Message(it.message, it.timestamp ?: "", it.isSentByMe, it.status ?: "sent")
                    }
                    
                    if (newMessages.size != messages.size) {
                        messages.clear()
                        messages.addAll(newMessages)
                        adapter.notifyDataSetChanged()
                        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }

            override fun onFailure(call: Call<com.simats.fixitnow.network.ChatResponse>, t: Throwable) {
                // Silently fail for polling
            }
        })
    }

    private fun updateChatInput(isActive: Boolean) {
        binding.messageEditText.isEnabled = isActive
        binding.sendButton.isEnabled = isActive
        binding.attachmentButton.isEnabled = isActive
        
        if (!isActive) {
            binding.messageEditText.hint = "Chat is disabled for completed jobs"
            binding.messageInputLayout.alpha = 0.6f
        } else {
            binding.messageEditText.hint = "Type a message..."
            binding.messageInputLayout.alpha = 1.0f
        }
    }

    private fun sendMessage(text: String) {
        val sharedPref = getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""
        
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val request = SendMessageRequest(otherUserEmail, text)
        
        binding.messageEditText.setText("")

        apiService.sendMessage("Bearer $token", request).enqueue(object : Callback<ChatMessage> {
            override fun onResponse(call: Call<ChatMessage>, response: Response<ChatMessage>) {
                if (response.isSuccessful) {
                    fetchMessages()
                } else {
                    Toast.makeText(this@ChatDetailActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ChatMessage>, t: Throwable) {
                Toast.makeText(this@ChatDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
