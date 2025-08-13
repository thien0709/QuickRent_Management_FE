package com.bxt.data.repository

import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.di.ApiResult

interface CategoryRepository {
    suspend fun getCategories(): ApiResult<List<CategoryResponse>>
    suspend fun getCategoryById(categoryId: Long): ApiResult<CategoryResponse>
}