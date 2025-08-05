package com.bxt.data.repository

import com.bxt.data.api.ApiService
import javax.inject.Inject


class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
//    suspend fun getUserInfo(): String {
//        return apiService.getUserInfo()
//    }
//
//    suspend fun updateUserInfo(name: String, email: String): String {
//        return apiService.updateUserInfo(name, email)
//    }
//
//    suspend fun deleteUserAccount(): String {
//        return apiService.deleteUserAccount()
//    }
}