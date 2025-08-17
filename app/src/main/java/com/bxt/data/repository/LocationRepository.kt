package com.bxt.data.repository

import com.bxt.di.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>>
    suspend fun getAddressFromLatLng(lat: Double, lng: Double): Result<String>
    suspend fun setLocationUser(lat: Double, lng: Double): ApiResult<Unit>
}
