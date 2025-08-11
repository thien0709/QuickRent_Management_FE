package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    override suspend fun getUserInfo(): ApiResult<UserResponse> {
        return ApiCallExecutor.execute { apiService.getUserInfo() }
    }

    override suspend fun updateUserInfo(
        userId: Long,
        username: String,
        email: String,
        phone: String,
        address: String
    ): ApiResult<RegisterResponse> {
        val request = UpdateUserRequest(username, email, phone, address)
        return ApiCallExecutor.execute { apiService.updateUserInfo(userId, request) }
    }

    override suspend fun updateUserAvatar(
        userId: Long,
        avatar: MultipartBody.Part
    ): ApiResult<RegisterResponse> {
        return ApiCallExecutor.execute { apiService.updateUserAvatar(userId, avatar) }
    }

    override suspend fun updateUserPassword(
        userId: Long,
        oldPassword: String,
        newPassword: String
    ): ApiResult<UserResponse> {
        return ApiCallExecutor.execute { apiService.changePassword(userId, oldPassword, newPassword) }
    }

    override suspend fun deleteUserAccount(userId: Long): ApiResult<Unit> {
        return ApiCallExecutor.execute { apiService.deleteUserAccount(userId) }
    }
}