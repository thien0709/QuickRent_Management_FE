
package com.bxt.data.repository

import com.bxt.data.api.dto.request.TransportPassengerRequest
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.di.ApiResult

interface TransportPassengerRepository {
    suspend fun createTransportPassenger(request: TransportPassengerRequest): ApiResult<TransportPassengerResponse>
    suspend fun getTransportPassengersByServiceId(serviceId: Long): ApiResult<List<TransportPassengerResponse>>
}