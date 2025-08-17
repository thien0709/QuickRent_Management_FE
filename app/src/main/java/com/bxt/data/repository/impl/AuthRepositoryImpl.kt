package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.request.RefreshTokenRequest
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import com.bxt.di.ApiResult
import com.bxt.di.ErrorResponse
import com.bxt.di.ErrorType
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val request = LoginRequest(username = username, password = password)
        return apiCallExecutor.execute { apiService.login(request) }
    }

    override suspend fun register(request: RegisterRequest, avatar : MultipartBody.Part): ApiResult<RegisterResponse> {
        return apiCallExecutor.execute { apiService.register(request,avatar) }
    }

    override suspend fun logout(): ApiResult<Unit> {
        return apiCallExecutor.execute { apiService.logout() }
    }

    override suspend fun refreshToken(): ApiResult<LoginResponse> {
         val currentRefreshToken = dataStoreManager.refreshToken.first()

           if (currentRefreshToken.isNullOrBlank()) {
              return ApiResult.Error(
                ErrorResponse(
                    message = "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.",
                    canRetry = false,
                    type = ErrorType.SESSION_EXPIRED
                )
            )
        }
        val request = RefreshTokenRequest(refreshToken = currentRefreshToken)
        val result = apiCallExecutor.execute { apiService.refreshToken(request) }

        result.onSuccess { loginResponse ->
            dataStoreManager.saveAuthData(
                token = loginResponse.accessToken,
                userId =  loginResponse.userId,
                refreshToken = loginResponse.refreshToken
            )
        }

        // BƯỚC 5: Trả về kết quả
        return result
    }
}
