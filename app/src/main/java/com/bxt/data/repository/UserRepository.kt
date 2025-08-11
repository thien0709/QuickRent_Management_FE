package com.bxt.data.repository

import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody

interface UserRepository {
    suspend fun getUserInfo(): ApiResult<UserResponse>
    suspend fun updateUserInfo(
        userId: Long,
        username: String,
        email: String,
        phone: String,
        address: String
    ): ApiResult<RegisterResponse>
    suspend fun updateUserAvatar(userId: Long, avatar: MultipartBody.Part): ApiResult<RegisterResponse>
    suspend fun updateUserPassword(userId: Long, oldPassword: String, newPassword: String): ApiResult<UserResponse>
    suspend fun deleteUserAccount(userId: Long): ApiResult<Unit>
}