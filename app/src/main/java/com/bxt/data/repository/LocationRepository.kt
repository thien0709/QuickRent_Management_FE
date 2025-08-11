package com.bxt.data.repository

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>>
    suspend fun getAddressFromLatLng(lat: Double, lng: Double): Result<String>
    suspend fun setLocationUser(userId  : Long , lat: Double, lng: Double): Result<Unit>
}
