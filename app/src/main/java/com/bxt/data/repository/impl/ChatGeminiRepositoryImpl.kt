package com.bxt.data.repository.impl

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.PromptRequest
import com.bxt.data.api.dto.response.ChatResponse
import com.bxt.data.repository.ChatGeminiRepository
import javax.inject.Inject


class ChatGeminiRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ChatGeminiRepository {

    override suspend fun sendMessageToGemini(prompt: String): Result<ChatResponse> {
        return try {
            val request = PromptRequest(prompt = prompt)
            val response = apiService.chatWithGemini(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}