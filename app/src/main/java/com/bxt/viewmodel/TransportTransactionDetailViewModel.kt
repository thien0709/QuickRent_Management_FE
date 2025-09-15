package com.bxt.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.repository.TransportPackageRepository
import com.bxt.data.repository.TransportPassengerRepository
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportTransactionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransportTransactionDetails(
    val service: TransportServiceResponse,
    val passengers: List<TransportPassengerResponse>,
    val packages: List<TransportPackageResponse>
)



@HiltViewModel
class TransportTransactionDetailViewModel @Inject constructor(
    private val transportServiceRepo: TransportServiceRepository,
    private val passengerRepo: TransportPassengerRepository,
    private val packageRepo: TransportPackageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serviceId: Long = savedStateHandle.get<Long>("serviceId") ?: 0L

    private val _uiState = MutableStateFlow<TransportTransactionState>(TransportTransactionState.Loading)
    val uiState: StateFlow<TransportTransactionState> = _uiState.asStateFlow()

    private val _actionResult = MutableSharedFlow<String>()
    val actionResult = _actionResult.asSharedFlow()
    private val _navigateBackEvent = MutableSharedFlow<Unit>()
    val navigateBackEvent = _navigateBackEvent.asSharedFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        if (serviceId == 0L) {
            _uiState.value = TransportTransactionState.Error("ID chuyến đi không hợp lệ.")
            return
        }
        viewModelScope.launch {
            _uiState.value = TransportTransactionState.Loading
            try {
                val serviceDeferred = async { transportServiceRepo.getTransportServiceById(serviceId) }
                val passengersDeferred = async { passengerRepo.getTransportPassengersByServiceId(serviceId) }
                val packagesDeferred = async { packageRepo.getTransportPackagesByServiceId(serviceId) }

                val serviceResult = serviceDeferred.await()
                val passengersResult = passengersDeferred.await()
                val packagesResult = packagesDeferred.await()

                if (serviceResult is ApiResult.Success) {
                    val details = TransportTransactionDetails(
                        service = serviceResult.data,
                        passengers = (passengersResult as? ApiResult.Success)?.data ?: emptyList(),
                        packages = (packagesResult as? ApiResult.Success)?.data ?: emptyList()
                    )
                    _uiState.value = TransportTransactionState.Success(details)
                } else {
                    val errorMsg = (serviceResult as ApiResult.Error).error.message
                    _uiState.value = TransportTransactionState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = TransportTransactionState.Error("Lỗi không xác định: ${e.message}")
            }
        }
    }

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            val result = transportServiceRepo.updateServiceStatus(serviceId, newStatus)
            when (result) {
                is ApiResult.Success -> {
                    _actionResult.emit("Cập nhật trạng thái thành công!")
                    loadDetails()
                }
                is ApiResult.Error -> {
                    _actionResult.emit("Lỗi: ${result.error.message}")
                }
            }
        }
    }

    fun cancelTrip() {
        viewModelScope.launch {
            val result = transportServiceRepo.updateServiceStatus(serviceId, "CANCELLED")
            when (result) {
                is ApiResult.Success -> {
                    _actionResult.emit("Đã hủy chuyến đi thành công.")
                    _navigateBackEvent.emit(Unit)
                }
                is ApiResult.Error -> {
                    _actionResult.emit("Lỗi: ${result.error.message}")
                }
            }
        }
    }
}