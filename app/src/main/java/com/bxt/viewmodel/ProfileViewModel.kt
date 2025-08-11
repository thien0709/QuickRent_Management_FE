package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // Kiểm tra token
                val token = dataStoreManager.accessToken.first()
                if (token.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shouldNavigateToLogin = true,
                        error = "Phiên đăng nhập hết hạn"
                    )
                    return@launch
                }

                val userInfo = userRepository.getUserInfo()

                _uiState.value = _uiState.value.copy(
                    user = userInfo,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khi tải thông tin: ${e.message ?: "Không xác định"}"
                )

                // Nếu lỗi 401 Unauthorized thì chuyển sang màn hình login
                if (e.message?.contains("401") == true) {
                    _uiState.value = _uiState.value.copy(
                        shouldNavigateToLogin = true
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clear()
            _uiState.value = ProfileUiState(
                shouldNavigateToLogin = true
            )
        }
    }
}

data class ProfileUiState(
    val user: ApiResult<UserResponse>? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val shouldNavigateToLogin: Boolean = false
)