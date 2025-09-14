// bxt/viewmodel/TransportDetailViewModel.kt
package com.bxt.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.R
import com.bxt.data.api.dto.request.TransportPackageRequest
import com.bxt.data.api.dto.request.TransportPassengerRequest
import com.bxt.data.api.dto.response.*
import com.bxt.data.repository.*
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportDetailState
import com.bxt.util.findMinimumDistanceToPolyline
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

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
    val driverRoutePoints: List<Point> = emptyList(),
    val userLocation: Point? = null,
    val dropOffPoint: Point? = null,
    val passengerRoutePoints: List<Point> = emptyList(),
    val isRouteValid: Boolean? = null
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

    // NOTE: Thêm 2 SharedFlow để gửi sự kiện một lần tới UI
    private val _actionResult = MutableSharedFlow<String>()
    val actionResult = _actionResult.asSharedFlow() // Gửi thông báo (Toast)

    private val _navigateBackEvent = MutableSharedFlow<Unit>()
    val navigateBackEvent = _navigateBackEvent.asSharedFlow() // Gửi tín hiệu điều hướng

    private val _deliverableAddresses = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val deliverableAddresses: StateFlow<Map<Long, Pair<String, String>>> = _deliverableAddresses.asStateFlow()

    private val _serviceAddresses = MutableStateFlow<Pair<String, String>?>(null)
    val serviceAddresses: StateFlow<Pair<String, String>?> = _serviceAddresses.asStateFlow()

    private val _userLocationAddress = MutableStateFlow<String?>(null)
    val userLocationAddress: StateFlow<String?> = _userLocationAddress.asStateFlow()

    private val _dropOffAddress = MutableStateFlow<String?>(null)
    val dropOffAddress: StateFlow<String?> = _dropOffAddress.asStateFlow()

    private val http = OkHttpClient()
    private val mapboxToken: String by lazy { context.getString(R.string.mapbox_access_token) }
    private var cachedDetails: FullTransportDetails? = null

    companion object {
        private const val MAX_DISTANCE_TO_ROUTE_KM = 0.5
    }

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
                val service = (transportServiceRepo.getTransportServiceById(serviceId) as? ApiResult.Success)?.data
                    ?: throw Exception("Không thể tải chi tiết chuyến đi")

                resolveServiceAddresses(service)

                val driverDeferred = async { service.driverId?.let { userRepo.getUserInfo() } }
                val passengersDeferred = async { passengerRepo.getTransportPassengersByServiceId(serviceId) }
                val packagesDeferred = async { packageRepo.getTransportPackagesByServiceId(serviceId) }
                val confirmedRentalsDeferred = async { rentalRequestRepo.getRentalRequestsByRenterOnConfirm() }
                val routeDeferred = async { fetchRoute(service.fromLatitude?.toDouble(), service.fromLongitude?.toDouble(), service.toLatitude?.toDouble(), service.toLongitude?.toDouble()) }
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

                val driverRoutePoints = routeDeferred.await()
                val userLocation = userLocationDeferred.await()
                userLocation?.let { resolveUserLocationAddress(it) }

                val fullDetails = FullTransportDetails(
                    service, driver, passengers, packagesOnService, deliverableRequests, driverRoutePoints, userLocation
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

    private fun resolveDropOffAddress(point: Point) {
        viewModelScope.launch(Dispatchers.IO) {
            _dropOffAddress.value = locationRepo.getAddressFromLatLng(point.latitude(), point.longitude()).getOrNull()
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
                    validateAndFetchPassengerRoute()
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
            validateAndFetchPassengerRoute()
        }
    }

    fun updateDropOffLocationManually(point: Point?, address: String?) {
        cachedDetails?.let {
            val targetPoint = point ?: it.dropOffPoint ?: return@let
            val updatedDetails = it.copy(dropOffPoint = targetPoint)
            cachedDetails = updatedDetails
            _uiState.value = TransportDetailState.Success(updatedDetails)
            if (address != null && address.isNotBlank()) {
                _dropOffAddress.value = address
            } else {
                resolveDropOffAddress(targetPoint)
            }
            validateAndFetchPassengerRoute()
        }
    }

    private fun validateAndFetchPassengerRoute() {
        viewModelScope.launch {
            cachedDetails?.let { details ->
                val pickup = details.userLocation
                val dropoff = details.dropOffPoint
                val driverRoute = details.driverRoutePoints

                if (pickup != null && dropoff != null && driverRoute.isNotEmpty()) {
                    val pickupDistance = findMinimumDistanceToPolyline(pickup, driverRoute)
                    val dropoffDistance = findMinimumDistanceToPolyline(dropoff, driverRoute)
                    val isRouteValid = pickupDistance <= MAX_DISTANCE_TO_ROUTE_KM && dropoffDistance <= MAX_DISTANCE_TO_ROUTE_KM
                    val passengerRoute = fetchRoute(pickup.latitude(), pickup.longitude(), dropoff.latitude(), dropoff.longitude())
                    val updatedDetails = details.copy(passengerRoutePoints = passengerRoute, isRouteValid = isRouteValid)
                    cachedDetails = updatedDetails
                    _uiState.value = TransportDetailState.Success(updatedDetails)
                } else {
                    val updatedDetails = details.copy(passengerRoutePoints = emptyList(), isRouteValid = null)
                    cachedDetails = updatedDetails
                    _uiState.value = TransportDetailState.Success(updatedDetails)
                }
            }
        }
    }

    private suspend fun getCurrentUserLocation(): Point? {
        return locationRepo.getCurrentLocation().getOrNull()?.let { (lat, lng) ->
            Point.fromLngLat(lng, lat)
        }
    }

    private suspend fun fetchRoute(fromLat: Double?, fromLng: Double?, toLat: Double?, toLng: Double?): List<Point> {
        val origin = fromLng?.let { lng -> fromLat?.let { lat -> "$lng,$lat" } } ?: return emptyList()
        val dest = toLng?.let { lng -> toLat?.let { lat -> "$lng,$lat" } } ?: return emptyList()
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
                            val updatedDetails = details.copy(driverRoutePoints = deliveryRoutePoints)
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
                    val originalRoute = fetchRoute(details.service.fromLatitude?.toDouble(), details.service.fromLongitude?.toDouble(), details.service.toLatitude?.toDouble(), details.service.toLongitude?.toDouble())
                    val updatedDetails = details.copy(driverRoutePoints = originalRoute)
                    cachedDetails = updatedDetails
                    _uiState.value = TransportDetailState.Success(updatedDetails)
                } catch (e: Exception) {
                    _uiState.value = TransportDetailState.Success(details)
                }
            }
        }
    }

    fun bookRide() {
        viewModelScope.launch {
            val currentState = (_uiState.value as? TransportDetailState.Success)?.details ?: return@launch
            if (currentState.isRouteValid != true) {
                _actionResult.emit("Điểm đón hoặc điểm đến của bạn quá xa lộ trình của tài xế.")
                return@launch
            }

            val pickupPoint = currentState.userLocation!!
            val dropOffPoint = currentState.dropOffPoint!!
            val currentUser = (userRepo.getUserInfo() as? ApiResult.Success)?.data!!

            val request = currentUser.id?.let {
                TransportPassengerRequest(
                    transportServiceId = serviceId,
                    userId = it,
                    pickupLatitude = pickupPoint.latitude().toBigDecimal(),
                    pickupLongitude = pickupPoint.longitude().toBigDecimal(),
                    dropoffLatitude = dropOffPoint.latitude().toBigDecimal(),
                    dropoffLongitude = dropOffPoint.longitude().toBigDecimal()
                )
            }

            when (val result = request?.let { passengerRepo.createTransportPassenger(it) }) {
                is ApiResult.Success -> {
                    _actionResult.emit("Đặt chỗ thành công!")
                    _navigateBackEvent.emit(Unit)
                }
                is ApiResult.Error -> {
                    _actionResult.emit(result.error.message ?: "Đặt chỗ thất bại, vui lòng thử lại.")
                }
                null -> _actionResult.emit("Không thể tạo yêu cầu đặt chỗ.")
            }
        }
    }

    fun acceptPackageDelivery(selectedRequest: DeliverableRequest) {
        viewModelScope.launch {
            val rentalRequestId = selectedRequest.request.id
            if (rentalRequestId == null) {
                _actionResult.emit("Yêu cầu thuê không hợp lệ.")
                return@launch
            }

            val senderId = selectedRequest.request.renterId
            if (senderId == null) {
                _actionResult.emit("Không tìm thấy thông tin người gửi.")
                return@launch
            }

            val receiptId = selectedRequest.item.ownerId
            if (receiptId == null) {
                _actionResult.emit("Không tìm thấy thông tin người nhận.")
                return@launch
            }

            val request = TransportPackageRequest(
                transportServiceId = serviceId,
                senderId = senderId,
                receiptId = receiptId,
                fromLatitude = selectedRequest.request.latFrom,
                fromLongitude = selectedRequest.request.lngFrom,
                toLatitude = selectedRequest.request.latTo,
                toLongitude = selectedRequest.request.lngTo,
                packageDescription = "Vận chuyển: ${selectedRequest.item.title}"
            )

            when (val result = packageRepo.createTransportPackage(request)) {
                is ApiResult.Success -> {
                    _actionResult.emit("Nhận vận chuyển thành công!")
                    _navigateBackEvent.emit(Unit)
                }
                is ApiResult.Error -> {
                    _actionResult.emit(result.error.message ?: "Nhận vận chuyển thất bại, vui lòng thử lại.")
                }
            }
        }
    }
}