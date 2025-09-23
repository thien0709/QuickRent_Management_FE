package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.request.TransportPassengerRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.data.repository.TransportPassengerRepository
import com.bxt.di.ApiResult
import javax.inject.Inject

class TransportPassengerRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : TransportPassengerRepository {

    override suspend fun createTransportPassenger(request: TransportPassengerRequest): ApiResult<TransportPassengerResponse> {
        return apiCallExecutor.execute { apiService.createTransportPassenger(request) }
    }

    override suspend fun getTransportPassengersByServiceId(serviceId: Long): ApiResult<List<TransportPassengerResponse>> {
        return apiCallExecutor.execute { apiService.getTransportPassengersByServiceId(serviceId) }
    }

    override suspend fun getTransportPassengerByOwner(page: Int): ApiResult<PagedResponse<TransportPassengerResponse>> {
        return apiCallExecutor.execute { apiService.getTransportPassengersOwner(page) }
    }

    override suspend fun getTransportPassengerByRental(page: Int): ApiResult<PagedResponse<TransportPassengerResponse>> {
        return apiCallExecutor.execute { apiService.getTransportPassengersRental(page) }
    }

    override suspend fun getTransportPassengerById(id: Long): ApiResult<TransportPassengerResponse> {
        return apiCallExecutor.execute { apiService.getTransportPassengerById(id) }
    }

    override suspend fun bookRide(
        request: TransportPassengerRequest
    ): ApiResult<TransportPassengerResponse> {
        return apiCallExecutor.execute {
            apiService.bookRide(request)
        }
    }

    override suspend fun cancelRide(
        bookingId: Long
    ): ApiResult<Unit> {
        return apiCallExecutor.execute {
            apiService.cancelRideBooking(bookingId)
            Unit
        }
    }

}