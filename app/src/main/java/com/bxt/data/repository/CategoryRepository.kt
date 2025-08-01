package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.CategoryResponse

class CategoryRepository(private val apiService: ApiService) {

    suspend fun getCategories(): List<CategoryResponse> {
        return apiService.getCategories()
    }
}