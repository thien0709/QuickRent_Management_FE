package com.bxt.data.repository

import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.di.ApiResult

interface RentalRequestRepository {
    suspend fun getRentalRequests(): ApiResult<List<RentalRequestResponse>>
    suspend fun getRentalRequestById(id: String): ApiResult<RentalRequestResponse>
    suspend fun createRentalRequest(request: RentalRequestRequest): ApiResult<RentalRequestResponse>
    suspend fun updateRentalRequest(id: String, request: RentalRequestRequest): ApiResult<RentalRequestResponse>
    suspend fun deleteRentalRequest(id: String): ApiResult<Unit>
    suspend fun getRentalRequestsByRenter(): ApiResult<List<RentalRequestResponse>>
    suspend fun getRentalRequestsByOwner(): ApiResult<List<RentalRequestResponse>>
    suspend fun updateRequestStatus(requestId: Long, newStatus: String): ApiResult<RentalRequestResponse>
}