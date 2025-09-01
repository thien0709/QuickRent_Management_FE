package com.bxt.viewmodel

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

enum class SelectTarget { NONE, FROM, TO }

data class AddTransportUiState(
    val currentPoint: Point? = null,
    val fromPoint: Point? = null,
    val fromAddress: String = "",
    val toPoint: Point? = null,
    val toAddress: String = "",
    val departTime: Instant = Instant.now(),
    val availableSeat: String = "",
    val deliveryFee: String = "",
    val description: String = "",

    val isLoading: Boolean = false,
    val creationSuccess: Boolean = false,
    val error: String? = null,
    val selecting: SelectTarget = SelectTarget.NONE,

    // Tuyến đường Mapbox
    val routePoints: List<Point> = emptyList(),
    val isRouting: Boolean = false
)

@HiltViewModel
class AddTransportViewModel @Inject constructor(
    private val transportRepository: TransportServiceRepository,
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransportUiState())
    val uiState: StateFlow<AddTransportUiState> = _uiState.asStateFlow()

    private val http = OkHttpClient()

    // Token cho các call REST (Directions/Geocoding)
    private val mapboxToken: String by lazy {
        val id = context.resources.getIdentifier("mapbox_access_token", "string", context.packageName)
        if (id != 0) context.getString(id) else ""
    }

    /* --------- Actions cơ bản --------- */

    fun setSelecting(target: SelectTarget) = _uiState.update { it.copy(selecting = target) }

    fun setCurrentLocation(point: Point, addressOrNull: String? = null) {
        _uiState.update { it.copy(currentPoint = point) }
        if (_uiState.value.fromPoint == null) {
            viewModelScope.launch {
                val addr = addressOrNull ?: withContext(Dispatchers.IO) {
                    reverseGeocode(point)
                }
                _uiState.update { it.copy(fromPoint = point, fromAddress = addr) }
                updateRouteIfPossible()
            }
        } else {
            updateRouteIfPossible()
        }
    }

    fun onMapClicked(point: Point) {
        when (_uiState.value.selecting) {
            SelectTarget.FROM -> viewModelScope.launch {
                val addr = withContext(Dispatchers.IO) { reverseGeocode(point) }
                _uiState.update { it.copy(fromPoint = point, fromAddress = addr, selecting = SelectTarget.NONE) }
                updateRouteIfPossible()
            }
            SelectTarget.TO -> viewModelScope.launch {
                val addr = withContext(Dispatchers.IO) { reverseGeocode(point) }
                _uiState.update { it.copy(toPoint = point, toAddress = addr, selecting = SelectTarget.NONE) }
                updateRouteIfPossible()
            }
            else -> Unit
        }
    }

    // Dùng cho MapboxSearchBar
    fun setFromBySearch(point: Point, address: String) {
        _uiState.update { it.copy(fromPoint = point, fromAddress = address) }
        updateRouteIfPossible()
    }
    fun setToBySearch(point: Point, address: String) {
        _uiState.update { it.copy(toPoint = point, toAddress = address) }
        updateRouteIfPossible()
    }

    fun onFeeChanged(fee: String) { if (fee.all { it.isDigit() }) _uiState.update { it.copy(deliveryFee = fee) } }
    fun onSeatsChanged(seats: String) { if (seats.all { it.isDigit() }) _uiState.update { it.copy(availableSeat = seats) } }
    fun onDescriptionChanged(description: String) = _uiState.update { it.copy(description = description) }
    fun onTimeChanged(time: Instant) = _uiState.update { it.copy(departTime = time) }

    fun createTransport() {
        viewModelScope.launch {
            val s = _uiState.value
            val from = s.fromPoint ?: s.currentPoint
            val to = s.toPoint

            if (from == null || to == null) {
                _uiState.update { it.copy(error = "Vui lòng chọn điểm đi và điểm đến") }
                return@launch
            }
            if (s.deliveryFee.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập phí chia sẻ") }
                return@launch
            }
            if (s.availableSeat.isBlank() || s.availableSeat.toLongOrNull() == 0L) {
                _uiState.update { it.copy(error = "Vui lòng nhập số ghế trống hợp lệ") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = dataStoreManager.userId.firstOrNull()
            val req = userId?.let {
                TransportServiceRequest(
                    driverId = it,
                    fromLatitude = BigDecimal(from.latitude().toString()),
                    fromLongitude = BigDecimal(from.longitude().toString()),
                    toLatitude = BigDecimal(to.latitude().toString()),
                    toLongitude = BigDecimal(to.longitude().toString()),
                    departTime = s.departTime,
                    availableSeat = s.availableSeat.toLongOrNull(),
                    deliveryFee = BigDecimal(s.deliveryFee),
                    description = s.description.ifBlank { null }
                )
            }

            when (val result = req?.let { transportRepository.createTransportService(it) }) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, creationSuccess = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.message ?: "Đã có lỗi xảy ra") }
                null -> _uiState.update { it.copy(isLoading = false, error = "Không thể tạo yêu cầu") }
            }
        }
    }

    /* --------- Directions (Mapbox) --------- */

    private fun updateRouteIfPossible() {
        val origin = _uiState.value.fromPoint ?: _uiState.value.currentPoint
        val dest = _uiState.value.toPoint
        if (origin == null || dest == null) {
            _uiState.update { it.copy(routePoints = emptyList()) }
            return
        }
        getDirections(origin, dest)
    }

    private fun getDirections(origin: Point, dest: Point) {
        if (mapboxToken.isBlank()) {
            _uiState.update { it.copy(error = "Thiếu Mapbox access token") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRouting = true) }
            try {
                val url =
                    "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                            "${origin.longitude()},${origin.latitude()};" +
                            "${dest.longitude()},${dest.latitude()}" +
                            "?alternatives=false&geometries=geojson&language=vi&access_token=$mapboxToken"

                val req = Request.Builder().url(url).get().build()
                val resp = http.newCall(req).execute()
                val body = resp.body?.string().orEmpty()

                val json = JSONObject(body)
                val routes = json.optJSONArray("routes")
                if (routes == null || routes.length() == 0) error("Directions lỗi")

                val first = routes.getJSONObject(0)
                val coords = first.getJSONObject("geometry").getJSONArray("coordinates")

                val pts = ArrayList<Point>(coords.length())
                for (i in 0 until coords.length()) {
                    val c = coords.getJSONArray(i)
                    pts.add(Point.fromLngLat(c.getDouble(0), c.getDouble(1)))
                }
                _uiState.update { it.copy(routePoints = pts, isRouting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRouting = false, error = e.message) }
            }
        }
    }

    /* --------- Reverse geocode --------- */

    private fun reverseGeocode(p: Point): String = reverseGeocode(p.latitude(), p.longitude())

    private fun reverseGeocode(lat: Double, lng: Double): String =
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)
            }
            val addr = list?.firstOrNull()
            listOfNotNull(addr?.featureName, addr?.subLocality, addr?.locality, addr?.adminArea)
                .joinToString(", ")
                .ifBlank { "$lat, $lng" }
        } catch (_: Exception) {
            "$lat, $lng"
        }
}
