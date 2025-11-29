package com.bxt.data.repository

import com.bxt.data.api.dto.response.RentalTransactionResponse
import com.bxt.data.api.dto.response.TransactionImageResponse
import com.bxt.di.ApiResult
import okhttp3.MultipartBody

interface RentalTransactionRepository {
    suspend fun getTransactionByRequestId(requestId: Long): ApiResult<RentalTransactionResponse>
    suspend fun getImagesByTransactionId(transactionId: Long): ApiResult<List<TransactionImageResponse>>
    suspend fun confirmPickup(transactionId: Long, newStatus: String): ApiResult<RentalTransactionResponse>
    suspend fun uploadTransactionImages(
        transactionId: Long,
        imageType: String,
        images: List<MultipartBody.Part>
    ): ApiResult<List<TransactionImageResponse>>


}