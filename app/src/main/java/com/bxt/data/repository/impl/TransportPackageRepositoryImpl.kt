
package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.TransportPackageRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.repository.TransportPackageRepository
import com.bxt.di.ApiResult
import javax.inject.Inject

class TransportPackageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : TransportPackageRepository {

    override suspend fun createTransportPackage(request: TransportPackageRequest): ApiResult<TransportPackageResponse> {
        return apiCallExecutor.execute {
            apiService.createTransportPackage(request)
        }
    }




    override suspend fun getTransportPackagesByServiceId(serviceId: Long): ApiResult<List<TransportPackageResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportPackagesByServiceId(serviceId)
        }
    }

    override suspend fun getTransportPackageByOwner(page: Int): ApiResult<PagedResponse<TransportPackageResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportPackagesOwner(page)
        }
    }

    override suspend fun getTransportPackageByRental(page: Int): ApiResult<PagedResponse<TransportPackageResponse>> {
        return apiCallExecutor.execute {
            apiService.getTransportPackagesRental(page)
        }
    }

    override suspend fun getTransportPackageById(id: Long): ApiResult<TransportPackageResponse> {
        return apiCallExecutor.execute {
            apiService.getTransportPackageById(id)
        }
    }

    override suspend fun requestPackageDelivery(
        request: TransportPackageRequest
    ): ApiResult<TransportPackageResponse> {
        return apiCallExecutor.execute {
            apiService.requestPackageDelivery(request)
        }
    }

    override suspend fun cancelPackageRequest(
        packageId: Long
    ): ApiResult<Unit> {
        return apiCallExecutor.execute {
            apiService.cancelPackageDelivery(packageId)
            Unit
        }
    }
}