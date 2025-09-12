package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.RentalTransactionResponse
import com.bxt.data.api.dto.response.TransactionImageResponse
import com.bxt.data.repository.RentalTransactionRepository
import com.bxt.di.ApiResult
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class RentalTransactionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : RentalTransactionRepository {


    override suspend fun getTransactionByRequestId(requestId: Long): ApiResult<RentalTransactionResponse> {
        return apiCallExecutor.execute {
            apiService.getRentalTransactionByRequestId(requestId)
        }
    }

    override suspend fun getImagesByTransactionId(transactionId: Long): ApiResult<List<TransactionImageResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransactionImages(transactionId)
        }
    }

    override suspend fun confirmPickup(transactionId: Long, newStatus: String): ApiResult<RentalTransactionResponse> {
        return apiCallExecutor.execute {
            apiService.confirmPickup(transactionId, newStatus)
        }
    }

    override suspend fun uploadTransactionImages(
        transactionId: Long,
        imageType: String,
        images: List<MultipartBody.Part>
    ): ApiResult<List<TransactionImageResponse>> {
        return apiCallExecutor.execute {
            val imageTypeRequestBody = imageType.toRequestBody(MultipartBody.FORM)
            apiService.uploadTransactionImages(transactionId, imageTypeRequestBody, images)
        }
    }

    
}