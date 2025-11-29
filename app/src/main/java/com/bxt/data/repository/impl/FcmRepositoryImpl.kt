package com.bxt.data.repository.impl

import android.os.Build
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.RegisterTokenRequest
import com.bxt.data.repository.FcmRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor(
    private val api: ApiService
) : FcmRepository {
    override suspend fun registerToken(token: String, userId: Long?) {
        val device = "${Build.BRAND} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        api.register(RegisterTokenRequest(fcmToken = token, deviceInfo = device, userId = userId))
    }

    override suspend fun unregisterToken(token: String) {
        api.unregister(token)
    }
}
