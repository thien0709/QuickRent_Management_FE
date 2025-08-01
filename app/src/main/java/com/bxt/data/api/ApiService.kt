package com.bxt.data.api


import com.bxt.data.api.dto.request.CategoryRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface ApiService {
    // User Authentication
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    //Category Management
    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>


    // Item Management
    @GET("items")
    suspend fun getItems(): List<ItemResponse>

    @GET("items/available")
    suspend fun getAvailableItems(): List<ItemResponse>


}