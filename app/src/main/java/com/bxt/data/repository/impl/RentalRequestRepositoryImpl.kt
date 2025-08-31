package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult

class RentalRequestRepositoryImpl(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : RentalRequestRepository {

    override suspend fun getRentalRequestById(id: Long): ApiResult<RentalRequestResponse> {
        return apiCallExecutor.execute {
            apiService.getRentalRequestById(id)
        }
    }

    override suspend fun createRentalRequest(request: RentalRequestRequest): ApiResult<RentalRequestResponse> {
        return apiCallExecutor.execute {
            apiService.createRentalRequest(request)
        }
    }

    override suspend fun updateRentalRequest(
        id: Long,
        request: RentalRequestRequest
    ): ApiResult<RentalRequestResponse> {
        return apiCallExecutor.execute {
            apiService.updateRentalRequest(id, request)
        }
    }

    override suspend fun deleteRentalRequest(id: Long): ApiResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getRentalRequestsByRenter(page: Int): ApiResult<PagedResponse<RentalRequestResponse>> {
        return apiCallExecutor.execute {
            apiService.getRentalRequestsByRenter(page = page)
        }
    }

    override suspend fun getRentalRequestsByOwner(page: Int): ApiResult<PagedResponse<RentalRequestResponse>> {
        return apiCallExecutor.execute {
            apiService.getRentalRequestsByOwner(page = page)
        }
    }

    override suspend fun updateRequestStatus(
        requestId: Long,
        newStatus: String
    ): ApiResult<RentalRequestResponse> {
        return apiCallExecutor.execute {
            apiService.updateRequestStatus(requestId, newStatus)
        }
    }
}