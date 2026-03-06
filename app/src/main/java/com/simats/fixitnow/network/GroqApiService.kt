package com.simats.fixitnow.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatRequest(
    val model: String = "mixtral-8x7b-32768",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqMessage,
    @SerializedName("finish_reason") val finishReason: String
)

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqChatRequest
    ): Call<GroqChatResponse>
}
