package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult

class TransportServiceRepositoryImpl(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : TransportServiceRepository {
    override suspend fun getTransportServices(): ApiResult<List<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServices()
        }
    }

    override suspend fun getTransportServiceById(id: Long): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.getTransportServiceById(id)
        }
    }

    override suspend fun createTransportService(request: TransportServiceRequest): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.createTransportService(request)
        }
    }

    override suspend fun updateTransportService(
        id: Long,
        request: TransportServiceRequest
    ): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.updateTransportService(id, request)
        }
    }

    override suspend fun deleteTransportService(id: Long): ApiResult<Unit> {
        return apiCallExecutor.execute {
            apiService.deleteTransportService(id)
        }
    }

    override suspend fun updateServiceStatus(
        serviceId: Long,
        newStatus: String
    ): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.updateServiceStatus(serviceId, newStatus)
        }
    }

    override suspend fun confirmTransportService(id: Long): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.confirmTransportService(id)
        }
    }

    override suspend fun startTransportService(id: Long): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.startTransportService(id)
        }
    }

    override suspend fun completeTransportService(id: Long): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.completeTransportService(id)
        }
    }

    override suspend fun cancelTransportService(
        id: Long,
        reason: String?
    ): ApiResult<TransportServiceResponse> {
        return apiCallExecutor.execute {
            apiService.cancelTransportService(id, reason)
        }
    }
}