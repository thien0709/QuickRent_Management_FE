package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    data class LocationState(
        val isLoading: Boolean = false,
        val currentAddress: String? = null,
        val error: String? = null,
        val permissionNeeded: Boolean = false,
        val gpsNeeded: Boolean = false
    )

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    fun updateCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = LocationState(isLoading = true)

            val result = try {
                locationRepository.getCurrentLocation()
            } catch (e: Exception) {
                Result.failure<Pair<Double, Double>>(e)
            }

            result.fold(
                onSuccess = { (lat, lng) ->
                    val userId = try {
                        dataStoreManager.userId.firstOrNull()
                    } catch (e: Exception) {
                        null
                    }

                    if (userId != null) {
                        // Nếu có userId thì cập nhật lên server
                        try {
                            locationRepository.setLocationUser(userId, lat, lng)
                        } catch (e: Exception) {
                            // Có thể log lỗi nhưng không cần trả về lỗi cho UI
                        }
                    }

                    val addressResult = locationRepository.getAddressFromLatLng(lat, lng)
                    addressResult.fold(
                        onSuccess = { address ->
                            _locationState.value = LocationState(
                                isLoading = false,
                                currentAddress = address,
                                error = null,
                                permissionNeeded = false,
                                gpsNeeded = false
                            )
                        },
                        onFailure = { e ->
                            _locationState.value = LocationState(
                                isLoading = false,
                                error = "Lỗi lấy địa chỉ: ${e.message}",
                                permissionNeeded = false,
                                gpsNeeded = false
                            )
                        }
                    )
                },
                onFailure = { e ->
                    when (e) {
                        is SecurityException -> _locationState.value = LocationState(
                            isLoading = false,
                            error = null,
                            permissionNeeded = true,
                            gpsNeeded = false
                        )
                        else -> {
                            if (e.message == "GPS not enabled") {
                                _locationState.value = LocationState(
                                    isLoading = false,
                                    error = null,
                                    permissionNeeded = false,
                                    gpsNeeded = true
                                )
                            } else {
                                _locationState.value = LocationState(
                                    isLoading = false,
                                    error = "Lỗi lấy vị trí: ${e.message}",
                                    permissionNeeded = false,
                                    gpsNeeded = false
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    fun setError(message: String) {
        _locationState.value = LocationState(error = message)
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            updateCurrentLocation()
        } else {
            _locationState.value = LocationState(
                isLoading = false,
                error = "Cần cấp quyền truy cập vị trí để sử dụng tính năng này",
                permissionNeeded = false,
                gpsNeeded = false
            )
        }
    }
}
