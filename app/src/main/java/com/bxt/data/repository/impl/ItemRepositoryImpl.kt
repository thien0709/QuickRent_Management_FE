package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : ItemRepository {

    override suspend fun addItem(
        reqPart: ItemRequest,
        imageParts: List<MultipartBody.Part>
    ): ApiResult<ItemResponse> {
        return apiCallExecutor.execute {
            apiService.addItem(reqJson = reqPart, images = imageParts)
        }
    }

    override suspend fun getItemInfo(itemId: Long): ApiResult<ItemResponse> {
        return apiCallExecutor.execute {
            apiService.getItemDetail(id = itemId)
        }
    }

    override suspend fun getItemImages(itemId: Long): ApiResult<List<String>> {
        return apiCallExecutor.execute {
            apiService.getItemImages(id = itemId)
        }
    }

    override suspend fun updateItem(
        itemId: Long,
        reqPart: ItemRequest,
        imageParts: List<MultipartBody.Part>
    ): ApiResult<ItemResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteItem(itemId: Long): ApiResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getAvailableItem(): ApiResult<PagedResponse<ItemResponse>> {
        return getAvailableItem(page = 0)
    }

    override suspend fun getAvailableItem(page: Int): ApiResult<PagedResponse<ItemResponse>> {
        return apiCallExecutor.execute {
            apiService.getAvailableItems(page = page)
        }
    }

    override suspend fun getItemsByUser(): ApiResult<PagedResponse<ItemResponse>> {
        return getItemsByUser(page = 0)
    }

    override suspend fun getItemsByUser(page: Int): ApiResult<PagedResponse<ItemResponse>> {
        return apiCallExecutor.execute {
            apiService.getItemsByUser(page = page)
        }
    }

    override suspend fun getItemsByCategory(categoryId: Long): ApiResult<PagedResponse<ItemResponse>> {
        return getItemsByCategory(categoryId, page = 0)
    }

    override suspend fun getItemsByCategory(categoryId: Long, page: Int): ApiResult<PagedResponse<ItemResponse>> {
        return apiCallExecutor.execute {
            apiService.getItemsByCategory(categoryId = categoryId, page = page)
        }
    }

    override suspend fun searchItems(
        query: String,
        page: Int
    ): ApiResult<PagedResponse<ItemResponse>> {
        TODO("Not yet implemented")
    }
}