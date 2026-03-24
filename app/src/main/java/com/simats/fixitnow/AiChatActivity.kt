package com.simats.fixitnow

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.fixitnow.databinding.ActivityChatDetailBinding
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.AiChatRequest
import com.simats.fixitnow.network.AiChatResponse
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatDetailAdapter

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

        setupToolbar()
        setupRecyclerView()
        setupInput()

        // Welcome message
        addMessage("Hello! I'm your FIXITNOW AI assistant. How can I help you with your home services today?", false)
    }

    private fun setupToolbar() {
        binding.chatName.text = "AI Support"
        binding.chatRole.text = "AI Assistant"
        binding.chatRole.visibility = View.VISIBLE
        binding.chatAvatar.setImageResource(R.drawable.ic_ai_assistant_avatar)
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ChatDetailAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = adapter
    }

    private fun setupInput() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageToAi(text)
            }
        }
    }

    private fun sendMessageToAi(userText: String) {
        // Add user message to UI
        addMessage(userText, true)
        binding.messageEditText.setText("")
        
        // Show "Typing..." state
        binding.typingIndicator.visibility = View.VISIBLE
        binding.sendButton.isEnabled = false
        
        // Handle common queries client-side for immediate response
        val clientSideResponse = handleCommonQueries(userText)
        if (clientSideResponse != null) {
            // Use a slight delay to feel natural
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                binding.typingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
                addMessage(clientSideResponse, false)
            }, 1000)
            return
        }

        val request = AiChatRequest(message = userText)
        val apiService = RetrofitClient.createService(ApiService::class.java)

        apiService.sendAiChat(request).enqueue(object : Callback<AiChatResponse> {
            override fun onResponse(call: Call<AiChatResponse>, response: Response<AiChatResponse>) {
                binding.typingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
                if (response.isSuccessful) {
                    val aiResponse = response.body()?.response
                    Log.d("AiChatActivity", "AI Response: $aiResponse")
                    if (aiResponse != null) {
                        addMessage(aiResponse, false)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AiChatActivity", "AI Error: ${response.code()} - $errorBody")
                    Toast.makeText(this@AiChatActivity, "AI Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AiChatResponse>, t: Throwable) {
                binding.typingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
                Log.e("AiChatActivity", "Network Error: ${t.message}", t)
                Toast.makeText(this@AiChatActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleCommonQueries(text: String): String? {
        val query = text.lowercase()
        return when {
            // More specific "how to book" matching
            (query.contains("how") && query.contains("book")) || query == "book" || query == "booking" -> 
                "To book a service, go to the Home screen and select your required category (Electrical or Plumbing). Choose a technician and click 'Book Service'."
            
            // Tracking queries
            query.contains("track") || query.contains("where is my technician") -> 
                "You can track your active bookings from the Home screen pager or by tapping on the booking Card."
            
            // Payment specific (but let AI handle "cost" if it's detailed like "price of plumbing")
            query == "pay" || query == "payment" || (query.contains("how") && query.contains("pay")) -> 
                "Payments can be made after the service is completed. You'll see a 'Make Payment' button on the tracking page."
            
            // Greetings
            query.contains("hello") || query.contains("hi") || query.contains("hey") -> 
                "Hello! I'm your FIXITNOW AI assistant. How can I help you today?"
            
            else -> null
        }
    }

    private fun addMessage(text: String, isSent: Boolean) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        messages.add(Message(text, currentTime, isSent, "sent"))
        adapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
    }
}
