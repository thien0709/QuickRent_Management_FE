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
import com.bxt.di.ErrorType
import com.bxt.ui.state.RentalRequestsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    private val _isUpdatingStatus = MutableStateFlow<Long?>(null)
    val isUpdatingStatus: StateFlow<Long?> = _isUpdatingStatus.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    private val _thumbs = MutableStateFlow<Map<Long, String?>>(emptyMap())
    val thumbs: StateFlow<Map<Long, String?>> = _thumbs.asStateFlow()

    private val _addresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val addresses: StateFlow<Map<Long, String>> = _addresses.asStateFlow()

    private var currentPage = 0
    private var endReached = false
    private var currentMode: LoadMode? = null

    private enum class LoadMode { OWNER, RENTER }

    // Thêm khối init để khởi tạo việc tải dữ liệu khi ViewModel được tạo
    init {
        initialLoad()
    }

    fun loadByRenter() {
        if (currentMode == LoadMode.RENTER) return // Tránh tải lại khi đã ở chế độ này
        currentMode = LoadMode.RENTER
        initialLoad()
    }

    fun loadByOwner() {
        if (currentMode == LoadMode.OWNER) return // Tránh tải lại khi đã ở chế độ này
        currentMode = LoadMode.OWNER
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            _state.value = RentalRequestsState.Loading
            currentPage = 0
            endReached = false
            fetchRequests(isInitialLoad = true)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // Loại bỏ kiểm tra `currentMode == null` vì nó đã được thiết lập trong `init`
            _state.value = RentalRequestsState.Loading
            currentPage = 0
            endReached = false
            fetchRequests(isInitialLoad = true)
        }
    }

    fun loadNextPage() {
        // Kiểm tra isLoadingMore để tránh gọi API nhiều lần
        val currentState = _state.value
        if (endReached || (currentState is RentalRequestsState.Success && currentState.isLoadingMore)) {
            return
        }

        viewModelScope.launch {
            fetchRequests(isInitialLoad = false)
        }
    }

    private suspend fun fetchRequests(isInitialLoad: Boolean) {
        if (!isInitialLoad) {
            (_state.value as? RentalRequestsState.Success)?.let {
                _state.value = it.copy(isLoadingMore = true)
            }
        }

        val loader: suspend () -> ApiResult<PagedResponse<RentalRequestResponse>> = {
            when (currentMode) {
                LoadMode.OWNER -> rentalRepo.getRentalRequestsByOwner(currentPage)
                LoadMode.RENTER -> rentalRepo.getRentalRequestsByRenter(currentPage)
                null -> ApiResult.Error(ErrorResponse("Chế độ tải không được thiết lập.", false, ErrorType.BAD_REQUEST))
            }
        }

        when (val result = loader()) {
            is ApiResult.Success -> {
                val pagedData = result.data
                val newItems = pagedData.content
                endReached = pagedData.last

                val currentItems = if (isInitialLoad) emptyList() else (_state.value as? RentalRequestsState.Success)?.data ?: emptyList()
                val mergedItems = currentItems + newItems

                _state.value = RentalRequestsState.Success(
                    data = mergedItems,
                    isLoadingMore = false,
                    canLoadMore = !endReached
                )

                if (!endReached) {
                    currentPage++
                }

                loadAdditionalData(newItems)
            }
            is ApiResult.Error -> {
                if (isInitialLoad) {
                    _state.value = RentalRequestsState.Error(result.error)
                } else {
                    _errorEvent.value = "Lỗi khi tải thêm: ${result.error.message}"
                    (_state.value as? RentalRequestsState.Success)?.let {
                        _state.value = it.copy(isLoadingMore = false)
                    }
                }
            }
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
            is ApiResult.Success -> onDone()
            is ApiResult.Error -> _errorEvent.value = result.error.message
        }
        _isUpdatingStatus.value = null
    }

    private fun loadAdditionalData(requests: List<RentalRequestResponse>) {
        requests.forEach { req ->
            req.itemId?.let { id -> if (!_thumbs.value.containsKey(id)) loadThumb(id) }
            req.id?.let { id -> if (!_addresses.value.containsKey(id)) resolveAddress(req) }
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