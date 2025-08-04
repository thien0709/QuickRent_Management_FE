package com.bxt.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.repository.AuthRepository
import com.bxt.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository : AuthRepository,
    private val dataStore : DataStoreManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<LoginResponse>?>(null)
    val loginState: StateFlow<Result<LoginResponse>?> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val result = repository.login(username, password)
                Log.d("AuthViewModel", "Login successful: ${result.accessToken}")

                _loginState.value = Result.success(result).also {
                    println("API Response: $it")
                    println("Token: ${result.accessToken}")
                    dataStore.saveAccessToken(result.accessToken)
                    dataStore.saveRefreshToken(result.refreshToken)
                    Log.d("AuthViewModel", "Access token saved: ${result.accessToken}")
                }

            } catch (e: Exception) {
                _loginState.value = Result.failure(e)
            }
        }
    }
}
