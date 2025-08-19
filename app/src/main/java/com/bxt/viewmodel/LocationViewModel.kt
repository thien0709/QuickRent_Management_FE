package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.LocationRepository
import com.bxt.ui.state.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        fetchCurrentLocation()
        observeLocationChanges()
    }



    private fun observeLocationChanges() {
        viewModelScope.launch {
            combine(
                dataStoreManager.userId,
                dataStoreManager.pendingLocation,
                dataStoreManager.isLoggedIn
            ) { userId, pendingLocation, isLoggedIn ->
                Triple(userId, pendingLocation, isLoggedIn)
            }.collect { (userId, pendingLocation, isLoggedIn) ->
                if (isLoggedIn && userId != null && pendingLocation != null) {
                    uploadLocationToServer(pendingLocation.first, pendingLocation.second)
                }
            }
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = LocationState.Loading
            val result = locationRepository.getCurrentLocation()
            result.onSuccess { (lat, lng) ->
                onNewLocation(lat, lng)
            }.onFailure { e ->
                _locationState.value = LocationState.Error(
                    message = e.localizedMessage ?: "Unable to fetch location",
                    location = Pair(0.0, 0.0)
                )
            }
        }
    }

    fun onNewLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            _locationState.value = LocationState.Success(location = Pair(lat, lng))
            dataStoreManager.saveCurrentLocation(lat, lng)
            dataStoreManager.savePendingLocation(lat, lng)
            fetchAddress(lat, lng)
        }
    }

    private suspend fun fetchAddress(lat: Double, lng: Double) {
        val address = locationRepository.getAddressFromLatLng(lat, lng).getOrNull()
        if (address != null) {
            dataStoreManager.saveCurrentLocation(lat, lng, address)
            _locationState.value = LocationState.Success(
                location = Pair(lat, lng),
                address = address
            )
        } else {
            _locationState.value = LocationState.Error(
                message = "Could not fetch address",
                location = Pair(lat, lng)
            )
        }
    }

    private suspend fun uploadLocationToServer(lat: Double, lng: Double) {
        if (_isUploading.value) return
        _isUploading.value = true
        try {
            locationRepository.setLocationUser( lat, lng)
            dataStoreManager.clearPendingLocation()
            fetchAddress(lat, lng)
        } catch (e: Exception) {
            _locationState.value = LocationState.Error(
                message = "Upload failed: ${e.localizedMessage}",
                location = Pair(lat, lng)
            )
        } finally {
            _isUploading.value = false
        }
    }

    fun retryUpload() {
        viewModelScope.launch {
            val userId = dataStoreManager.userId.firstOrNull()
            val pendingLocation = dataStoreManager.pendingLocation.firstOrNull()
            if (userId != null && pendingLocation != null) {
                uploadLocationToServer(pendingLocation.first, pendingLocation.second)
            }
        }
    }

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            fetchCurrentLocation()
        } else {
            _locationState.value = LocationState.PermissionRequired(shouldShowRationale)
        }
    }

    fun onGpsStatusChanged(enabled: Boolean) {
        if (enabled) {
            fetchCurrentLocation()
        } else {
            _locationState.value = LocationState.GpsDisabled(false)
        }
    }
}
