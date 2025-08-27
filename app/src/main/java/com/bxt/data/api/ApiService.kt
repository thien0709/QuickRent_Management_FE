package com.bxt.data.api

import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.request.RefreshTokenRequest
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.*
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // User Authentication
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

    @GET("users/{id}")
    suspend fun getUserNameById(@Path("id") id: Long): String

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

    // Category
    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): CategoryResponse

    // Items
    @GET("items")
    suspend fun getItems(): List<ItemResponse>

    @Multipart
    @POST("items")
    suspend fun addItem(
        @Part("req") reqJson: ItemRequest,
        @Part images: List<MultipartBody.Part>
    ): ItemResponse

    @GET("items/available")
    suspend fun getAvailableItems(
        @Query("page") page: Int = 0
    ): PagedResponse<ItemResponse>

    @GET("items/owner")
    suspend fun getItemsByUser(
        @Query("page") page: Int = 0
    ): PagedResponse<ItemResponse>

    @GET("items/categories/{categoryId}")
    suspend fun getItemsByCategory(
        @Path("categoryId") categoryId: Long,
        @Query("page") page: Int = 0
    ): PagedResponse<ItemResponse>


    @POST("api/items/search")
    suspend fun searchItems(
        @Body request: ItemRequest,
        @Query("page") page: Int
    ): PagedResponse<ItemResponse>

    @GET("items/{id}")
    suspend fun getItemDetail(@Path("id") id: Long): ItemResponse

    @GET("items/{id}/images")
    suspend fun getItemImages(@Path("id") id: Long): List<String>

    // Rental Services
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

    // Transport Services
    @GET("transport-services")
    suspend fun getTransportServices(): List<TransportServiceResponse>

    @GET("transport-services/owner")
    suspend fun getTransportServicesByOwner(): List<TransportServiceResponse>

    @GET("transport-services/renter")
    suspend fun getTransportServicesByRenter(): List<TransportServiceResponse>

    @GET("transport-services/{id}")
    suspend fun getTransportServiceById(@Path("id") id: Long): TransportServiceResponse

    @POST("transport-services")
    suspend fun createTransportService(@Body request: TransportServiceRequest): TransportServiceResponse

    @PATCH("transport-services/{id}")
    suspend fun updateTransportService(
        @Path("id") id: Long,
        @Body request: TransportServiceRequest
    ): TransportServiceResponse

    @DELETE("transport-services/{id}")
    suspend fun deleteTransportService(id: Long)

    @PATCH("transport-services/{id}/status")
    suspend fun updateServiceStatus(
        @Path("id") serviceId: Long,
        @Body newStatus: String
    ): TransportServiceResponse

}
