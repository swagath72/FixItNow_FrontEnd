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
import com.simats.fixitnow.network.GroqApiService
import com.simats.fixitnow.network.GroqChatRequest
import com.simats.fixitnow.network.GroqChatResponse
import com.simats.fixitnow.network.GroqMessage
import com.simats.fixitnow.network.GroqRetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatDetailAdapter
    //private val apiKey = ""
    
    // System prompt to give context to the AI
    private val systemPrompt = GroqMessage(
        role = "system",
        content = "You are FIXITNOW AI Assistant. You help users with home service related questions like plumbing, electrical work, and app navigation. Be helpful, concise, and professional."
    )

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
        binding.chatAvatar.setImageResource(R.drawable.ic_ai_assistant_avatar)
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ChatDetailAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
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
        
        val groqMessages = mutableListOf<GroqMessage>()
        groqMessages.add(systemPrompt)
        
        // Convert local messages to Groq model (limit to last 5 for context to keep within limits)
        messages.takeLast(5).forEach { 
            groqMessages.add(GroqMessage(role = if (it.isSent) "user" else "assistant", content = it.text))
        }

        // Using llama-3.1-8b-instant which is a recommended replacement on Groq
        val request = GroqChatRequest(model = "llama-3.1-8b-instant", messages = groqMessages)
        val apiService = GroqRetrofitClient.createService(GroqApiService::class.java)

        apiService.getChatCompletion(apiKey, request).enqueue(object : Callback<GroqChatResponse> {
            override fun onResponse(call: Call<GroqChatResponse>, response: Response<GroqChatResponse>) {
                binding.typingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
                if (response.isSuccessful) {
                    val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content
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

            override fun onFailure(call: Call<GroqChatResponse>, t: Throwable) {
                binding.typingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
                Log.e("AiChatActivity", "Network Error: ${t.message}", t)
                Toast.makeText(this@AiChatActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMessage(text: String, isSent: Boolean) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        messages.add(Message(text, currentTime, isSent, "sent"))
        adapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }
}
