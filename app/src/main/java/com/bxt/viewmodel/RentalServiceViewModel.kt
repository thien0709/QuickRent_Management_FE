package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
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
) : ViewModel() {

    private val _state = MutableStateFlow<RentalRequestsState>(RentalRequestsState.Loading)
    val state: StateFlow<RentalRequestsState> = _state.asStateFlow()

    // Cache cho ảnh và địa chỉ - Giữ nguyên
    private val _thumbs = MutableStateFlow<Map<Long, String?>>(emptyMap())
    val thumbs: StateFlow<Map<Long, String?>> = _thumbs.asStateFlow()

    private val _addresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val addresses: StateFlow<Map<Long, String>> = _addresses.asStateFlow()

    fun loadByRenter() = loadData { rentalRepo.getRentalRequestsByRenter() }
    fun loadByOwner() = loadData { rentalRepo.getRentalRequestsByOwner() }

    private fun loadData(loader: suspend () -> ApiResult<List<RentalRequestResponse>>) = viewModelScope.launch {
        _state.value = RentalRequestsState.Loading
        when (val res = loader()) {
            is ApiResult.Success -> {
                val data = res.data
                _state.value = RentalRequestsState.Success(data)
                // Sau khi có danh sách, tải dữ liệu phụ
                loadAdditionalData(data)
            }
            is ApiResult.Error -> _state.value = RentalRequestsState.Error(res.error)
        }
    }

    private fun loadAdditionalData(requests: List<RentalRequestResponse>) {
        requests.forEach { req ->
            // Tải ảnh thumbnail nếu chưa có trong cache
            req.itemId?.let { id ->
                if (!_thumbs.value.containsKey(id)) {
                    loadThumb(id)
                }
            }
            // Tải địa chỉ nếu chưa có trong cache
            // Sử dụng req.id làm key cho địa chỉ là hợp lý
            req.id?.let { id ->
                if (!_addresses.value.containsKey(id)) {
                    resolveAddress(req)
                }
            }
        }
    }

    private fun loadThumb(itemId: Long) = viewModelScope.launch {
        when (val res = itemRepo.getItemDetail(itemId)) {
            is ApiResult.Success -> {
                _thumbs.update { currentMap ->
                    currentMap + (itemId to res.data.imagePrimary)
                }
            }
            is ApiResult.Error -> {
                _thumbs.update { currentMap ->
                    currentMap + (itemId to null) // Ghi nhận lỗi để không thử lại
                }
            }
        }
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
            .onSuccess { addr ->
                _addresses.update { it + (id to addr.ifBlank { "Không rõ vị trí" }) }
            }
            .onFailure {
                _addresses.update { it + (id to "Không thể lấy địa chỉ") }
            }
    }

    fun updateStatus(
        requestId: Long?,
        newStatus: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        if (requestId == null) return@launch
        // TODO: Hiển thị loading indicator
        when (rentalRepo.updateRequestStatus(requestId, newStatus)) {
            is ApiResult.Success -> {
                onDone()
            }
            is ApiResult.Error -> {
                // TODO: Hiển thị thông báo lỗi
            }
        }
    }
}