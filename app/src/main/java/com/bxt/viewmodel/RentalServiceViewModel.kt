// bxt/viewmodel/RentalServiceViewModel.kt

package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
import com.bxt.di.ErrorResponse
import com.bxt.ui.state.RentalRequestsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RentalServiceViewModel @Inject constructor(
    private val rentalRepo: RentalRequestRepository,
    private val itemRepo: ItemRepository,
    private val locationRepo: LocationRepository,
    // Sẽ thêm repository cho transaction image ở bước sau
) : ViewModel() {

    private val _state = MutableStateFlow<RentalRequestsState>(RentalRequestsState.Loading)
    val state: StateFlow<RentalRequestsState> = _state.asStateFlow()

    private val _isUpdatingStatus = MutableStateFlow<Long?>(null)
    val isUpdatingStatus: StateFlow<Long?> = _isUpdatingStatus.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _thumbs = MutableStateFlow<Map<Long, String?>>(emptyMap())
    val thumbs: StateFlow<Map<Long, String?>> = _thumbs.asStateFlow()

    private val _addresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val addresses: StateFlow<Map<Long, String>> = _addresses.asStateFlow()

    fun loadByRenter() = loadData { rentalRepo.getRentalRequestsByRenter() }
    fun loadByOwner() = loadData { rentalRepo.getRentalRequestsByOwner() }

    private fun loadData(loader: suspend () -> ApiResult<PagedResponse<RentalRequestResponse>>) = viewModelScope.launch {
        _state.value = RentalRequestsState.Loading
        when (val res = loader()) {
            is ApiResult.Success -> {
                val data = res.data.content
                _state.value = RentalRequestsState.Success(data)
                loadAdditionalData(data)
            }
            is ApiResult.Error -> _state.value = RentalRequestsState.Error(res.error)
        }
    }

    fun errorEventConsumed() {
        _errorEvent.value = null
    }

    fun updateStatus(
        requestId: Long?,
        newStatus: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        if (requestId == null) return@launch
        _isUpdatingStatus.value = requestId

        when (val result = rentalRepo.updateRequestStatus(requestId, newStatus)) {
            is ApiResult.Success -> {
                onDone()
            }
            is ApiResult.Error -> {
                _errorEvent.value = result.error.message
            }
        }
        _isUpdatingStatus.value = null
    }

    private fun loadAdditionalData(requests: List<RentalRequestResponse>) {
        requests.forEach { req ->
            req.itemId?.let { id ->
                if (!_thumbs.value.containsKey(id)) {
                    loadThumb(id)
                }
            }
            req.id?.let { id ->
                if (!_addresses.value.containsKey(id)) {
                    resolveAddress(req)
                }
            }
            // TODO: Ở bước tiếp theo, chúng ta sẽ thêm logic để tải ảnh giao dịch tại đây
        }
    }

    private fun loadThumb(itemId: Long) = viewModelScope.launch {
        val imageUrl = when (val res = itemRepo.getItemInfo(itemId)) {
            is ApiResult.Success -> res.data.imagePrimary
            is ApiResult.Error -> null
        }
        _thumbs.update { currentMap -> currentMap + (itemId to imageUrl) }
    }

    private fun resolveAddress(req: RentalRequestResponse) = viewModelScope.launch {
        val id = req.id ?: return@launch
        val lat = req.latTo?.toDouble()
        val lng = req.lngTo?.toDouble()

        if (lat == null || lng == null) {
            _addresses.update { it + (id to "Tọa độ không hợp lệ") }
            return@launch
        }
        locationRepo.getAddressFromLatLng(lat, lng)
            .onSuccess { addr -> _addresses.update { it + (id to addr.ifBlank { "Không rõ vị trí" }) } }
            .onFailure { _addresses.update { it + (id to "Không thể lấy địa chỉ") } }
    }
}