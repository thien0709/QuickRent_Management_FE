package com.bxt.data.api


import android.graphics.pdf.PdfDocument.Page
import com.bxt.data.api.dto.request.CategoryRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.PagedResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>


    @GET("items")
    suspend fun getItems(): List<ItemResponse>

    @GET("items/available")
    suspend fun getAvailableItems(): PagedResponse<ItemResponse>


    // Image Item
    @GET("items/{id}/primary-image")
    suspend fun getItemPrimaryImage(id: Long): String

    @GET("items/{itemId}/images")
    suspend fun getItemImages(id: Long): List<String>

}