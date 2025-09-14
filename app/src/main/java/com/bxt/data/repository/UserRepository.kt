package com.bxt.data.repository

import com.bxt.data.api.dto.request.UpdateProfileRequest
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import java.math.BigDecimal

interface UserRepository {
    suspend fun getUserInfo(): ApiResult<UserResponse>
    suspend fun getUserLocation(id: Long): ApiResult<Map<String, BigDecimal>>
    suspend fun updateUserInfo(
        request : UpdateProfileRequest,
        avatarFile: MultipartBody.Part?
    ): ApiResult<RegisterResponse>
    suspend fun updateUserAvatar( avatar: MultipartBody.Part): ApiResult<RegisterResponse>
    suspend fun updateUserPassword( oldPassword: String, newPassword: String): ApiResult<UserResponse>
    suspend fun deleteUserAccount(): ApiResult<Unit>
}