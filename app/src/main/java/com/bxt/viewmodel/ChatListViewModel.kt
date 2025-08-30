package com.bxt.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.ApiService
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ChatRepository
import com.bxt.ui.state.ChatListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatThreadUi(
    val otherUserId: String,
    val displayName: String,
    val avatarUrl: String?,
    val lastMessage: String?,
    val timestamp: Long
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val dataStore: DataStoreManager,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadChatList(isRefreshAction = false)
    }

    fun refresh() {
        loadChatList(isRefreshAction = true)
    }

    private fun loadChatList(isRefreshAction: Boolean) {
        viewModelScope.launch {
            if (isRefreshAction) {
                _isRefreshing.value = true
            } else {
                _uiState.value = ChatListUiState.Loading
            }

            try {
                val myId = dataStore.userId.first()?.toString()
                if (myId == null) {
                    _uiState.value = ChatListUiState.Error("Không tìm thấy userId local")
                    if (isRefreshAction) _isRefreshing.value = false
                    return@launch
                }

                chatRepository.getChatList(myId) { chats ->
                    viewModelScope.launch {
                        try {
                            // Lọc bỏ cuộc trò chuyện với chính mình
                            val filteredChats = chats.filterKeys { otherId -> otherId != myId }

                            val baseThreads = filteredChats.entries.map { (otherId, info) ->
                                val last = info["lastMessage"] as? String
                                val ts = (info["timestamp"] as? Long) ?: 0L
                                ChatThreadUi(otherId, "...", null, last, ts)
                            }

                            val profileJobs = baseThreads.map { thread ->
                                async {
                                    try {
                                        val userId = thread.otherUserId.toLongOrNull()
                                        if (userId != null) {
                                            val userResponse = apiService.getUserNameById(userId)
                                            thread.copy(
                                                displayName = userResponse.fullName?.takeIf { it.isNotBlank() } ?: "User #${thread.otherUserId}",
                                                avatarUrl = userResponse.avatarUrl
                                            )
                                        } else {
                                            thread.copy(displayName = "User #${thread.otherUserId}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ChatListViewModel", "Lỗi tải profile: ${thread.otherUserId}", e)
                                        thread.copy(displayName = "User #${thread.otherUserId}")
                                    }
                                }
                            }

                            val threadsWithProfiles = profileJobs.awaitAll()
                            _uiState.value = ChatListUiState.Success(threadsWithProfiles.sortedByDescending { it.timestamp })

                        } catch (e: Exception) {
                            _uiState.value = ChatListUiState.Error("Không thể xử lý danh sách chat: ${e.message}")
                        } finally {
                            if (isRefreshAction) {
                                _isRefreshing.value = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatListUiState.Error("Không thể tải danh sách chat: ${e.message}")
                if (isRefreshAction) {
                    _isRefreshing.value = false
                }
            }
        }
    }
}