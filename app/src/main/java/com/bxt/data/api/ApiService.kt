package com.bxt.data.api

import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.request.RefreshTokenRequest
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.*
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // === Authentication ===
    // Các endpoint này là public, không cần token
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @Multipart
    @POST("register")
    suspend fun register(
        @Part("request") request: RegisterRequest,
        @Part avatar: MultipartBody.Part
    ): RegisterResponse

    @POST("refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): LoginResponse

    @POST("logout")
    suspend fun logout()

    @GET("users/profile")
    suspend fun getUserInfo(): UserResponse

    @Multipart
    @PATCH("users/profile")
    suspend fun updateUserInfo(
        @Part("request") request: UpdateUserRequest,
        @Part avatar: MultipartBody.Part?
    ): RegisterResponse

    @POST("users/change-password")
    @FormUrlEncoded
    suspend fun changePassword(
        @Field("oldPassword") oldPassword: String,
        @Field("newPassword") newPassword: String
    ): UserResponse

    // Sửa lại: Xóa Path("id")
    @Multipart
    @PATCH("users/avatar")
    suspend fun updateUserAvatar(
        @Part avatar: MultipartBody.Part
    ): RegisterResponse

    @DELETE("users/account")
    suspend fun deleteUserAccount(): Unit

    @PATCH("users/location")
    suspend fun updateLocation(
        @Body location: Map<String, Double>
    ): Unit

    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): CategoryResponse

    // === Item ===
    @GET("items")
    suspend fun getItems(): List<ItemResponse>

    @Multipart
    @POST("items")
    suspend fun addItem(
        @Part("req") reqJson: ItemRequest,
        @Part images: List<MultipartBody.Part>
    ): ItemResponse


    @GET("items/available")
    suspend fun getAvailableItems(): PagedResponse<ItemResponse>

    @GET("items/owner")
    suspend fun getItemsByUser(): PagedResponse<ItemResponse>

    @GET("items/categories/{categoryId}")
    suspend fun getItemsByCategory(
        @Path("categoryId") categoryId: Long
    ): PagedResponse<ItemResponse>

    @GET("items/{id}")
    suspend fun getItemDetail(@Path("id") id: Long): ItemResponse

    @GET("items/{id}/images")
    suspend fun getItemImages(@Path("id") id: Long): List<String>

    @POST("rental-requests")
    suspend fun createRentalRequest(@Body request: RentalRequestRequest): RentalRequestResponse

    @GET("rental-requests/owner")
    suspend fun getRentalRequestsByOwner(): List<RentalRequestResponse>

    @GET("rental-requests/renter")
    suspend fun getRentalRequestsByRenter(): List<RentalRequestResponse>

    @GET("rental-requests/{id}")
    suspend fun updateRentalRequest(id: String, request: RentalRequestRequest) : RentalRequestResponse

    @PATCH("rental-requests/{id}")
    suspend fun updateRequestStatus(requestId: Long, newStatus: String) : RentalRequestResponse


}