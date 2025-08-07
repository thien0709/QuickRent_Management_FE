package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginFormState(
    val email: String = "",
    val password: String = ""
)

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> get() = _formState

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> get() = _loginState

    fun onEmailChanged(email: String) {
        _formState.value = _formState.value.copy(email = email)
    }

    fun onPasswordChanged(password: String) {
        _formState.value = _formState.value.copy(password = password)
    }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val result = authRepository.login(
                    _formState.value.email,
                    _formState.value.password
                )
                result.onSuccess {
                    dataStore.saveAccessToken(it.accessToken)
                    dataStore.saveRefreshToken(it.refreshToken)
                    _loginState.value = LoginUiState.Success
                }
                result.onFailure {
                    _loginState.value = LoginUiState.Error(it.message ?: "Lỗi đăng nhập")
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginUiState.Idle
    }
}
