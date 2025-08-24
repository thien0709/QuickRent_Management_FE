package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.repository.TransportServiceRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.TransportServiceListState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TransportServiceViewModel @Inject constructor(
    private val repository: TransportServiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransportServiceListState>(TransportServiceListState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    init {
        loadTransportServices()
    }

    fun loadTransportServices() {
        viewModelScope.launch {
            _uiState.value = TransportServiceListState.Loading
            when (val result = repository.getTransportServices()) {
                is ApiResult.Success -> {
                    _uiState.value = TransportServiceListState.Success(result.data.orEmpty())
                }
                is ApiResult.Error -> {
                    _uiState.value = TransportServiceListState.Error(
                        result.error.message ?: "Lỗi khi tải dịch vụ."
                    )
                }
            }
        }
    }

    fun deleteTransportService(id: Long) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is TransportServiceListState.Success) {
                when (val result = repository.deleteTransportService(id)) {
                    is ApiResult.Success -> {
                        _userMessage.emit("Xóa dịch vụ thành công!")
                        loadTransportServices()
                    }
                    is ApiResult.Error -> {
                        _userMessage.emit(result.error.message ?: "Xóa thất bại.")
                    }
                }
            }
        }
    }
}
