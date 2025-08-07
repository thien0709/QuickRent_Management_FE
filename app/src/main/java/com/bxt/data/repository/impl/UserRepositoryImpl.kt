package com.bxt.data.repository.impl

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.repository.UserRepository
import javax.inject.Inject


class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {
    override suspend fun getUserInfo(): UserResponse {
        return apiService.getUserInfo()
    }

    override suspend fun updateUserInfo(
        userId: Long,
        username: String,
        email: String,
        phone: String,
        address: String
    ): RegisterResponse {
        val request = UpdateUserRequest(
            username = username,
            email = email,
            phoneNumber = phone,
            address = address
        )
        return apiService.updateUserInfo(userId, request)
    }


    override suspend fun updateUserAvatar(userId: Long, avatar: String): UserResponse {
        TODO("Not yet implemented")
    }

    override suspend fun updateUserPassword(
        userId: Long,
        oldPassword: String,
        newPassword: String
    ): UserResponse {
        TODO("Not yet implemented")
    }

    override suspend fun deleteUserAccount(userId: Long): Result<Unit> {
        TODO("Not yet implemented")
    }


}