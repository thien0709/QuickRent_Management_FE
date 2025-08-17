package com.bxt.data.repository

import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import java.io.File

interface UserRepository {
    suspend fun getUserInfo(): ApiResult<UserResponse>
    suspend fun updateUserInfo(
        username: String,
        email: String,
        phone: String,
        address: String,
        avatarFile: File?
    ): ApiResult<RegisterResponse>
    suspend fun updateUserAvatar( avatar: MultipartBody.Part): ApiResult<RegisterResponse>
    suspend fun updateUserPassword( oldPassword: String, newPassword: String): ApiResult<UserResponse>
    suspend fun deleteUserAccount(): ApiResult<Unit>
}