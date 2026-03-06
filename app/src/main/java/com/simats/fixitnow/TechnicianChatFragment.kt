package com.simats.fixitnow

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.fixitnow.network.ApiService
import com.simats.fixitnow.network.ChatListItem
import com.simats.fixitnow.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TechnicianChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val pollingRunnable = object : Runnable {
        override fun run() {
            fetchChatList(silent = true)
            handler.postDelayed(this, 5000) // Poll every 5 seconds
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_technician_chat, container, false)

        recyclerView = view.findViewById(R.id.chatsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatAdapter(emptyList())
        recyclerView.adapter = adapter

        fetchChatList(silent = false)

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(pollingRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(pollingRunnable)
    }

    private fun fetchChatList(silent: Boolean = false) {
        val sharedPref = requireActivity().getSharedPreferences("FIXITNOW_PREFS", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", "") ?: ""

        if (token.isEmpty()) return

        if (!silent) progressBar.visibility = View.VISIBLE
        val apiService = RetrofitClient.createService(ApiService::class.java)
        apiService.getChatList("Bearer $token").enqueue(object : Callback<List<ChatListItem>> {
            override fun onResponse(call: Call<List<ChatListItem>>, response: Response<List<ChatListItem>>) {
                if (!isAdded) return
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val chats = response.body() ?: emptyList()
                    if (chats.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                    } else {
                        emptyStateText.visibility = View.GONE
                        adapter.updateChats(chats)
                    }
                }
            }

            override fun onFailure(call: Call<List<ChatListItem>>, t: Throwable) {
                if (!isAdded) return
                progressBar.visibility = View.GONE
                if (!silent) showDummyData()
            }
        })
    }

    private fun showDummyData() {
        val dummyChats = listOf(
            ChatListItem("Customer Support", "support@fixitnow.com", "How can I help you today?", "Now", 0, "AI Assistant"),
            ChatListItem("Sarah Johnson", "sarah@example.com", "Thank you for the great service!", "11:45 AM", 1, "Customer"),
            ChatListItem("Michael Chen", "michael@example.com", "Can you come tomorrow at 2 PM?", "Yesterday", 0, "Customer")
        )
        emptyStateText.visibility = View.GONE
        adapter.updateChats(dummyChats)
    }
}
