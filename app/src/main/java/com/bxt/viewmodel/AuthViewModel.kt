package com.bxt.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.repository.AuthRepository
import com.bxt.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class AuthViewModel : ViewModel() {
    private val repository = AuthRepository(RetrofitClient.apiService)

    private val _loginState = MutableStateFlow<Result<LoginResponse>?>(null)
    val loginState: StateFlow<Result<LoginResponse>?> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val result = repository.login(username, password)
                Log.d("AuthViewModel", "Login successful: ${result.token}")

                _loginState.value = Result.success(result).also {
                    println("API Response: $it")
                    println("Token: ${result.token}")
                }

            } catch (e: Exception) {
                _loginState.value = Result.failure(e)
            }
        }
    }
}
