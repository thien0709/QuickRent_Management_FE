package com.bxt.data.api

import android.graphics.pdf.PdfDocument.Page
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.request.LoginRequest
import com.bxt.data.api.dto.request.PromptRequest
import com.bxt.data.api.dto.request.RefreshTokenRequest
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.api.dto.request.RegisterTokenRequest
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.request.TransportPackageRequest
import com.bxt.data.api.dto.request.TransportPassengerRequest
import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.api.dto.request.UpdateProfileRequest
import com.bxt.data.api.dto.response.*
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import java.math.BigDecimal

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
    suspend fun getUserNameById(@Path("id") id: Long): UserResponse

    @GET("users/profile")
    suspend fun getUserInfo(): UserResponse

    @Multipart
    @PATCH("users/profile")
    suspend fun updateUserInfo(
        @Part("request") request: UpdateProfileRequest,
        @Part avatar: MultipartBody.Part?
    ): RegisterResponse

    @GET("users/{id}/location")
    suspend fun getUserLocationById(@Path("id") id: Long): Map<String, BigDecimal>

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


    @POST("items/search")
    suspend fun searchItems(
        @Body request: ItemRequest,
        @Query("page") page: Int,
        @Query("centerLat") centerLat: Double? = null,
        @Query("centerLng") centerLng: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null
    ): PagedResponse<ItemResponse>


    @GET("items/{id}")
    suspend fun getItemDetail(@Path("id") id: Long): ItemResponse

    @GET("items/{id}/images")
    suspend fun getItemImages(@Path("id") id: Long): List<String>

    // Rental Services
    @POST("rental-requests")
    suspend fun createRentalRequest(@Body request: RentalRequestRequest): RentalRequestResponse

    @GET("rental-requests/owner")
    suspend fun getRentalRequestsByOwner(@Query("page") page: Int): PagedResponse<RentalRequestResponse>

    @GET("rental-requests/renter")
    suspend fun getRentalRequestsByRenter(@Query("page") page: Int): PagedResponse<RentalRequestResponse>

    @GET("rental-requests/renter/on-confirm")
    suspend fun getRentalRequestsByRenterOnConfirm(): List<RentalRequestResponse>

    @PATCH("rental-requests/{id}")
    suspend fun updateRentalRequest(
        @Path("id") id: Long,
        @Body request: RentalRequestRequest
    ): RentalRequestResponse

    @PATCH("rental-requests/{id}/confirm")
    suspend fun confirmRentalRequest(@Path("id") requestId: Long): RentalRequestResponse

    @PATCH("rental-requests/{id}/reject")
    suspend fun rejectRentalRequest(@Path("id") requestId: Long):RentalRequestResponse

    @PATCH("rental-requests/{id}/cancel")
    suspend fun cancelRentalRequest(@Path("id") requestId: Long): RentalRequestResponse

    @PATCH("rental-requests/{id}/start")
    suspend fun startRentalRequest(@Path("id") requestId: Long): RentalRequestResponse

    @PATCH("rental-requests/{id}/complete")
    suspend fun completeRentalRequest(@Path("id") requestId: Long):RentalRequestResponse


    @GET("rental-requests/{id}")
    suspend fun getRentalRequestById(@Path("id") id: Long): RentalRequestResponse

    // Transport Services
    // Rental Transactions
    @GET("rental-transactions/{transactionId}/images")
    suspend fun getTransactionImages(
        @Path("transactionId") transactionId: Long
    ): List<TransactionImageResponse>

    @PATCH("rental-transactions/{transactionId}/confirm-pickup")
    suspend fun confirmPickup(
        @Path("transactionId") transactionId: Long,
        @Query("status") newStatus: String
    ): RentalTransactionResponse

    @GET("rental-transactions/by-request/{requestId}")
    suspend fun getRentalTransactionByRequestId(@Path("requestId") requestId: Long): RentalTransactionResponse

    @Multipart
    @POST("rental-transactions/{transactionId}/images")
    suspend fun uploadTransactionImages(
        @Path("transactionId") transactionId: Long,
        @Part("imageType") imageType: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): List<TransactionImageResponse>

    // Firebase Cloud Messaging Token
    @POST("fcm/register")
    suspend fun register(@Body body: RegisterTokenRequest): Unit

    @DELETE("fcm/register")
    suspend fun unregister(@Query("token") token: String): Unit

    // Chat With AI
    @POST("chat-gemini")
    suspend fun chatWithGemini(@Body request: PromptRequest): ChatResponse

    @GET("transport-services")
    suspend fun getTransportServices(): List<TransportServiceResponse>

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
    suspend fun deleteTransportService(@Path("id") id: Long)

    // ✅ ĐÚNG với backend: POST + @Query("status")
    @POST("transport-services/{id}/status")
    suspend fun updateServiceStatus(
        @Path("id") serviceId: Long,
        @Query("status") status: String
    ): TransportServiceResponse

    // Hành động lifecycle
    @POST("transport-services/{id}/confirm")
    suspend fun confirmTransportService(@Path("id") id: Long): TransportServiceResponse

    @POST("transport-services/{id}/start")
    suspend fun startTransportService(@Path("id") id: Long): TransportServiceResponse

    @POST("transport-services/{id}/complete")
    suspend fun completeTransportService(@Path("id") id: Long): TransportServiceResponse

    @POST("transport-services/{id}/cancel")
    suspend fun cancelTransportService(
        @Path("id") id: Long,
        @Query("reason") reason: String? = null
    ): TransportServiceResponse

    @GET("transport-services/{id}/bookings-info")
    suspend fun getServiceBookingsInfo(
        @Path("id") id: Long
    ): Map<String, @JvmSuppressWildcards Any>

    @GET("transport-services/{serviceId}/packages")
    suspend fun getTransportPackagesByServiceId(
        @Path("serviceId") serviceId: Long
    ): List<TransportPackageResponse>

    @GET("transport-services/{serviceId}/passengers")
    suspend fun getTransportPassengersByServiceId(
        @Path("serviceId") serviceId: Long
    ): List<TransportPassengerResponse>



