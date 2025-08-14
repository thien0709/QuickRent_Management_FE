package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ItemRepository {

    override suspend fun addItem(
        reqPart: ItemRequest,
        imageParts: List<MultipartBody.Part>
    ): ApiResult<ItemResponse> {
        return ApiCallExecutor.execute {
            apiService.addItem(reqJson = reqPart, images = imageParts)
        }
    }

    override suspend fun getAvailableItem(): ApiResult<PagedResponse<ItemResponse>> {
        return ApiCallExecutor.execute { apiService.getAvailableItems() }
    }

    override suspend fun getItemsByCategory(
        categoryId: Long,
    ): ApiResult<PagedResponse<ItemResponse>> {
        return ApiCallExecutor.execute {
            apiService.getItemsByCategory(
                categoryId = categoryId
            )
        }
    }

    override suspend fun getItemDetail(itemId: Long): ApiResult<ItemResponse> {
        return ApiCallExecutor.execute {
            apiService.getItemDetail(id = itemId)
        }
    }

}
