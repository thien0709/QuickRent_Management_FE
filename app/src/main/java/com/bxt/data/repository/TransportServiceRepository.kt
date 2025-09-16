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
    suspend fun getTransportServicesByDriver(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>>
    suspend fun getTransportServicesByParticipant(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>>
    suspend fun getTransportServicesBySender(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>>
    suspend fun getTransportServicesByReceiver(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>>
    suspend fun getTransportServicesAsPassenger(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>>
}
