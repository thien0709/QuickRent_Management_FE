package com.bxt.data.repository

import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart

interface ItemRepository {

    suspend fun addItem(
        reqPart: ItemRequest,
        imageParts: List<MultipartBody.Part>
    ): ApiResult<ItemResponse>

    suspend fun getAvailableItem(): ApiResult<PagedResponse<ItemResponse>>
    suspend fun getItemsByCategory(
        categoryId: Long,
    ): ApiResult<PagedResponse<ItemResponse>>

    suspend fun getItemDetail(
        itemId: Long,
    ): ApiResult<ItemResponse>

    suspend fun getItemsByUser(
    ): ApiResult<PagedResponse<ItemResponse>>

    suspend fun updateItem(
        itemId: Long,
        reqPart: ItemRequest,
        imageParts: List<MultipartBody.Part>
    ): ApiResult<ItemResponse>

    suspend fun deleteItem(
        itemId: Long,
    ): ApiResult<Unit>


}