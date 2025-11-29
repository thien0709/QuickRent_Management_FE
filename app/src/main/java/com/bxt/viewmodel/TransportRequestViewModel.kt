package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.TransportPackageRepository
import com.bxt.data.repository.TransportPassengerRepository
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportRequestState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

enum class RequestTab { OWNER, RENTER }
enum class ParticipationKind { OWNER, RIDE, PKG_SEND, PKG_RECV }

data class UnifiedTrip(
    val service: TransportServiceResponse,
    val kind: ParticipationKind,
    val passenger: TransportPassengerResponse? = null,
    val pkg: TransportPackageResponse? = null
)

@HiltViewModel
class TransportRequestViewModel @Inject constructor(
    private val passengerRepo: TransportPassengerRepository,
    private val packageRepo: TransportPackageRepository,
    private val serviceRepo: TransportServiceRepository,
    private val locationRepo: LocationRepository
) : ViewModel() {

    private val _tab = MutableStateFlow(RequestTab.OWNER)
    val tab: StateFlow<RequestTab> = _tab.asStateFlow()

    private val _uiState = MutableStateFlow<TransportRequestState>(TransportRequestState.Loading)
    val uiState: StateFlow<TransportRequestState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** key -> (fromAddress, toAddress)
     *  key dạng:
     *   - OWNER: "S:<serviceId>"
     *   - RIDE : "R:<passengerId>"
     *   - PKG  : "K:<packageId>"
     */
    private val _addresses = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val addresses: StateFlow<Map<String, Pair<String, String>>> = _addresses.asStateFlow()

    // paging độc lập
    private var pagePassenger = 0
    private var pagePackage = 0
    private var lastPassenger = false
    private var lastPackage = false

    private val serviceCache = mutableMapOf<Long, TransportServiceResponse>()
    private val buffer = mutableListOf<UnifiedTrip>()

    init { refresh() }

    fun switchMode(newTab: RequestTab) {
        if (_tab.value == newTab) return
        _tab.value = newTab
        refresh()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = TransportRequestState.Loading

            pagePassenger = 0
            pagePackage   = 0
            lastPassenger = false
            lastPackage   = false
            serviceCache.clear()
            buffer.clear()
            _addresses.value = emptyMap()

            fetchNext()
            _isRefreshing.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || _isRefreshing.value || (lastPassenger && lastPackage)) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchNext()
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchNext() = coroutineScope {
        var err: String? = null
        val newRowsForAddress = mutableListOf<UnifiedTrip>()

        val pDef = if (!lastPassenger) async {
            when (_tab.value) {
                RequestTab.OWNER  -> passengerRepo.getTransportPassengerByOwner(pagePassenger)
                RequestTab.RENTER -> passengerRepo.getTransportPassengerByRental(pagePassenger)
            }
        } else null

        val kDef = if (!lastPackage) async {
            when (_tab.value) {
                RequestTab.OWNER  -> packageRepo.getTransportPackageByOwner(pagePackage)
                RequestTab.RENTER -> packageRepo.getTransportPackageByRental(pagePackage)
            }
        } else null

        pDef?.await()?.let { res ->
            when (res) {
                is ApiResult.Success<PagedResponse<TransportPassengerResponse>> -> {
                    val page = res.data
                    if (page.content.isNotEmpty()) {
                        for (r in page.content) {
                            val svc = getService(r.transportServiceId) ?: continue
                            val kind =
                                if (_tab.value == RequestTab.OWNER) ParticipationKind.OWNER
                                else ParticipationKind.RIDE
                            val row = UnifiedTrip(service = svc, kind = kind, passenger = r)
                            buffer += row
                            newRowsForAddress += row
                        }
                        pagePassenger++
                    }
                    lastPassenger = page.last
                }
                is ApiResult.Error -> err = res.error.message
            }
        }

        kDef?.await()?.let { res ->
            when (res) {
                is ApiResult.Success<PagedResponse<TransportPackageResponse>> -> {
                    val page = res.data
                    if (page.content.isNotEmpty()) {
                        for (pk in page.content) {
                            val svc = getService(pk.transportServiceId) ?: continue
                            val kind = when (_tab.value) {
                                RequestTab.OWNER  -> ParticipationKind.OWNER
                                RequestTab.RENTER -> when {
                                    pk.senderId != null  -> ParticipationKind.PKG_SEND
                                    pk.receiptId != null -> ParticipationKind.PKG_RECV
                                    else -> ParticipationKind.PKG_SEND
                                }
                            }
                            val row = UnifiedTrip(service = svc, kind = kind, pkg = pk)
                            buffer += row
                            newRowsForAddress += row
                        }
                        pagePackage++
                    }
                    lastPackage = page.last
                }
                is ApiResult.Error -> err = res.error.message
            }
        }

        if (err != null) {
            _uiState.value = TransportRequestState.Error(err!!)
            return@coroutineScope
        }

        // Resolve địa chỉ cho các dòng mới
        if (newRowsForAddress.isNotEmpty()) resolveAddresses(newRowsForAddress)

        val merged = buffer
            .groupBy { it.service.id }
            .values
            .map { group ->
                group.sortedBy {
                    when (it.kind) {
                        ParticipationKind.RIDE     -> 0
                        ParticipationKind.PKG_SEND -> 1
                        ParticipationKind.PKG_RECV -> 2
                        ParticipationKind.OWNER    -> 3
                    }
                }.first()
            }
            .sortedByDescending { it.service.id ?: 0L }

        _uiState.value = TransportRequestState.Success(merged)
    }

    private suspend fun getService(serviceId: Long?): TransportServiceResponse? {
        if (serviceId == null) return null
        serviceCache[serviceId]?.let { return it }
        return when (val res = serviceRepo.getTransportServiceById(serviceId)) {
            is ApiResult.Success<TransportServiceResponse> -> {
                serviceCache[serviceId] = res.data
                res.data
            }
            is ApiResult.Error -> null
        }
    }

    /** Resolve địa chỉ và đẩy vào cache */
    private fun resolveAddresses(rows: List<UnifiedTrip>) {
        rows.forEach { row ->
            val key = when (row.kind) {
                ParticipationKind.OWNER    -> "S:${row.service.id}"
                ParticipationKind.RIDE     -> "R:${row.passenger?.id}"
                ParticipationKind.PKG_SEND -> "K:${row.pkg?.id}"
                ParticipationKind.PKG_RECV -> "K:${row.pkg?.id}"
            }
            if (_addresses.value.containsKey(key)) return@forEach

            viewModelScope.launch {
                val (fromText, toText) = when (row.kind) {
                    ParticipationKind.OWNER -> {
                        val s = row.service
                        resolveAddressPair(s.fromLatitude, s.fromLongitude, s.toLatitude, s.toLongitude)
                    }
                    ParticipationKind.RIDE -> {
                        val p = row.passenger
                        resolveAddressPair(p?.pickupLatitude, p?.pickupLongitude, p?.dropoffLatitude, p?.dropoffLongitude)
                    }
                    ParticipationKind.PKG_SEND,
                    ParticipationKind.PKG_RECV -> {
                        val k = row.pkg
                        resolveAddressPair(k?.fromLatitude, k?.fromLongitude, k?.toLatitude, k?.toLongitude)
                    }
                }
                _addresses.update { it + (key to (fromText to toText)) }
            }
        }
    }

    private suspend fun resolveAddressPair(
        fromLat: BigDecimal?, fromLng: BigDecimal?,
        toLat: BigDecimal?, toLng: BigDecimal?
    ): Pair<String, String> {
        val from = if (fromLat != null && fromLng != null)
            (locationRepo.getAddressFromLatLng(fromLat.toDouble(), fromLng.toDouble()).getOrNull()
                ?: "(${fromLat.short()}, ${fromLng.short()})")
        else "Không rõ"

        val to = if (toLat != null && toLng != null)
            (locationRepo.getAddressFromLatLng(toLat.toDouble(), toLng.toDouble()).getOrNull()
                ?: "(${toLat.short()}, ${toLng.short()})")
        else "Không rõ"

        return from to to
    }
}

/* ---- small helper ---- */
private fun BigDecimal.short(): String =
    try { this.setScale(5, RoundingMode.HALF_UP).toPlainString() } catch (_: Throwable) { this.toString() }