// --- TRANSPORT PASSENGERS ---

    @POST("transport-passengers")
    suspend fun createTransportPassenger(
        @Body request: TransportPassengerRequest
    ): TransportPassengerResponse

    @GET("transport-passengers/{id}")
    suspend fun getTransportPassengerById(@Path("id") id: Long): TransportPassengerResponse

    @PATCH("transport-passengers/{id}")
    suspend fun updateTransportPassenger(
        @Path("id") id: Long,
        @Body request: TransportPassengerRequest
    ): TransportPassengerResponse

    @DELETE("transport-passengers/{id}")
    suspend fun deleteTransportPassenger(@Path("id") id: Long): Map<String, String>

    // Danh sách của tôi (owner) & tôi tham gia (rental)
    @GET("transport-passengers/owner")
    suspend fun getTransportPassengersOwner(
        @Query("page") page: Int
    ): PagedResponse<TransportPassengerResponse>

    @GET("transport-passengers/rental")
    suspend fun getTransportPassengersRental(
        @Query("page") page: Int
    ): PagedResponse<TransportPassengerResponse>

    // Đặt xe
    @POST("transport-passengers/book")
    suspend fun bookRide(
        @Body request: TransportPassengerRequest
    ): TransportPassengerResponse

    // ✅ HỦY BOOKING: đúng endpoint là POST /{id}/cancel-booking
    @POST("transport-passengers/{id}/cancel-booking")
    suspend fun cancelRideBooking(
        @Path("id") bookingId: Long
    ): Map<String, String>

    // (A) GIỮ NGUYÊN THEO BACKEND BẠN DÁN: PATCH + @RequestParam
    @PATCH("transport-passengers/{id}/status")
    suspend fun updateTransportPassengerStatusPatch(
        @Path("id") id: Long,
        @Query("status") status: String
    ): TransportServiceResponse // (type đang lạ ở backend)


// --- TRANSPORT PACKAGES ---

    @GET("transport-packages")
    suspend fun getAllTransportPackages(): List<TransportPackageResponse>

    @GET("transport-packages/{id}")
    suspend fun getTransportPackageById(@Path("id") id: Long): TransportPackageResponse

    @POST("transport-packages")
    suspend fun createTransportPackage(
        @Body request: TransportPackageRequest
    ): TransportPackageResponse

    @PATCH("transport-packages/{id}")
    suspend fun updateTransportPackage(
        @Path("id") id: Long,
        @Body request: TransportPackageRequest
    ): TransportPackageResponse

    @DELETE("transport-packages/{id}")
    suspend fun deleteTransportPackage(@Path("id") id: Long): Map<String, String>

    @GET("transport-packages/owner")
    suspend fun getTransportPackagesOwner(
        @Query("page") page: Int
    ): PagedResponse<TransportPackageResponse>

    @GET("transport-packages/rental")
    suspend fun getTransportPackagesRental(
        @Query("page") page: Int
    ): PagedResponse<TransportPackageResponse>

    // ✅ ĐÚNG PATH backend: /request-delivery
    @POST("transport-packages/request-delivery")
    suspend fun requestPackageDelivery(
        @Body request: TransportPackageRequest
    ): TransportPackageResponse

    // ✅ HỦY YÊU CẦU: POST /{id}/cancel-delivery
    @POST("transport-packages/{id}/cancel-delivery")
    suspend fun cancelPackageDelivery(
        @Path("id") packageId: Long
    ): Map<String, String>

    // (A) GIỮ NGUYÊN THEO BACKEND BẠN DÁN: PATCH + @RequestParam
    @PATCH("transport-packages/{id}/status")
    suspend fun updateTransportPackageStatusPatch(
        @Path("id") id: Long,
        @Query("status") status: String
    ): TransportServiceResponse


}
