package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.ItemImageResponse
import com.bxt.di.ApiResult
import javax.inject.Inject

class ItemImageRepository @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) {
    suspend fun getItemImages(itemId: Long): ApiResult<List<String>> {
        return apiCallExecutor.execute { apiService.getItemImages(itemId) }
    }
}
