package com.bxt.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.ApiService
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ChatRepository
import com.google.firebase.database.ChildEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val dataStore: DataStoreManager,
    private val userApi: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val otherUserId: String = savedStateHandle["otherUserId"]!!
    private val initialAttachableJson: String? = savedStateHandle["attachableJson"]

    // login local
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // tên người nhận (từ API)
    private val _recipientName = MutableStateFlow<String?>(null)
    val recipientName: StateFlow<String?> = _recipientName.asStateFlow()

    // UI state
    private val _messages = mutableStateListOf<Map<String, Any?>>()
    val messages: List<Map<String, Any?>> = _messages

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var messageListener: ChildEventListener? = null
    private var listeningForUserId: String? = null

    init {
        observeLocalSession()
        fetchRecipientName()
    }

    private fun observeLocalSession() {
        viewModelScope.launch {
            combine(dataStore.accessToken, dataStore.userId) { token, uid ->
                val loggedIn = !token.isNullOrBlank()
                val uidStr = uid?.toString()
                Pair(loggedIn, uidStr)
            }.collect { (loggedIn, uidStr) ->
                _isUserLoggedIn.value = loggedIn
                _currentUserId.value = uidStr

                if (!loggedIn || uidStr == null) {
                    _errorMessage.value = "Vui lòng đăng nhập để sử dụng tính năng chat"
                    detachListener()
                } else {
                    _errorMessage.value = null
                    if (listeningForUserId != uidStr) {
                        detachListener()
                        listenForMessages()
                    }
                }
            }
        }
    }

    private fun fetchRecipientName() {
        val idLong = otherUserId.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                _recipientName.value = userApi.getUserNameById(idLong)
            } catch (_: Exception) {
                // giữ null -> UI fallback "Chat với: {id}"
            }
        }
    }

    fun listenForMessages() {
        val myId = _currentUserId.value ?: return
        if (!_isUserLoggedIn.value) return
        if (messageListener != null && listeningForUserId == myId) return

        listeningForUserId = myId
        try {
            messageListener = chatRepository.listenForMessages(myId, otherUserId) { newMessage ->
                _messages.add(newMessage)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Không thể tải tin nhắn: ${e.message}"
        }
    }

    fun sendMessage(text: String) {
        if (!_isUserLoggedIn.value) {
            _errorMessage.value = "Bạn cần đăng nhập để gửi tin nhắn"
            return
        }
        val myId = _currentUserId.value ?: run {
            _errorMessage.value = "Không xác định được người dùng hiện tại"
            return
        }
        if (text.isBlank() && initialAttachableJson == null) return

        viewModelScope.launch {
            try {
                val hasAttachable = _messages.any { it.containsKey("attachable") }
                val messageMap = hashMapOf<String, Any?>(
                    "senderId" to myId,
                    "text" to text.trim(),
                    "timestamp" to System.currentTimeMillis()
                )
                if (!hasAttachable && initialAttachableJson != null) {
                    val type = object : TypeToken<Map<String, Any?>>() {}.type
                    messageMap["attachable"] = Gson().fromJson<Map<String, Any?>>(initialAttachableJson, type)
                }
                chatRepository.sendMessage(myId, otherUserId, messageMap)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Không thể gửi tin nhắn: ${e.message}"
            }
        }
    }

    private fun detachListener() {
        val myId = listeningForUserId ?: return
        messageListener?.let { chatRepository.removeMessagesListener(myId, otherUserId, it) }
        messageListener = null
        listeningForUserId = null
        _messages.clear()
    }

    override fun onCleared() {
        super.onCleared()
        detachListener()
    }
}
