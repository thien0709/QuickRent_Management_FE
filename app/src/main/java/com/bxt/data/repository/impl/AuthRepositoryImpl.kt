package com.bxt.data.repository.impl

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username = username, password = password)
            val response = apiService.login(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username = username, password = password)
            val response = apiService.register(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            apiService.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<LoginResponse> {
        return try {
            val response = apiService.refreshToken()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
