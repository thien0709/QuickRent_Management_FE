package com.bxt.data.repository

import com.bxt.data.api.dto.response.LoginResponse

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<LoginResponse>
    suspend fun register(username: String, password: String): Result<LoginResponse>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<LoginResponse>
}