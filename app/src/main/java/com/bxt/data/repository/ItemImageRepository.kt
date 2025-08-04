package com.bxt.data.repository

import com.bxt.data.api.ApiService
import javax.inject.Inject

class ItemImageRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getItemImages(itemId: Long): List<String> {
        return apiService.getItemImages(itemId)
    }
}