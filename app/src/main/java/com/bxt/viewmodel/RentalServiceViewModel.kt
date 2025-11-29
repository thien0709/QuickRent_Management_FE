package com.bxt.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.state.RentalServiceState
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

    companion object {
        private const val TAG = "RentalServiceVM"
    }

    enum class LoadMode { OWNER, RENTER }

    private val _uiState = MutableStateFlow<RentalServiceState>(RentalServiceState.Loading)
    val uiState: StateFlow<RentalServiceState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isUpdatingStatus = MutableStateFlow<Long?>(null)
    val isUpdatingStatus: StateFlow<Long?> = _isUpdatingStatus.asStateFlow()

    private val _thumbs = MutableStateFlow<Map<Long, String?>>(emptyMap())
    val thumbs: StateFlow<Map<Long, String?>> = _thumbs.asStateFlow()

    private val _addresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val addresses: StateFlow<Map<Long, String>> = _addresses.asStateFlow()

    private var currentPage = 0
    private var endReached = false
    private var currentMode: LoadMode = LoadMode.OWNER

    init {
        Log.d(TAG, "ViewModel init")
        initialLoad()
    }

    private fun initialLoad() {
        Log.d(TAG, "initialLoad() called")
        viewModelScope.launch {
            _uiState.value = RentalServiceState.Loading
            currentPage = 0
            endReached = false
            Log.d(TAG, "Starting initial load - page: $currentPage, mode: $currentMode")
            fetchRequests(isInitialLoad = true)
        }
    }

    fun switchMode(mode: LoadMode) {
        Log.d(TAG, "switchMode called: $currentMode -> $mode")
        if (currentMode == mode && _uiState.value !is RentalServiceState.Error) {
            Log.d(TAG, "Mode unchanged, skipping")
            return
        }
        currentMode = mode
        initialLoad()
    }

    fun refresh() {
        Log.d(TAG, "refresh() called, isRefreshing: ${_isRefreshing.value}")
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            endReached = false
            Log.d(TAG, "Starting refresh - reset to page 0")
            fetchRequests(isInitialLoad = true)
            _isRefreshing.value = false
        }
    }

    fun canLoadMore(): Boolean = !endReached && !_isLoadingMore.value

    fun isEndReached(): Boolean = endReached

    fun loadNextPage() {
        Log.d(TAG, "loadNextPage() called")
        Log.d(TAG, "  - isLoadingMore: ${_isLoadingMore.value}")
        Log.d(TAG, "  - endReached: $endReached")
        Log.d(TAG, "  - currentPage: $currentPage")

        if (_isLoadingMore.value || endReached) {
            Log.d(TAG, "Skipping loadNextPage - already loading or end reached")
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            Log.d(TAG, "Starting load next page: ${currentPage + 1}")
            fetchRequests(isInitialLoad = false)
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchRequests(isInitialLoad: Boolean) {
        Log.d(TAG, "=== fetchRequests START ===")
        Log.d(TAG, "Parameters:")
        Log.d(TAG, "  - currentPage: $currentPage")
        Log.d(TAG, "  - isInitialLoad: $isInitialLoad")
        Log.d(TAG, "  - endReached: $endReached")
        Log.d(TAG, "  - currentMode: $currentMode")

        val result = when (currentMode) {
            LoadMode.OWNER -> {
                Log.d(TAG, "Calling rentalRepo.getRentalRequestsByOwner($currentPage)")
                rentalRepo.getRentalRequestsByOwner(currentPage)
            }
            LoadMode.RENTER -> {
                Log.d(TAG, "Calling rentalRepo.getRentalRequestsByRenter($currentPage)")
                rentalRepo.getRentalRequestsByRenter(currentPage)
            }
        }

        when (result) {
            is ApiResult.Success -> {
                val pagedData = result.data
                val newItems = pagedData.content

                Log.d(TAG, "=== API SUCCESS ===")
                Log.d(TAG, "Response data:")
                Log.d(TAG, "  - Items received: ${newItems.size}")
                Log.d(TAG, "  - Is last page: ${pagedData.last}")
                Log.d(TAG, "  - Total pages: ${pagedData.totalPages}")
                Log.d(TAG, "  - Total elements: ${pagedData.totalElements}")
                Log.d(TAG, "  - Page size: ${pagedData.size}")

                // **LOGIC GIỐNG HOMEVIEWMODEL**
                if (newItems.isEmpty()) {
                    endReached = true
                    Log.d(TAG, "No new items, set endReached = true")
                }

                val currentState = _uiState.value as? RentalServiceState.Success
                val allItems = if (isInitialLoad) {
                    newItems
                } else {
                    currentState?.requests.orEmpty() + newItems
                }

                _uiState.value = RentalServiceState.Success(allItems)
                Log.d(TAG, "Updated UI state with ${allItems.size} total items")

                // **QUAN TRỌNG: Chỉ tăng currentPage khi có newItems**
                if (newItems.isNotEmpty()) {
                    currentPage++
                    Log.d(TAG, "Incremented currentPage to: $currentPage")
                    loadAdditionalData(newItems)
                }

                Log.d(TAG, "=== fetchRequests END (SUCCESS) ===")
            }
            is ApiResult.Error -> {
                Log.e(TAG, "=== API ERROR ===")
                Log.e(TAG, "Error: ${result.error.message}")
                val errorMessage = result.error.message
                _uiState.value = RentalServiceState.Error(errorMessage)
                ErrorPopupManager.showError(errorMessage, canRetry = true, onRetry = { initialLoad() })
                Log.d(TAG, "=== fetchRequests END (ERROR) ===")
            }
        }
    }

    fun confirmRequest(requestId: Long) = performAction(requestId) { rentalRepo.confirmRequest(requestId) }
    fun rejectRequest(requestId: Long) = performAction(requestId) { rentalRepo.rejectRequest(requestId) }
    fun cancelRequest(requestId: Long) = performAction(requestId) { rentalRepo.cancelRequest(requestId) }

    private fun performAction(requestId: Long, apiCall: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _isUpdatingStatus.value = requestId
            when (val result = apiCall()) {
                is ApiResult.Success -> refresh()
                is ApiResult.Error -> ErrorPopupManager.showError(result.error.message)
            }
            _isUpdatingStatus.value = null
        }
    }

    private fun loadAdditionalData(requests: List<RentalRequestResponse>) {
        requests.forEach { req ->
            req.itemId?.let { id -> if (_thumbs.value[id] == null) loadThumb(id) }
            req.id?.let { id -> if (_addresses.value[id] == null) resolveAddress(req) }
        }
    }

    private fun loadThumb(itemId: Long) = viewModelScope.launch {
        (itemRepo.getItemInfo(itemId) as? ApiResult.Success)?.data?.imagePrimary?.let { imageUrl ->
            _thumbs.update { it + (itemId to imageUrl) }
        }
    }

    private fun resolveAddress(req: RentalRequestResponse) = viewModelScope.launch {
        val id = req.id ?: return@launch
        val lat = req.latTo?.toDouble()
        val lng = req.lngTo?.toDouble()
        if (lat == null || lng == null) return@launch

        locationRepo.getAddressFromLatLng(lat, lng).onSuccess { addr ->
            _addresses.update { it + (id to addr.ifBlank { "Không rõ vị trí" }) }
        }
    }
}