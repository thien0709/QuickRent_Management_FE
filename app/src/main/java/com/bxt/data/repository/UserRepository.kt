package com.bxt.data.repository

import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse


interface UserRepository {
    suspend fun getUserInfo(): UserResponse
    suspend fun updateUserInfo(userId: Long,
        username: String,
        email: String,
        phone: String,
        address: String
    ): RegisterResponse
    suspend fun updateUserAvatar(userId: Long, avatar: String): UserResponse
    suspend fun updateUserPassword(userId: Long, oldPassword: String, newPassword: String): UserResponse
    suspend fun deleteUserAccount(userId: Long): Result<Unit>
}