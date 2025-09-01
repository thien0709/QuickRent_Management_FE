package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
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
        username: String,
        email: String,
        phone: String,
        address: String,
        avatarFile: File?
    ): ApiResult<RegisterResponse> {
        val request = UpdateUserRequest(username, email, phone, address)
        val avatarPart: MultipartBody.Part? = avatarFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("avatar", file.name, requestFile)
        }

        return apiCallExecutor.execute { apiService.updateUserInfo(request, avatarPart) }
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