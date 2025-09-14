// bxt/viewmodel/TransportDetailViewModel.kt
package com.bxt.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.R
import com.bxt.data.api.dto.response.*
import com.bxt.data.repository.*
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportDetailState
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

// Các data class để gom nhóm dữ liệu
enum class TransportMode { RIDE, PACKAGE }

data class DeliverableRequest(
    val request: RentalRequestResponse,
    val item: ItemResponse
)

data class FullTransportDetails(
    val service: TransportServiceResponse,
    val driver: UserResponse?,
    val passengers: List<TransportPassengerResponse>,
    val packagesOnService: List<TransportPackageResponse>,
    val deliverableRequests: List<DeliverableRequest> = emptyList(),
    val routePoints: List<Point> = emptyList(),
    val userLocation: Point? = null
)

@HiltViewModel
class TransportDetailViewModel @Inject constructor(
    private val transportServiceRepo: TransportServiceRepository,
    private val passengerRepo: TransportPassengerRepository,
    private val packageRepo: TransportPackageRepository,
    private val userRepo: UserRepository,
    private val rentalRequestRepo: RentalRequestRepository,
    private val itemRepo: ItemRepository,
    private val locationRepo: LocationRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serviceId: Long = savedStateHandle.get<Long>("serviceId") ?: 0L

    private val _uiState = MutableStateFlow<TransportDetailState>(TransportDetailState.Loading)
    val uiState: StateFlow<TransportDetailState> = _uiState.asStateFlow()

    private val _deliverableAddresses = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val deliverableAddresses: StateFlow<Map<Long, Pair<String, String>>> = _deliverableAddresses.asStateFlow()

    private val _serviceAddresses = MutableStateFlow<Pair<String, String>?>(null)
    val serviceAddresses: StateFlow<Pair<String, String>?> = _serviceAddresses.asStateFlow()

    private val _userLocationAddress = MutableStateFlow<String?>(null)
    val userLocationAddress: StateFlow<String?> = _userLocationAddress.asStateFlow()

    private val http = OkHttpClient()
    private val mapboxToken: String by lazy { context.getString(R.string.mapbox_access_token) }
    private var cachedDetails: FullTransportDetails? = null

    init {
        if (serviceId > 0) {
            loadAllDetails()
        } else {
            _uiState.value = TransportDetailState.Error("ID của chuyến đi không hợp lệ")
        }
    }

    fun loadAllDetails() {
        viewModelScope.launch {
            _uiState.value = TransportDetailState.Loading
            try {
                val serviceResult = transportServiceRepo.getTransportServiceById(serviceId)
                if (serviceResult !is ApiResult.Success) {
                    _uiState.value = TransportDetailState.Error("Không thể tải chi tiết chuyến đi")
                    return@launch
                }
                val service = serviceResult.data

                resolveServiceAddresses(service)

                val driverDeferred = async { service.driverId?.let { userRepo.getUserInfo() } }
                val passengersDeferred = async { passengerRepo.getTransportPassengersByServiceId(serviceId) }
                val packagesDeferred = async { packageRepo.getTransportPackagesByServiceId(serviceId) }
                val confirmedRentalsDeferred = async { rentalRequestRepo.getRentalRequestsByRenterOnConfirm() }
                val routeDeferred = async { fetchRoute(service) }
                val userLocationDeferred = async { getCurrentUserLocation() }

                val driver = (driverDeferred.await() as? ApiResult.Success)?.data
                val passengers = (passengersDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
                val packagesOnService = (packagesDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
                val confirmedRentals = (confirmedRentalsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()

                resolveAddressesForDeliverables(confirmedRentals)

                val deliverableRequests = confirmedRentals.mapNotNull { rental ->
                    rental.itemId?.let { itemId ->
                        val itemResult = itemRepo.getItemInfo(itemId)
                        if (itemResult is ApiResult.Success) DeliverableRequest(request = rental, item = itemResult.data) else null
                    }
                }

                val routePoints = routeDeferred.await()
                val userLocation = userLocationDeferred.await()
                userLocation?.let { resolveUserLocationAddress(it) }

                val fullDetails = FullTransportDetails(
                    service, driver, passengers, packagesOnService, deliverableRequests, routePoints, userLocation
                )
                cachedDetails = fullDetails
                _uiState.value = TransportDetailState.Success(fullDetails)
            } catch (e: Exception) {
                _uiState.value = TransportDetailState.Error("Đã xảy ra lỗi: ${e.message}")
            }
        }
    }

    private fun resolveServiceAddresses(service: TransportServiceResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            val from = service.fromLatitude?.toDouble()?.let { lat ->
                service.fromLongitude?.toDouble()?.let { lng -> locationRepo.getAddressFromLatLng(lat, lng).getOrNull() }
            } ?: "Không rõ điểm đi"
            val to = service.toLatitude?.toDouble()?.let { lat ->
                service.toLongitude?.toDouble()?.let { lng -> locationRepo.getAddressFromLatLng(lat, lng).getOrNull() }
            } ?: "Không rõ điểm đến"
            _serviceAddresses.value = from to to
        }
    }

    private fun resolveAddressesForDeliverables(requests: List<RentalRequestResponse>) {
        viewModelScope.launch(Dispatchers.IO) {
            val addressMap = mutableMapOf<Long, Pair<String, String>>()
            requests.forEach { request ->
                request.id?.let { id ->
                    val from = request.latFrom?.toDouble()?.let { lat -> request.lngFrom?.toDouble()?.let { lng -> locationRepo.getAddressFromLatLng(lat, lng).getOrNull() } } ?: "Không rõ"
                    val to = request.latTo?.toDouble()?.let { lat -> request.lngTo?.toDouble()?.let { lng -> locationRepo.getAddressFromLatLng(lat, lng).getOrNull() } } ?: "Không rõ"
                    addressMap[id] = from to to
                }
            }
            _deliverableAddresses.value = addressMap
        }
    }

    private fun resolveUserLocationAddress(point: Point) {
        viewModelScope.launch(Dispatchers.IO) {
            _userLocationAddress.value = locationRepo.getAddressFromLatLng(point.latitude(), point.longitude()).getOrNull()
        }
    }

    fun updateUserLocation() {
        viewModelScope.launch {
            cachedDetails?.let { details ->
                try {
                    val newUserLocation = getCurrentUserLocation()
                    val updatedDetails = details.copy(userLocation = newUserLocation)
                    newUserLocation?.let { resolveUserLocationAddress(it) }
                    cachedDetails = updatedDetails
                    _uiState.value = TransportDetailState.Success(updatedDetails)
                } catch (e: Exception) {
                    _uiState.value = TransportDetailState.Success(details)
                }
            } ?: loadAllDetails()
        }
    }

    fun updateUserLocationManually(point: Point?, address: String?) {
        cachedDetails?.let {
            val targetPoint = point ?: it.userLocation ?: return@let
            val updatedDetails = it.copy(userLocation = targetPoint)
            cachedDetails = updatedDetails
            _uiState.value = TransportDetailState.Success(updatedDetails)
            if (address != null && address.isNotBlank()) {
                _userLocationAddress.value = address
            } else {
                resolveUserLocationAddress(targetPoint)
            }
        }
    }

    private suspend fun getCurrentUserLocation(): Point? {
        return locationRepo.getCurrentLocation().getOrNull()?.let { (lat, lng) ->
            Point.fromLngLat(lng, lat)
        }
    }

    private suspend fun fetchRoute(service: TransportServiceResponse): List<Point> {
        val origin = service.fromLatitude?.toDouble()?.let { lat -> service.fromLongitude?.toDouble()?.let { lng -> "$lng,$lat" } } ?: return emptyList()
        val dest = service.toLatitude?.toDouble()?.let { lat -> service.toLongitude?.toDouble()?.let { lng -> "$lng,$lat" } } ?: return emptyList()
        if (mapboxToken.isBlank()) return emptyList()

        return try {
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$origin;$dest?alternatives=false&geometries=geojson&language=vi&access_token=$mapboxToken"
            val request = Request.Builder().url(url).build()
            val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
            response.use {
                it.body?.string()?.takeIf(String::isNotBlank)?.let { body ->
                    val routes = JSONObject(body).optJSONArray("routes")?.takeIf { r -> r.length() > 0 } ?: return emptyList()
                    val coordinates = routes.getJSONObject(0).optJSONObject("geometry")?.optJSONArray("coordinates") ?: return emptyList()
                    (0 until coordinates.length()).map { i ->
                        val coord = coordinates.getJSONArray(i)
                        Point.fromLngLat(coord.getDouble(0), coord.getDouble(1))
                    }
                } ?: emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    fun calculateDeliveryRoute(selectedRequest: DeliverableRequest?) {
        if (selectedRequest == null) return
        viewModelScope.launch {
            cachedDetails?.let { details ->
                try {
                    val waypoints = listOfNotNull(
                        details.service.fromLongitude?.toDouble() to details.service.fromLatitude?.toDouble(),
                        selectedRequest.request.lngFrom?.toDouble() to selectedRequest.request.latFrom?.toDouble(),
                        selectedRequest.request.lngTo?.toDouble() to selectedRequest.request.latTo?.toDouble(),
                        details.service.toLongitude?.toDouble() to details.service.toLatitude?.toDouble()
                    ).joinToString(";") { (lng, lat) -> "$lng,$lat" }

                    if (waypoints.split(";").size < 4 || mapboxToken.isBlank()) return@launch

                    val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$waypoints?alternatives=false&geometries=geojson&language=vi&access_token=$mapboxToken"
                    val request = Request.Builder().url(url).build()
                    val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
                    response.use { resp ->
                        resp.body?.string()?.takeIf { it.isNotBlank() }?.let { body ->
                            val json = JSONObject(body)
                            val routes = json.optJSONArray("routes")?.takeIf { it.length() > 0 } ?: return@let
                            val geometry = routes.getJSONObject(0).optJSONObject("geometry")
                            val coordinates = geometry?.optJSONArray("coordinates") ?: return@let
                            val deliveryRoutePoints = (0 until coordinates.length()).map { i ->
                                val coord = coordinates.getJSONArray(i)
                                Point.fromLngLat(coord.getDouble(0), coord.getDouble(1))
                            }
                            val updatedDetails = details.copy(routePoints = deliveryRoutePoints)
                            cachedDetails = updatedDetails
                            _uiState.value = TransportDetailState.Success(updatedDetails)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = TransportDetailState.Success(details)
                }
            }
        }
    }

    fun resetToOriginalRoute() {
        viewModelScope.launch {
            cachedDetails?.let { details ->
                try {
                    val originalRoute = fetchRoute(details.service)
                    val updatedDetails = details.copy(routePoints = originalRoute)
                    cachedDetails = updatedDetails
                    _uiState.value = TransportDetailState.Success(updatedDetails)
                } catch (e: Exception) {
                    _uiState.value = TransportDetailState.Success(details)
                }
            }
        }
    }
}