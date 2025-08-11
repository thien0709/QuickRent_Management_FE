package com.bxt.data.repository

import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.di.ApiResult

interface AuthRepository {
    suspend fun login(username: String, password: String): ApiResult<LoginResponse>
    suspend fun register(username: String, password: String): ApiResult<LoginResponse>
    suspend fun logout(): ApiResult<Unit>
    suspend fun refreshToken(): ApiResult<LoginResponse>
}
