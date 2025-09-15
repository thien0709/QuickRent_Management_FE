package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TransportRequestState {
    object Loading : TransportRequestState()
    data class Success(val services: List<TransportServiceResponse>) : TransportRequestState()
    data class Error(val message: String) : TransportRequestState()
}

@HiltViewModel
class TransportRequestViewModel @Inject constructor(
    private val transportRepo: TransportServiceRepository,
    private val locationRepo: LocationRepository,
) : ViewModel() {

    enum class LoadMode { DRIVER, PARTICIPANT }

    private val _uiState = MutableStateFlow<TransportRequestState>(TransportRequestState.Loading)
    val uiState: StateFlow<TransportRequestState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _addresses = MutableStateFlow<Map<Long, Pair<String, String>>>(emptyMap())
    val addresses: StateFlow<Map<Long, Pair<String, String>>> = _addresses.asStateFlow()

    private var currentPage = 0
    private var endReached = false
    private var currentMode: LoadMode = LoadMode.DRIVER

    init {
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            _uiState.value = TransportRequestState.Loading
            currentPage = 0
            endReached = false
            fetchRequests(isInitialLoad = true)
        }
    }

    fun switchMode(mode: LoadMode) {
        if (currentMode == mode && _uiState.value !is TransportRequestState.Error) return
        currentMode = mode
        initialLoad()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            endReached = false
            fetchRequests(isInitialLoad = true)
            _isRefreshing.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || endReached || _isRefreshing.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchRequests(isInitialLoad = false)
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchRequests(isInitialLoad: Boolean) {
        val result = when (currentMode) {
            LoadMode.DRIVER -> transportRepo.getTransportServicesByDriver(currentPage)
            LoadMode.PARTICIPANT -> transportRepo.getTransportServicesByParticipant(currentPage)
        }

        when (result) {
            is ApiResult.Success -> {
                val pagedData: PagedResponse<TransportServiceResponse> = result.data
                val newItems = pagedData.content
                endReached = pagedData.last

                val currentState = _uiState.value as? TransportRequestState.Success
                val previousItems = if (isInitialLoad) emptyList() else currentState?.services.orEmpty()
                val allItems = previousItems.plus(newItems)

                _uiState.value = TransportRequestState.Success(allItems)

                if (newItems.isNotEmpty()) {
                    currentPage++
                    loadAddresses(newItems)
                }
            }
            is ApiResult.Error -> {
                val errorMessage = result.error.message
                _uiState.value = TransportRequestState.Error(errorMessage)
                ErrorPopupManager.showError(errorMessage, canRetry = true, onRetry = { initialLoad() })
            }
        }
    }

    private fun loadAddresses(services: List<TransportServiceResponse>) {
        services.forEach { service ->
            service.id?.let { id ->
                if (_addresses.value[id] == null) {
                    resolveAddress(service)
                }
            }
        }
    }

    private fun resolveAddress(service: TransportServiceResponse) = viewModelScope.launch {
        val id = service.id ?: return@launch
        val fromAddress = service.fromLatitude?.toDouble()?.let { lat ->
            service.fromLongitude?.toDouble()?.let { lng ->
                locationRepo.getAddressFromLatLng(lat, lng).getOrNull()
            }
        } ?: "Không rõ địa chỉ"

        val toAddress = service.toLatitude?.toDouble()?.let { lat ->
            service.toLongitude?.toDouble()?.let { lng ->
                locationRepo.getAddressFromLatLng(lat, lng).getOrNull()
            }
        } ?: "Không rõ địa chỉ"

        _addresses.update { it + (id to (fromAddress to toAddress)) }
    }
}