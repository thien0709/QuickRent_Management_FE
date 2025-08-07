package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Kiểm tra token trước khi gọi API
                val token = dataStoreManager.accessToken.first()
                if (token.isNullOrEmpty()) {
                    _error.value = "Không có token, vui lòng đăng nhập lại"
                    return@launch // Thoát nếu không có token
                }

                val result = userRepository.getUserInfo()
                _user.value = result
                _error.value = null // Xóa lỗi nếu thành công
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clear()
            _user.value = null
            _error.value = null
        }
    }
}