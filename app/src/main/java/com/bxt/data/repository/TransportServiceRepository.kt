package com.bxt.data.repository

import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.di.ApiResult

interface TransportServiceRepository {
    suspend fun getTransportServices(): ApiResult<List<TransportServiceResponse>>
    suspend fun getTransportServiceById(id: Long): ApiResult<TransportServiceResponse>
    suspend fun createTransportService(request: TransportServiceRequest): ApiResult<TransportServiceResponse>
    suspend fun updateTransportService(id: Long, request: TransportServiceRequest): ApiResult<TransportServiceResponse>
    suspend fun deleteTransportService(id: Long): ApiResult<Unit>
    suspend fun updateServiceStatus(serviceId: Long, newStatus: String): ApiResult<TransportServiceResponse>

    suspend fun confirmTransportService(id: Long): ApiResult<TransportServiceResponse>
    suspend fun startTransportService(id: Long): ApiResult<TransportServiceResponse>
    suspend fun completeTransportService(id: Long): ApiResult<TransportServiceResponse>
    suspend fun cancelTransportService(id: Long, reason: String? = null): ApiResult<TransportServiceResponse>

}
