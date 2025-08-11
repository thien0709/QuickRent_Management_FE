package com.bxt.data.repository

import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.di.ApiResult

interface ItemRepository {
    suspend fun getAvailableItem(): ApiResult<PagedResponse<ItemResponse>>
    suspend fun getItemsByCategory(
        categoryId: Long,
        page: Int,
        pageSize: Int
    ): ApiResult<PagedResponse<ItemResponse>>
}