package com.bxt.data.api

import com.bxt.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = runBlocking {
            dataStoreManager.accessToken.first()
        }
        val requestBuilder = originalRequest.newBuilder()
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        val newRequest = requestBuilder.build()

         return chain.proceed(newRequest)
    }
}