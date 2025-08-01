package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.LoginResponse

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(username: String, password: String): LoginResponse {
        return apiService.login(LoginRequest(username, password))
    }
}
