package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ChatRepository
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference

    private val _threads = MutableStateFlow<List<ChatThreadUi>>(emptyList())
    val threads: StateFlow<List<ChatThreadUi>> = _threads

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadChatList() {
        viewModelScope.launch {
            _loading.value = true
            val myId = dataStore.userId.first()?.toString()
            if (myId == null) {
                _error.value = "Không tìm thấy userId local"
                _loading.value = false
                return@launch
            }

            chatRepository.getChatList(myId) { chats ->
                // build list thô (chưa có profile)
                val base = chats.entries.map { (otherId, info) ->
                    val last = info["lastMessage"] as? String
                    val ts = (info["timestamp"] as? Long) ?: 0L
                    ChatThreadUi(
                        otherUserId = otherId,
                        displayName = "User #$otherId",
                        avatarUrl = null,
                        lastMessage = last,
                        timestamp = ts
                    )
                }.sortedByDescending { it.timestamp }.toMutableList()

                _threads.value = base

                // nạp profile cho từng otherId (nếu có)
                base.forEach { thread ->
                    db.child("users").child(thread.otherUserId).child("profile").get()
                        .addOnSuccessListener { snap ->
                            val name = snap.child("displayName").getValue(String::class.java)
                            val avatar = snap.child("avatarUrl").getValue(String::class.java)
                            val updated = _threads.value.toMutableList()
                            val idx = updated.indexOfFirst { it.otherUserId == thread.otherUserId }
                            if (idx >= 0) {
                                updated[idx] = updated[idx].copy(
                                    displayName = name ?: updated[idx].displayName,
                                    avatarUrl = avatar
                                )
                                _threads.value = updated
                            }
                        }
                        .addOnFailureListener {
                            // bỏ qua, dùng fallback
                        }
                }

                _loading.value = false
            }
        }
    }
}
