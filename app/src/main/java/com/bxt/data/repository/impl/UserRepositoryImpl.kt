package com.bxt.data.repository.impl

import androidx.core.net.toUri
import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.UpdateProfileRequest
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import java.math.BigDecimal
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : UserRepository {

    override suspend fun getUserInfo(): ApiResult<UserResponse> {
        return apiCallExecutor.execute { apiService.getUserInfo() }
    }


    override suspend fun getUserLocation(id: Long): ApiResult<Map<String, BigDecimal>> {
        return apiCallExecutor.execute { apiService.getUserLocationById(id) }
    }

    override suspend fun updateUserInfo(
        request : UpdateProfileRequest,
        avatarFile: MultipartBody.Part?
    ): ApiResult<RegisterResponse> {
        return apiCallExecutor.execute { apiService.updateUserInfo(request, avatarFile) }
    }

    override suspend fun updateUserAvatar(
        avatar: MultipartBody.Part
    ): ApiResult<RegisterResponse> {
        return apiCallExecutor.execute { apiService.updateUserAvatar(avatar) }
    }

    override suspend fun updateUserPassword(
        oldPassword: String,
        newPassword: String
    ): ApiResult<UserResponse> {
        return apiCallExecutor.execute { apiService.changePassword(oldPassword, newPassword) }
    }

    override suspend fun deleteUserAccount(): ApiResult<Unit> {
        return apiCallExecutor.execute { apiService.deleteUserAccount() }
    }
}