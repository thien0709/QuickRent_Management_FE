package com.bxt.data.repository

import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.Multipart

interface AuthRepository {
    suspend fun login(username: String, password: String): ApiResult<LoginResponse>
    suspend fun register(request : RegisterRequest, avatar : MultipartBody.Part): ApiResult<RegisterResponse>
    suspend fun logout(): ApiResult<Unit>
    suspend fun refreshToken(): ApiResult<LoginResponse>
}
