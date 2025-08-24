// file: com/bxt/viewmodel/AddTransportViewModel.kt
package com.bxt.viewmodel

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.TransportServiceRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
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
import kotlin.coroutines.resumeWithException

enum class SelectTarget { NONE, FROM, TO }

data class PredictionItem(
    val placeId: String,
    val primary: String,
    val secondary: String
)

data class AddTransportUiState(
    val currentLatLng: LatLng? = null,
    val fromLatLng: LatLng? = null,
    val fromAddress: String = "",
    val toLatLng: LatLng? = null,
    val toAddress: String = "",
    val departTime: Instant = Instant.now(),
    val availableSeat: String = "",
    val deliveryFee: String = "",
    val description: String = "",

    val isLoading: Boolean = false,
    val creationSuccess: Boolean = false,
    val error: String? = null,

    val selecting: SelectTarget = SelectTarget.NONE,

    // Autocomplete
    val isSearchingFrom: Boolean = false,
    val isSearchingTo: Boolean = false,
    val fromPredictions: List<PredictionItem> = emptyList(),
    val toPredictions: List<PredictionItem> = emptyList(),

    // Directions polyline points
    val routePoints: List<LatLng> = emptyList(),
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

    private val client = OkHttpClient()

    private val apiKey: String by lazy {
        // Lấy API key từ meta-data "com.google.android.geo.API_KEY"
        val ai = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        ai.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }

    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey, Locale("vi", "VN"))
        }
        Places.createClient(context)
    }

    fun setSelecting(target: SelectTarget) {
        _uiState.update { it.copy(selecting = target) }
    }

    fun setCurrentLocation(latLng: LatLng, addressOrNull: String? = null) {
        _uiState.update { it.copy(currentLatLng = latLng) }
        // Nếu chưa có điểm đi, gán mặc định FROM = vị trí hiện tại
        if (_uiState.value.fromLatLng == null) {
            val addr = addressOrNull ?: reverseGeocode(latLng)
            _uiState.update { it.copy(fromLatLng = latLng, fromAddress = addr) }
        }
        updateRouteIfPossible()
    }

    fun onMapClicked(latLng: LatLng) {
        when (_uiState.value.selecting) {
            SelectTarget.FROM -> {
                val addr = reverseGeocode(latLng)
                _uiState.update { it.copy(fromLatLng = latLng, fromAddress = addr) }
            }
            SelectTarget.TO -> {
                val addr = reverseGeocode(latLng)
                _uiState.update { it.copy(toLatLng = latLng, toAddress = addr) }
            }
            else -> return
        }
        updateRouteIfPossible()
    }

    fun onFeeChanged(fee: String) {
        if (fee.all { it.isDigit() }) {
            _uiState.update { it.copy(deliveryFee = fee) }
        }
    }

    fun onSeatsChanged(seats: String) {
        if (seats.all { it.isDigit() }) {
            _uiState.update { it.copy(availableSeat = seats) }
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onTimeChanged(time: Instant) {
        _uiState.update { it.copy(departTime = time) }
    }

    fun searchPlaces(query: String, target: SelectTarget) {
        if (query.length < 3) {
            when (target) {
                SelectTarget.FROM -> _uiState.update { it.copy(fromPredictions = emptyList(), isSearchingFrom = false, fromAddress = query) }
                SelectTarget.TO -> _uiState.update { it.copy(toPredictions = emptyList(), isSearchingTo = false, toAddress = query) }
                else -> {}
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (target) {
                SelectTarget.FROM -> _uiState.update { it.copy(isSearchingFrom = true, fromAddress = query) }
                SelectTarget.TO -> _uiState.update { it.copy(isSearchingTo = true, toAddress = query) }
                else -> {}
            }

            val req = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setTypeFilter(TypeFilter.ADDRESS)
                .setCountries(listOf("VN")) // ưu tiên VN
                .build()

            try {
                val resp = placesClient.findAutocompletePredictions(req).await()
                val items = resp.autocompletePredictions.map { it.toItem() }
                when (target) {
                    SelectTarget.FROM -> _uiState.update { it.copy(fromPredictions = items, isSearchingFrom = false) }
                    SelectTarget.TO -> _uiState.update { it.copy(toPredictions = items, isSearchingTo = false) }
                    else -> {}
                }
            } catch (e: Exception) {
                when (target) {
                    SelectTarget.FROM -> _uiState.update { it.copy(isSearchingFrom = false, error = e.message) }
                    SelectTarget.TO -> _uiState.update { it.copy(isSearchingTo = false, error = e.message) }
                    else -> {}
                }
            }
        }
    }

    fun choosePrediction(prediction: PredictionItem, target: SelectTarget) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = FetchPlaceRequest.newInstance(
                    prediction.placeId,
                    listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                )
                val resp = placesClient.fetchPlace(request).await()
                val p = resp.place
                val latLng = p.latLng ?: return@launch
                val address = p.address ?: "${latLng.latitude}, ${latLng.longitude}"

                when (target) {
                    SelectTarget.FROM -> _uiState.update {
                        it.copy(
                            fromLatLng = latLng,
                            fromAddress = address,
                            fromPredictions = emptyList()
                        )
                    }
                    SelectTarget.TO -> _uiState.update {
                        it.copy(
                            toLatLng = latLng,
                            toAddress = address,
                            toPredictions = emptyList()
                        )
                    }
                    else -> {}
                }
                updateRouteIfPossible()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createTransport() {
        viewModelScope.launch {
            val state = _uiState.value

            val from = state.fromLatLng ?: state.currentLatLng
            val to = state.toLatLng

            if (from == null || to == null) {
                _uiState.update { it.copy(error = "Vui lòng chọn điểm đi và điểm đến") }
                return@launch
            }
            if (state.deliveryFee.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập phí chia sẻ") }
                return@launch
            }
            if (state.availableSeat.isBlank() || state.availableSeat.toLongOrNull() == 0L) {
                _uiState.update { it.copy(error = "Vui lòng nhập số ghế trống hợp lệ") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = dataStoreManager.userId.firstOrNull()
            val req = userId?.let {
                TransportServiceRequest(
                    driverId = it,
                    fromLatitude = BigDecimal(from.latitude.toString()),
                    fromLongitude = BigDecimal(from.longitude.toString()),
                    toLatitude = BigDecimal(to.latitude.toString()),
                    toLongitude = BigDecimal(to.longitude.toString()),
                    departTime = state.departTime,
                    availableSeat = state.availableSeat.toLongOrNull(),
                    deliveryFee = BigDecimal(state.deliveryFee),
                    description = state.description.ifBlank { null }
                )
            }

            when (val result = req?.let { transportRepository.createTransportService(it) }) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, creationSuccess = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = (result.error.message ?: "Đã có lỗi xảy ra"))
                }

                null -> TODO()
            }
        }
    }

    private fun updateRouteIfPossible() {
        val origin = _uiState.value.fromLatLng ?: _uiState.value.currentLatLng
        val dest = _uiState.value.toLatLng
        if (origin == null || dest == null) {
            _uiState.update { it.copy(routePoints = emptyList()) }
            return
        }
        getDirections(origin, dest)
    }


    private fun getDirections(origin: LatLng, dest: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRouting = true) }
            try {
                val url =
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=${origin.latitude},${origin.longitude}" +
                            "&destination=${dest.latitude},${dest.longitude}" +
                            "&mode=driving&language=vi&key=$apiKey"

                val req = Request.Builder().url(url).get().build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string().orEmpty()

                val json = JSONObject(body)
                val status = json.optString("status")
                if (status != "OK") {
                    throw IllegalStateException("Directions lỗi: $status")
                }
                val routes = json.getJSONArray("routes")
                val first = routes.getJSONObject(0)
                val polyline = first.getJSONObject("overview_polyline").getString("points")
                val points = decodePolyline(polyline)

                _uiState.update { it.copy(routePoints = points, isRouting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRouting = false, error = e.message) }
            }
        }
    }

    private fun reverseGeocode(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            }
            val addr = list?.firstOrNull()
            listOfNotNull(addr?.featureName, addr?.subLocality, addr?.locality, addr?.adminArea)
                .joinToString(", ")
                .ifBlank { "${latLng.latitude}, ${latLng.longitude}" }
        } catch (_: Exception) {
            "${latLng.latitude}, ${latLng.longitude}"
        }
    }

    // Google Polyline decoder (không cần thêm lib utils)
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    withContext(Dispatchers.IO) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it, null) }
            addOnFailureListener { cont.resumeWithException(it) }
            addOnCanceledListener { cont.cancel() }
        }
    }

private fun AutocompletePrediction.toItem() = PredictionItem(
    placeId = placeId,
    primary = getPrimaryText(null).toString(),
    secondary = getSecondaryText(null)?.toString().orEmpty()
)
