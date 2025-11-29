package com.bxt.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.TransportPackageRepository
import com.bxt.data.repository.TransportPassengerRepository
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.Permissions
import com.bxt.ui.state.TransportServiceDetails
import com.bxt.ui.state.TransportTransactionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@HiltViewModel
class TransportTransactionDetailViewModel @Inject constructor(
    private val transportServiceRepo: TransportServiceRepository,
    private val passengerRepo: TransportPassengerRepository,
    private val packageRepo: TransportPackageRepository,
    private val userRepo: UserRepository,
    private val dataStore: DataStoreManager,
    private val locationRepo: LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ĐỌC THAM SỐ AN TOÀN KIỂU ---

    // serviceId là Long (NavType.LongType)
    val serviceId: Long = when (val raw = savedStateHandle.get<Any?>("serviceId")) {
        is Long -> raw
        is String -> raw.toLongOrNull() ?: error("serviceId invalid")
        else -> error("serviceId is required")
    }

    // asOwner là Boolean (NavType.BoolType) — KHÔNG đọc như String nữa
    private val asOwnerArg: Boolean = savedStateHandle.get<Boolean>("asOwner") ?: false

    // entity là optional String: "", "passenger", "package" -> map "" -> null
    private val entityArg: String? = savedStateHandle.get<String>("entity")?.takeIf { it.isNotBlank() }

    // entityId là Long với defaultValue = -1L -> map -1L -> null
    private val entityIdArg: Long? = savedStateHandle.get<Long>("entityId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow<TransportTransactionState>(TransportTransactionState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _actionResult = MutableSharedFlow<String>()
    val actionResult = _actionResult.asSharedFlow()

    private val _navigateBackEvent = MutableSharedFlow<Unit>()
    val navigateBackEvent = _navigateBackEvent.asSharedFlow()

    private val _myPackagesSent = MutableStateFlow<List<TransportPackageResponse>>(emptyList())
    val myPackagesSent = _myPackagesSent.asStateFlow()

    private val _myPackagesReceived = MutableStateFlow<List<TransportPackageResponse>>(emptyList())
    val myPackagesReceived = _myPackagesReceived.asStateFlow()

    // ===== Địa chỉ (reverse geocode) =====
    private val _serviceAddr = MutableStateFlow<Pair<String, String>?>(null)
    val serviceAddr: StateFlow<Pair<String, String>?> = _serviceAddr.asStateFlow()

    private val _passengerAddrs = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val passengerAddrs: StateFlow<Map<Long, Pair<String, String>>> = _passengerAddrs.asStateFlow()

    private val _packageAddrs = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val packageAddrs: StateFlow<Map<Long, Pair<String, String>>> = _packageAddrs.asStateFlow()

    init { loadDetails() }
    fun refresh() = loadDetails()

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = TransportTransactionState.Loading
            _serviceAddr.value = null
            _passengerAddrs.value = emptyMap()
            _packageAddrs.value = emptyMap()

            try {
                val serviceDef    = async { transportServiceRepo.getTransportServiceById(serviceId) }
                val passengersDef = async { getPassengersScoped() }
                val packagesDef   = async { getPackagesScoped() }
                val meDef         = async { userRepo.getUserInfo() }

                val service: TransportServiceResponse = when (val res = serviceDef.await()) {
                    is ApiResult.Success -> res.data
                    is ApiResult.Error   -> {
                        _uiState.value = TransportTransactionState.Error(res.error.message)
                        return@launch
                    }
                }

                val passengers = passengersDef.await()
                val packages   = packagesDef.await()

                var meId: Long? = (meDef.await() as? ApiResult.Success<UserResponse>)?.data?.id
                if (meId == null) meId = runCatching { dataStore.userId.firstOrNull() }.getOrNull()

                val myPassenger = meId?.let { uid -> passengers.firstOrNull { it.userId == uid } }
                val sent = meId?.let { uid -> packages.filter { it.senderId == uid } } ?: emptyList()
                val recv = meId?.let { uid -> packages.filter { it.receiptId == uid } } ?: emptyList()
                _myPackagesSent.value = sent
                _myPackagesReceived.value = recv

                val isOwner = asOwnerArg || (meId != null && service.driverId == meId)
                val perms = when (service.status) {
                    "PENDING"     -> Permissions(canConfirm = isOwner, canCancelTrip = isOwner, canCancelMyBooking = myPassenger != null)
                    "CONFIRMED"   -> Permissions(canStart = isOwner,   canCancelTrip = isOwner, canCancelMyBooking = myPassenger != null)
                    "IN_PROGRESS" -> Permissions(canComplete = isOwner, canCancelTrip = isOwner)
                    else          -> Permissions()
                }

                _uiState.value = TransportTransactionState.Success(
                    details = TransportServiceDetails(service, passengers, packages),
                    isOwner = isOwner,
                    myPassenger = myPassenger,
                    permissions = perms
                )

                resolveAllAddresses(service, passengers, packages)

            } catch (e: Exception) {
                _uiState.value = TransportTransactionState.Error("Lỗi: ${e.message}")
            }
        }
    }

    // ---------- Lấy dữ liệu theo scope entity ----------
    private suspend fun getPassengersScoped(): List<TransportPassengerResponse> {
        if (entityArg?.equals("passenger", ignoreCase = true) == true && entityIdArg != null) {
            return when (val res = passengerRepo.getTransportPassengerById(entityIdArg)) {
                is ApiResult.Success -> listOf(res.data)
                is ApiResult.Error   -> emptyList()
            }
        }
        return getPassengersForService(serviceId)
    }

    private suspend fun getPackagesScoped(): List<TransportPackageResponse> {
        if (entityArg?.equals("package", ignoreCase = true) == true && entityIdArg != null) {
            return when (val res = packageRepo.getTransportPackageById(entityIdArg)) {
                is ApiResult.Success -> listOf(res.data)
                is ApiResult.Error   -> emptyList()
            }
        }
        return getPackagesForService(serviceId)
    }

    // ---------- Reverse geocode toàn bộ ----------
    private fun resolveAllAddresses(
        service: TransportServiceResponse,
        passengers: List<TransportPassengerResponse>,
        packages: List<TransportPackageResponse>
    ) {
        viewModelScope.launch {
            val fromService = async { toAddress(service.fromLatitude, service.fromLongitude) }
            val toService   = async { toAddress(service.toLatitude, service.toLongitude) }
            _serviceAddr.value = fromService.await() to toService.await()

            val pResults = passengers.mapNotNull { p ->
                val id = p.id ?: return@mapNotNull null
                async {
                    val from = toAddress(p.pickupLatitude, p.pickupLongitude)
                    val to   = toAddress(p.dropoffLatitude, p.dropoffLongitude)
                    id to (from to to)
                }
            }.awaitAll()
            if (pResults.isNotEmpty()) _passengerAddrs.update { it + pResults.toMap() }

            val kResults = packages.mapNotNull { k ->
                val id = k.id ?: return@mapNotNull null
                async {
                    val from = toAddress(k.fromLatitude, k.fromLongitude)
                    val to   = toAddress(k.toLatitude, k.toLongitude)
                    id to (from to to)
                }
            }.awaitAll()
            if (kResults.isNotEmpty()) _packageAddrs.update { it + kResults.toMap() }
        }
    }

    // Chuyển lat/lng -> địa chỉ (fallback: toạ độ rút gọn)
    private suspend fun toAddress(lat: BigDecimal?, lng: BigDecimal?): String {
        if (lat == null || lng == null) return "Không rõ"
        val res = locationRepo.getAddressFromLatLng(lat.toDouble(), lng.toDouble())
        return res.getOrElse { "(${lat.short()}, ${lng.short()})" }
    }

    // ---------- Fallback helpers ----------
    private suspend fun getPassengersForService(serviceId: Long): List<TransportPassengerResponse> {
        when (val res = passengerRepo.getTransportPassengersByServiceId(serviceId)) {
            is ApiResult.Success -> if (res.data.isNotEmpty()) return res.data
            is ApiResult.Error -> { /* fallback */ }
        }
        val out = mutableListOf<TransportPassengerResponse>()
        var page = 0
        var last: Boolean
        do {
            val owner = passengerRepo.getTransportPassengerByOwner(page)
            val renter = passengerRepo.getTransportPassengerByRental(page)
            var touched = false

            if (owner is ApiResult.Success<PagedResponse<TransportPassengerResponse>>) {
                out += owner.data.content.filter { it.transportServiceId == serviceId }
                touched = touched || owner.data.content.isNotEmpty()
                last = owner.data.last
            } else last = true

            if (renter is ApiResult.Success<PagedResponse<TransportPassengerResponse>>) {
                out += renter.data.content.filter { it.transportServiceId == serviceId }
                touched = touched || renter.data.content.isNotEmpty()
                last = last && renter.data.last
            } else last = true

            page++
            if (!touched) break
        } while (!last)
        return out
    }

    private suspend fun getPackagesForService(serviceId: Long): List<TransportPackageResponse> {
        when (val res = packageRepo.getTransportPackagesByServiceId(serviceId)) {
            is ApiResult.Success -> if (res.data.isNotEmpty()) return res.data
            is ApiResult.Error -> { /* fallback */ }
        }
        val out = mutableListOf<TransportPackageResponse>()
        var page = 0
        var last: Boolean
        do {
            val owner = packageRepo.getTransportPackageByOwner(page)
            val renter = packageRepo.getTransportPackageByRental(page)
            var touched = false

            if (owner is ApiResult.Success<PagedResponse<TransportPackageResponse>>) {
                out += owner.data.content.filter { it.transportServiceId == serviceId }
                touched = touched || owner.data.content.isNotEmpty()
                last = owner.data.last
            } else last = true

            if (renter is ApiResult.Success<PagedResponse<TransportPackageResponse>>) {
                out += renter.data.content.filter { it.transportServiceId == serviceId }
                touched = touched || renter.data.content.isNotEmpty()
                last = last && renter.data.last
            } else last = true

            page++
            if (!touched) break
        } while (!last)
        return out
    }

    // ---------- Actions ----------
    fun confirmTrip() {
        viewModelScope.launch {
            when (val res = transportServiceRepo.confirmTransportService(serviceId)) {
                is ApiResult.Success -> { _actionResult.emit("Đã xác nhận."); loadDetails() }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Xác nhận thất bại.")
            }
        }
    }

    fun startTrip() {
        viewModelScope.launch {
            when (val res = transportServiceRepo.startTransportService(serviceId)) {
                is ApiResult.Success -> { _actionResult.emit("Đã bắt đầu."); loadDetails() }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Bắt đầu thất bại.")
            }
        }
    }

    fun completeTrip() {
        viewModelScope.launch {
            when (val res = transportServiceRepo.completeTransportService(serviceId)) {
                is ApiResult.Success -> { _actionResult.emit("Đã hoàn thành."); loadDetails() }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Hoàn thành thất bại.")
            }
        }
    }

    fun cancelTrip(reason: String? = null) {
        viewModelScope.launch {
            when (val res = transportServiceRepo.cancelTransportService(serviceId, reason)) {
                is ApiResult.Success -> { _actionResult.emit("Đã huỷ chuyến."); _navigateBackEvent.emit(Unit) }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Huỷ thất bại.")
            }
        }
    }

    fun cancelMyBooking() {
        val cur = _uiState.value as? TransportTransactionState.Success ?: return
        val passengerId = when {
            entityArg?.equals("passenger", true) == true && entityIdArg != null -> entityIdArg
            else -> cur.myPassenger?.id
        } ?: return

        viewModelScope.launch {
            when (val res = passengerRepo.cancelRide(passengerId)) {
                is ApiResult.Success -> { _actionResult.emit("Đã huỷ đặt chỗ."); loadDetails() }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Không thể huỷ đặt chỗ.")
            }
        }
    }

    fun cancelMyPackage(packageId: Long? = null) {
        val targetId: Long = packageId
            ?: if (entityArg?.equals("package", true) == true && entityIdArg != null) entityIdArg!!
            else return

        viewModelScope.launch {
            when (val res = packageRepo.cancelPackageRequest(targetId)) {
                is ApiResult.Success -> { _actionResult.emit("Đã huỷ gói hàng #$targetId."); loadDetails() }
                is ApiResult.Error   -> _actionResult.emit(res.error.message ?: "Không thể huỷ gói hàng.")
            }
        }
    }
}

private fun BigDecimal.short(): String =
    try { this.setScale(5, RoundingMode.HALF_UP).toPlainString() } catch (_: Throwable) { this.toString() }
