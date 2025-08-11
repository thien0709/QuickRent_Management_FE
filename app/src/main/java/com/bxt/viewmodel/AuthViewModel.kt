package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import com.bxt.di.ApiResult
import com.bxt.ui.screen.ErrorPopupManager
import com.bxt.ui.state.LoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginRequest())
    val formState: StateFlow<LoginRequest> get() = _formState

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> get() = _loginState

    fun onUsernameChanged(username: String) {
        _formState.value = _formState.value.copy(username = username)
    }

    fun onPasswordChanged(password: String) {
        _formState.value = _formState.value.copy(password = password)
    }
    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = authRepository.login(
                    _formState.value.username,
                    _formState.value.password
                )
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        dataStore.saveAccessToken(data.accessToken)
                        dataStore.saveRefreshToken(data.refreshToken)
                        _loginState.value = LoginState.Success(
                            LoginResponse(data.accessToken,data.username,data.refreshToken,data.role)
                        )
                    }
                    is ApiResult.Error -> {
                        _loginState.value = LoginState.Error(result.error.message ?: "Lỗi đăng nhập")
                        ErrorPopupManager.showError(result.error.message ?: "Lỗi đăng nhập")
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Lỗi không xác định")
                ErrorPopupManager.showError(e.message ?: "Lỗi không xác định")
            } finally {
                if (_loginState.value !is LoginState.Success) {
                    _loginState.value = LoginState.Idle
                }
            }
        }
    }


    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
