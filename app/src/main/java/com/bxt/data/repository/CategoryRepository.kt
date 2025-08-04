package com.bxt.data.repository

import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.CategoryResponse
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getCategories(): List<CategoryResponse> {
        return apiService.getCategories()
    }
}