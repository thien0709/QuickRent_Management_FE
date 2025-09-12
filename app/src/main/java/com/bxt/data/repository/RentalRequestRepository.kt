package com.bxt.data.repository

import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.di.ApiResult

interface RentalRequestRepository {
    suspend fun getRentalRequestById(id: Long): ApiResult<RentalRequestResponse>
    suspend fun createRentalRequest(request: RentalRequestRequest): ApiResult<RentalRequestResponse>
    suspend fun updateRentalRequest(id: Long, request: RentalRequestRequest): ApiResult<RentalRequestResponse>
    suspend fun deleteRentalRequest(id: Long): ApiResult<Unit>
    suspend fun getRentalRequestsByRenter(page: Int): ApiResult<PagedResponse<RentalRequestResponse>>
    suspend fun getRentalRequestsByOwner(page: Int): ApiResult<PagedResponse<RentalRequestResponse>>
    suspend fun confirmRequest(requestId: Long): ApiResult<RentalRequestResponse>
    suspend fun rejectRequest(requestId: Long): ApiResult<RentalRequestResponse>
    suspend fun cancelRequest(requestId: Long): ApiResult<RentalRequestResponse>
    suspend fun completeRequest(requestId: Long): ApiResult<RentalRequestResponse>
    suspend fun startRental(requestId: Long): ApiResult<RentalRequestResponse>
}