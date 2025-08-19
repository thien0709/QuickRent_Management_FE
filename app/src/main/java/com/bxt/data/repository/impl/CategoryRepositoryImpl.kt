package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.repository.CategoryRepository
import com.bxt.di.ApiResult
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : CategoryRepository {

    override suspend fun getCategories(): ApiResult<List<CategoryResponse>> {
        return apiCallExecutor.execute { apiService.getCategories() }
    }

    override suspend fun getCategoryById(categoryId: Long): ApiResult<CategoryResponse> {
        return apiCallExecutor.execute { apiService.getCategoryById(categoryId) }
    }

//    override suspend fun addCategory(category: String): ApiResult<Unit> {
//        return apiCallExecutor.execute { apiService.addCategory(category) }
//    }
//
//    override suspend fun updateCategory(oldCategory: String, newCategory: String): ApiResult<Unit> {
//        return apiCallExecutor.execute { apiService.updateCategory(oldCategory, newCategory) }
    }
