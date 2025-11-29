package com.bxt.data.repository

import com.bxt.data.api.dto.request.TransportPackageRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.di.ApiResult

interface TransportPackageRepository {
    suspend fun createTransportPackage(request: TransportPackageRequest): ApiResult<TransportPackageResponse>
    suspend fun getTransportPackagesByServiceId(serviceId: Long): ApiResult<List<TransportPackageResponse>>
    suspend fun getTransportPackageByOwner(page: Int): ApiResult<PagedResponse<TransportPackageResponse>>
    suspend fun getTransportPackageByRental(page: Int): ApiResult<PagedResponse<TransportPackageResponse>>
    suspend fun getTransportPackageById(id: Long): ApiResult<TransportPackageResponse>

    suspend fun requestPackageDelivery(request: TransportPackageRequest): ApiResult<TransportPackageResponse>
    suspend fun cancelPackageRequest(packageId: Long): ApiResult<Unit>
}