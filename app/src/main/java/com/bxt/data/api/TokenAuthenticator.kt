package com.bxt.data.api // Hoặc package phù hợp

import com.bxt.data.local.DataStoreManager
import com.google.gson.Gson // Thêm thư viện Gson nếu chưa có
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import javax.inject.Inject
import javax.inject.Named

data class AccessTokenResponse(val accessToken: String)

class TokenAuthenticator @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    @Named("BaseUrl") private val baseUrl: String
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = runBlocking { dataStoreManager.refreshToken.first() }
        if (refreshToken.isNullOrBlank()) {
            return null
        }


        val client = OkHttpClient()
        val refreshRequest = Request.Builder()
            .url("${baseUrl}api/refresh-token")
            .header("Authorization", "Bearer $refreshToken")
            .post(RequestBody.create(null, byteArrayOf()))
            .build()

        try {
            val refreshResponse = client.newCall(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                val newAccessToken = Gson().fromJson(responseBody, AccessTokenResponse::class.java).accessToken
                runBlocking { dataStoreManager.saveAccessToken(newAccessToken) }
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        runBlocking { dataStoreManager.clearAll() }
        return null
    }
}