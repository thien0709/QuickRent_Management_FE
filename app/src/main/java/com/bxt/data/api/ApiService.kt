package com.bxt.data.api


import android.graphics.pdf.PdfDocument.Page
import com.bxt.data.api.dto.request.CategoryRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.request.UpdateUserRequest
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemImageResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.RegisterResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {

    // Authentication
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("logout")
    suspend fun logout()

    @POST("register")
    suspend fun register(@Body request: LoginRequest): LoginResponse

    @GET("refresh-token")
    suspend fun refreshToken(): LoginResponse

    // User Profile
    @GET("users/profile")
    suspend fun getUserInfo(): UserResponse

    @Multipart
    @PATCH("users/{id}/profile")
    suspend fun updateUserInfo(
        @Path("id") id: Long,
        @Body registerRequest : UpdateUserRequest
    ): RegisterResponse

    @POST("users/{id}/change-password")
    @FormUrlEncoded
    suspend fun changePassword(
        @Path("id") id: Long,
        @Field("oldPassword") oldPassword: String,
        @Field("newPassword") newPassword: String
    ): UserResponse

    @Multipart
    @PATCH("users/{id}/avatar")
    suspend fun updateUserAvatar(
        @Path("id") id: Long,
        @Part avatar: MultipartBody.Part
    ): RegisterResponse


    @DELETE("users/{id}")
    suspend fun deleteUserAccount(
        @Path("id") id: Long
    ): String

    @PATCH("users/{id}/location")
    suspend fun updateLocation(
        @Path("id") id: Long,
        @Body location: Map<String, Double>
    ): Boolean

    // Category
    @GET("categories")
    suspend fun getCategories(): List<CategoryResponse>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): CategoryResponse


    // Item
    @GET("items")
    suspend fun getItems(): List<ItemResponse>

    @GET("items/available")
    suspend fun getAvailableItems(): PagedResponse<ItemResponse>

    @GET("items/categories/{categoryId}")
    suspend fun getItemsByCategory(
        @Path("categoryId") categoryId: Long
    ): PagedResponse<ItemResponse>

    @GET("items/{id}")
    suspend fun getItemDetail(@Path("id") id: Long): ItemResponse

    @GET("items/{id}/images")
    suspend fun getItemImages(@Path("id") id: Long): List<String>


}