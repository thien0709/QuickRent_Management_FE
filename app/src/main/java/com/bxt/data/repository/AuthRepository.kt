package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.local.DataStoreManager
import javax.inject.Inject


class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(username: String, password: String): LoginResponse {
        val request = LoginRequest(username = username, password = password)
        return apiService.login(request)
    }
}
