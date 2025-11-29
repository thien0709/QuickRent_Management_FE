package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.NotificationResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.NotificationRepository
import com.bxt.di.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _notificationsState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val notificationsState: StateFlow<NotificationUiState> = _notificationsState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = NotificationUiState.Loading
            when (val result = notificationRepository.getAllNotifications()) {
                is ApiResult.Success -> {
                    _notificationsState.value = NotificationUiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _notificationsState.value = NotificationUiState.Error(
                        result.error.message ?: "Không thể tải thông báo"
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            val currentState = _notificationsState.value
            if (currentState is NotificationUiState.Success) {
                val updatedList = currentState.notifications.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                _notificationsState.value = NotificationUiState.Success(updatedList)
            }

            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val currentState = _notificationsState.value
            if (currentState is NotificationUiState.Success) {
                val updatedList = currentState.notifications.map { notification ->
                    notification.copy(isRead = true)
                }
                _notificationsState.value = NotificationUiState.Success(updatedList)
            }

            notificationRepository.markAllAsRead()
        }
    }
}

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(val notifications: List<NotificationResponse>) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}