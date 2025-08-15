// com/bxt/viewmodel/ItemViewModel.kt
package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.RentalRequestRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.ItemState
import com.bxt.ui.state.RentalState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch



@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val rentalRepository: RentalRequestRepository,
    dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemState>(ItemState.Loading)
    val uiState: StateFlow<ItemState> = _uiState.asStateFlow()

    val userAddress: StateFlow<String> =
        dataStore.currentAddress
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val userId: StateFlow<Long?> =
        dataStore.userId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _rentalState = MutableStateFlow<RentalState>(RentalState.Idle)
    val rentalState: StateFlow<RentalState> = _rentalState.asStateFlow()

    fun load(itemId: Long) {
        viewModelScope.launch {
            _uiState.value = ItemState.Loading
            when (val res = itemRepository.getItemDetail(itemId)) {
                is ApiResult.Success -> _uiState.value = ItemState.Success(res.data)
                is ApiResult.Error -> _uiState.value =
                    ItemState.Error(res.error.message ?: "Failed to load item")
            }
        }
    }

    /** Tạo rental request với thời điểm dạng java.time.Instant. */
    fun createRentalRequest(
        itemId: Long,
        startAt: Instant,
        endAt: Instant,
        totalPrice: BigDecimal
    ) {
        val renter = userId.value
        if (renter == null) {
            _rentalState.value = RentalState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _rentalState.value = RentalState.Submitting

            val body = RentalRequestRequest(
                itemId = itemId,
                renterId = renter,       // Nếu BE tự lấy từ token, bỏ field này ở cả DTO & đây
                rentalStartTime = startAt,
                rentalEndTime = endAt,
                totalPrice = totalPrice
            )

            when (val res = rentalRepository.createRentalRequest(body)) {
                is ApiResult.Success -> {
                    _rentalState.value = RentalState.Success(res.data.id) // lấy id từ response
                }
                is ApiResult.Error -> {
                    _rentalState.value = RentalState.Error(
                        res.error.message ?: "Create request failed"
                    )
                }
            }
        }
    }

    fun resetRentalState() {
        _rentalState.value = RentalState.Idle
    }
}
