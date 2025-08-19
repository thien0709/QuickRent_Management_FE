package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.ItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemState>(ItemState.Loading)
    val uiState: StateFlow<ItemState> = _uiState.asStateFlow()

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
}