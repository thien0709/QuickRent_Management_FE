package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.ItemResponse
import javax.inject.Inject

class ItemRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getAvailableItem(): List<ItemResponse> {
        return apiService.getAvailableItems()
    }
//    suspend fun getItemsByCategory(categoryId: Long): List<ItemResponse> {
//        return apiService.getItemsByCategory(categoryId)
//    }
//

}