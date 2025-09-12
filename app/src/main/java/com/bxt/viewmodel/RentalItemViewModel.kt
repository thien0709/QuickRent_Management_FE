package com.bxt.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.ItemDataState
import com.bxt.ui.state.RentalState
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class RentalItemViewModel @Inject constructor(
    private val rentalRepository: RentalRequestRepository,
    private val itemRepository: ItemRepository,
    private val dataStore: DataStoreManager,
    private val locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0L

    private val _itemDataState = MutableStateFlow<ItemDataState>(ItemDataState.Loading)
    val itemDataState: StateFlow<ItemDataState> = _itemDataState.asStateFlow()

    private val _rentalState = MutableStateFlow<RentalState>(RentalState.Idle)
    val rentalState: StateFlow<RentalState> = _rentalState.asStateFlow()

    private val _resolvedAddress = MutableStateFlow<String?>(null)
    val resolvedAddress: StateFlow<String?> = _resolvedAddress.asStateFlow()

    private val userId: StateFlow<Long?> = dataStore.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val initialAddress: StateFlow<String?> = dataStore.currentAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val initialMapLocation: StateFlow<Point> = dataStore.currentLocation
        .map { locationPair ->
            locationPair?.let { Point.fromLngLat(it.second, it.first) }
                ?: Point.fromLngLat(106.660172, 10.762622)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Point.fromLngLat(106.660172, 10.762622))

    init {
        loadItemDetails()
    }

    private fun loadItemDetails() {
        if (itemId == 0L) {
            _itemDataState.value = ItemDataState.Error("Item ID không hợp lệ.")
            return
        }
        viewModelScope.launch {
            _itemDataState.value = ItemDataState.Loading
            when (val result = itemRepository.getItemInfo(itemId)) {
                is ApiResult.Success -> {
                    _itemDataState.value = ItemDataState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _itemDataState.value = ItemDataState.Error(result.error.message)
                }
            }
        }
    }

    fun getAddressFromPoint(point: Point) {
        viewModelScope.launch {
            val result = locationRepository.getAddressFromLatLng(point.latitude(), point.longitude())
            _resolvedAddress.value = result.getOrNull() ?: "Không thể lấy địa chỉ"
        }
    }

    fun clearResolvedAddress() {
        _resolvedAddress.value = null
    }

    fun createRentalRequest(
        item: ItemResponse, // Nhận toàn bộ đối tượng item
        startAt: Instant,
        endAt: Instant,
        totalPrice: BigDecimal,
        address: String,
        latTo : BigDecimal,
        lngTo : BigDecimal,
        paymentMethod : String
    ) {
        val renterId = userId.value
        if (renterId == null) {
            _rentalState.value = RentalState.Error("Vui lòng đăng nhập để thuê")
            return
        }
        if (address.isBlank()) {
            _rentalState.value = RentalState.Error("Vui lòng nhập địa chỉ giao hàng")
            return
        }

        viewModelScope.launch {
            _rentalState.value = RentalState.Submitting
            val request = RentalRequestRequest(
                itemId = item.id!!,
                renterId = renterId,
                rentalStartTime = startAt,
                rentalEndTime = endAt,
                totalPrice = totalPrice,
                latTo = latTo,
                lngTo = lngTo,
                paymentMethod = paymentMethod
            )

            when (val result = rentalRepository.createRentalRequest(request)) {
                is ApiResult.Success -> {
                    _rentalState.value = RentalState.Success(result.data.id)
                }
                is ApiResult.Error -> {
                    _rentalState.value = RentalState.Error(result.error.message)
                }
            }
        }
    }

    fun resetRentalState() {
        _rentalState.value = RentalState.Idle
    }
}