package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult

class RentalRequestRepositoryImpl(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : RentalRequestRepository {


    // Other methods can be added as needed
    override suspend fun getRentalRequests(): ApiResult<List<RentalRequestResponse>> {
        TODO("Not yet implemented")
    }

    override suspend fun getRentalRequestById(id: String): ApiResult<RentalRequestResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun createRentalRequest(request: RentalRequestRequest): ApiResult<RentalRequestResponse> {
        return apiCallExecutor.execute {
            apiService.createRentalRequest(request)
        }
    }

    override suspend fun updateRentalRequest(
        id: String,
        request: RentalRequestRequest
    ): ApiResult<RentalRequestResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRentalRequest(id: String): ApiResult<Unit> {
        TODO("Not yet implemented")
    }
}