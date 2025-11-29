package com.bxt.data.repository

import android.os.Build
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.RegisterTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

interface FcmRepository {
    suspend fun registerToken(token: String, userId: Long?)
    suspend fun unregisterToken(token: String)
}