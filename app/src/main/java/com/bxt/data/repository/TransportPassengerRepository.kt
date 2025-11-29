
package com.bxt.data.repository

import com.bxt.data.api.dto.request.TransportPassengerRequest
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.di.ApiResult

interface TransportPassengerRepository {
    suspend fun createTransportPassenger(request: TransportPassengerRequest): ApiResult<TransportPassengerResponse>
    suspend fun getTransportPassengersByServiceId(serviceId: Long): ApiResult<List<TransportPassengerResponse>>
    suspend fun getTransportPassengerByOwner(page: Int): ApiResult<PagedResponse<TransportPassengerResponse>>
    suspend fun getTransportPassengerByRental(page: Int): ApiResult<PagedResponse<TransportPassengerResponse>>
    suspend fun getTransportPassengerById(id: Long): ApiResult<TransportPassengerResponse>

    suspend fun bookRide(request: TransportPassengerRequest): ApiResult<TransportPassengerResponse>
    suspend fun cancelRide(bookingId: Long): ApiResult<Unit>


}