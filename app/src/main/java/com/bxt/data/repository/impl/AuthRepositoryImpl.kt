package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.repository.AuthRepository
import com.bxt.di.ApiResult
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val request = LoginRequest(username = username, password = password)
        return ApiCallExecutor.execute { apiService.login(request) }
    }

    override suspend fun register(username: String, password: String): ApiResult<LoginResponse> {
        val request = LoginRequest(username = username, password = password)
        return ApiCallExecutor.execute { apiService.register(request) }
    }

    override suspend fun logout(): ApiResult<Unit> {
        return ApiCallExecutor.execute { apiService.logout() }
    }

    override suspend fun refreshToken(): ApiResult<LoginResponse> {
        return ApiCallExecutor.execute { apiService.refreshToken() }
    }
}
