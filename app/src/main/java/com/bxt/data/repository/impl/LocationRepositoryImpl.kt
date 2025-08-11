package com.bxt.data.repository.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.bxt.data.api.ApiService
import com.bxt.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

class LocationRepositoryImpl(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder = Geocoder(context, Locale.getDefault())

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override suspend fun getCurrentLocation(): Result<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Location permission not granted"))
        }
        try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(Result.success(location.latitude to location.longitude))
                        } else {
                            continuation.resume(Result.failure(Exception("Location is null")))
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }


    override suspend fun getAddressFromLatLng(lat: Double, lng: Double): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addressLine = addresses[0].getAddressLine(0) ?: "Unknown location"
                Result.success(addressLine)
            } else {
                Result.failure(Exception("No address found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setLocationUser(userId  : Long , lat: Double, lng: Double): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val locationMap = mapOf(
                "lat" to lat,
                "lng" to lng
            )
            val response = apiService.updateLocation(userId,locationMap)

            if (response) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
