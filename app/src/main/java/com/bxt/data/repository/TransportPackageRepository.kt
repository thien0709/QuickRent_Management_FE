package com.bxt.data.repository

import com.bxt.data.api.dto.request.TransportPackageRequest
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.di.ApiResult

interface TransportPackageRepository {
    suspend fun createTransportPackage(request: TransportPackageRequest): ApiResult<TransportPackageResponse>
    suspend fun getTransportPackagesByServiceId(serviceId: Long): ApiResult<List<TransportPackageResponse>>
}