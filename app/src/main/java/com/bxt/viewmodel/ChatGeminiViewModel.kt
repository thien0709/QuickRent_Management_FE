package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.repository.ChatGeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Participant {
    USER, BOT
}

data class Message(
    val text: String,
    val participant: Participant,
    val isPending: Boolean = false
)

data class ChatGeminiUiState(
    val messages: List<Message> = emptyList(),
    val isBotTyping: Boolean = false
)

@HiltViewModel
class ChatGeminiViewModel @Inject constructor(
    private val repository: ChatGeminiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatGeminiUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank() || _uiState.value.isBotTyping) return

        val userMessage = Message(text = userPrompt, participant = Participant.USER)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBotTyping = true)

            val result = repository.sendMessageToGemini(userPrompt)

            result.onSuccess { response ->
                val botMessage = Message(text = response.message, participant = Participant.BOT)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + botMessage,
                    isBotTyping = false
                )
            }.onFailure { exception ->
                val errorMessage = Message(text = "Lỗi: ${exception.message}", participant = Participant.BOT)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + errorMessage,
                    isBotTyping = false
                )
            }
        }
    }
}