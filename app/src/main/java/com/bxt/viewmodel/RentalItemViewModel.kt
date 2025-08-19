package com.bxt.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.state.RentalState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class RentalItemViewModel @Inject constructor(
    private val rentalRepository: RentalRequestRepository,
    private val dataStore: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0L
    val pricePerHour: BigDecimal = BigDecimal(savedStateHandle.get<String>("price") ?: "0")

    private val _rentalState = MutableStateFlow<RentalState>(RentalState.Idle)
    val rentalState: StateFlow<RentalState> = _rentalState.asStateFlow()

    private val userId: StateFlow<Long?> = dataStore.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun createRentalRequest(
        startAt: Instant,
        endAt: Instant,
        totalPrice: BigDecimal,
        address: String,
        latTo : BigDecimal,
        lngTo : BigDecimal
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
                itemId = itemId,
                renterId = renterId,
                rentalStartTime = startAt,
                rentalEndTime = endAt,
                totalPrice = totalPrice,
                latTo = latTo,
                lngTo = lngTo
            )
            when (val result = rentalRepository.createRentalRequest(request)) {
                is ApiResult.Success -> {
                    _rentalState.value = RentalState.Success(result.data.id)
                }
                is ApiResult.Error -> {
                    val err = result.error
                    ErrorPopupManager.showError(
                        message = err.message,
                        canRetry = err.canRetry,
                        onRetry = if (err.canRetry) { {  } } else null
                    )
                    _rentalState.value = RentalState.Error(err.message)
                }
            }
        }
    }

    fun resetRentalState() {
        _rentalState.value = RentalState.Idle
    }
}