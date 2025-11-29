package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.PromptRequest
import com.bxt.data.api.dto.response.ChatResponse
import javax.inject.Inject

interface ChatGeminiRepository{
    suspend fun sendMessageToGemini(prompt: String): Result<ChatResponse>
}