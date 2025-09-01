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
import kotlinx.coroutines.delay
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val otherUserId: String = savedStateHandle["otherUserId"]!!
    private var lastSentAttachableJson: String? = null

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    private val _recipientName = MutableStateFlow<String?>(null)
    val recipientName: StateFlow<String?> = _recipientName.asStateFlow()
    private val _messages = mutableStateListOf<Map<String, Any?>>()
    val messages: List<Map<String, Any?>> = _messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var messageListener: ChildEventListener? = null
    private var listeningForUserId: String? = null
    private var hasLoadedInitialHistory = false

    private var attachableMessageTimestamp: Long? = null

    init {
        fetchRecipientName()
        observeLocalSession()
    }

    private fun observeLocalSession() {
        viewModelScope.launch {
            combine(dataStore.accessToken, dataStore.userId) { token, uid ->
                Pair(!token.isNullOrBlank(), uid?.toString())
            }.collect { (loggedIn, uidStr) ->
                _isUserLoggedIn.value = loggedIn
                _currentUserId.value = uidStr

                if (uidStr != null && uidStr == otherUserId) {
                    _errorMessage.value = "Bạn không thể tự trò chuyện với chính mình."
                    detachListener()
                    return@collect
                }

                if (!loggedIn || uidStr == null) {
                    _errorMessage.value = "Vui lòng đăng nhập để sử dụng tính năng chat"
                    detachListener()
                } else {
                    _errorMessage.value = null
                    if (!hasLoadedInitialHistory) {
                        loadHistoryAndListenForNewMessages()
                    }
                }
            }
        }
    }

    private fun loadHistoryAndListenForNewMessages() {
        val myId = _currentUserId.value ?: return
        hasLoadedInitialHistory = true

        sendInitialAttachableMessageIfNeeded()

        viewModelScope.launch {
            delay(100)
            chatRepository.getInitialMessages(myId, otherUserId) { initialMessages ->
                _messages.clear()
                _messages.addAll(initialMessages)
                listenForNewMessages()
            }
        }
    }

    private fun sendInitialAttachableMessageIfNeeded() {
        val myId = _currentUserId.value ?: return
        if (myId == otherUserId) return

        val currentAttachableJson: String? = savedStateHandle["attachableJson"]

        if (currentAttachableJson != null && currentAttachableJson != lastSentAttachableJson) {
            viewModelScope.launch {
                try {
                    lastSentAttachableJson = currentAttachableJson

                    // Tạo timestamp và lưu lại
                    val timestamp = System.currentTimeMillis()
                    attachableMessageTimestamp = timestamp

                    val messageMap = hashMapOf<String, Any?>(
                        "senderId" to myId,
                        "text" to "",
                        "timestamp" to timestamp
                    )
                    val type = object : TypeToken<Map<String, Any?>>() {}.type
                    messageMap["attachable"] =
                        Gson().fromJson<Map<String, Any?>>(currentAttachableJson, type)

                    chatRepository.sendMessage(myId, otherUserId, messageMap)
                } catch (e: Exception) {
                    lastSentAttachableJson = null
                    attachableMessageTimestamp = null
                    _errorMessage.value = "Không thể gửi thông tin sản phẩm."
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val myId = _currentUserId.value
        if (myId == otherUserId) {
            _errorMessage.value = "Bạn không thể tự trò chuyện với chính mình."
            return
        }
        if (myId == null || !_isUserLoggedIn.value) {
            _errorMessage.value = "Bạn cần đăng nhập để gửi tin nhắn"
            return
        }
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val messageMap = hashMapOf<String, Any?>(
                    "senderId" to myId,
                    "text" to text.trim(),
                    "timestamp" to System.currentTimeMillis()
                )
                chatRepository.sendMessage(myId, otherUserId, messageMap)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Không thể gửi tin nhắn: ${e.message}"
            }
        }
    }

    private fun fetchRecipientName() {
        val idLong = otherUserId.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val response = userApi.getUserNameById(idLong)
                _recipientName.value =
                    response.fullName?.takeIf { it.isNotBlank() } ?: "Người dùng #${otherUserId}"
            } catch (_: Exception) {
                _recipientName.value = "Người dùng #${otherUserId}"
            }
        }
    }

    private fun listenForNewMessages() {
        val myId = _currentUserId.value ?: return
        if (messageListener != null && listeningForUserId == myId) return

        listeningForUserId = myId
        try {
            messageListener = chatRepository.listenForMessages(myId, otherUserId) { newMessage ->
                // Kiểm tra để tránh tin nhắn bị trùng lặp
                val messageTimestamp = newMessage["timestamp"] as? Long
                val isDuplicate = _messages.any { it["timestamp"] == messageTimestamp }

                if (!isDuplicate) {
                    _messages.add(newMessage)
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Không thể tải tin nhắn: ${e.message}"
        }
    }

    private fun detachListener() {
        listeningForUserId?.let {
            messageListener?.let { listener ->
                chatRepository.removeMessagesListener(it, otherUserId, listener)
            }
        }
        messageListener = null
        listeningForUserId = null
        _messages.clear()
        hasLoadedInitialHistory = false
    }

    override fun onCleared() {
        super.onCleared()
        detachListener()
    }
}