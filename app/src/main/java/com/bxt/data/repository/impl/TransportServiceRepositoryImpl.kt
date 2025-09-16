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

    override suspend fun getTransportServicesByDriver(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServicesByDriver(currentPage)
        }
    }

    override suspend fun getTransportServicesByParticipant(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServicesByParticipant(currentPage)
        }
    }

    // Phương thức mới để lấy danh sách chuyến đi mà người dùng là người gửi
    override suspend fun getTransportServicesBySender(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServicesBySender(currentPage)
        }
    }

    // Phương thức mới để lấy danh sách chuyến đi mà người dùng là người nhận
    override suspend fun getTransportServicesByReceiver(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServicesByReceiver(currentPage)
        }
    }

    // Phương thức mới để lấy danh sách chuyến đi mà người dùng là hành khách
    override suspend fun getTransportServicesAsPassenger(currentPage: Int): ApiResult<PagedResponse<TransportServiceResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportServicesAsPassenger(currentPage)
        }
    }
}