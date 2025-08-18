package com.bxt.data.api

import com.bxt.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
class AuthInterceptor @Inject constructor(
    private val dataStore: DataStoreManager
) : Interceptor {

    private val whiteList = listOf(
        "login",
        "register",
        "refresh"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath

        if (whiteList.any { path.endsWith(it) }) {
            return chain.proceed(req)
        }

        val token = runBlocking { dataStore.accessToken.first() }
        val newReq = if (!token.isNullOrBlank()) {
            req.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            req
        }
        return chain.proceed(newReq)
    }
}
