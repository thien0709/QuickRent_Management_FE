package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportServiceListState
import com.bxt.data.api.dto.response.TransportServiceResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@HiltViewModel
class TransportServiceViewModel @Inject constructor(
    private val repository: TransportServiceRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransportServiceListState>(TransportServiceListState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _addressMap = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val addressMap = _addressMap.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    init {
        loadTransportServices()
    }

    fun loadTransportServices() {
        viewModelScope.launch {
            _uiState.value = TransportServiceListState.Loading
            when (val result = repository.getTransportServices()) {
                is ApiResult.Success -> {
                    val list = result.data.orEmpty()
                    _uiState.value = TransportServiceListState.Success(list)
                    prefetchAddresses(list)
                }
                is ApiResult.Error -> {
                    _uiState.value = TransportServiceListState.Error(
                        result.error.message ?: "Lỗi khi tải dịch vụ."
                    )
                }
            }
        }
    }

    /** Resolve tất cả địa chỉ cho danh sách (song song, có fallback) */
    private fun prefetchAddresses(list: List<TransportServiceResponse>) {
        viewModelScope.launch(Dispatchers.IO) {
            val map = mutableMapOf<Long, Pair<String, String>>()

            // chạy song song từng item
            val jobs = list.mapNotNull { s ->
                val id = s.id ?: return@mapNotNull null
                async {
                    val from = if (s.fromLatitude != null && s.fromLongitude != null) {
                        locationRepository
                            .getAddressFromLatLng(
                                s.fromLatitude.toDouble(),
                                s.fromLongitude.toDouble()
                            )
                            .getOrDefault("${s.fromLatitude}, ${s.fromLongitude}")
                    } else "Chưa có địa chỉ"

                    val to = if (s.toLatitude != null && s.toLongitude != null) {
                        locationRepository
                            .getAddressFromLatLng(
                                s.toLatitude.toDouble(),
                                s.toLongitude.toDouble()
                            )
                            .getOrDefault("${s.toLatitude}, ${s.toLongitude}")
                    } else "Chưa có địa chỉ"

                    id to (from to to)
                }
            }

            jobs.forEach { deferred ->
                val (id, pair) = runCatching { deferred.await() }.getOrNull() ?: return@forEach
                map[id] = pair
            }

            _addressMap.value = map
        }
    }

    // Cho trường hợp bạn vẫn dùng Card phiên bản cũ (resolver-based)
    private val cache = mutableMapOf<String, String>()
    suspend fun resolveAddress(lat: Double, lng: Double): String {
        val key = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        cache[key]?.let { return it }
        val text = withContext(Dispatchers.IO) {
            locationRepository.getAddressFromLatLng(lat, lng).getOrDefault("$lat, $lng")
        }
        cache[key] = text
        return text
    }
}
