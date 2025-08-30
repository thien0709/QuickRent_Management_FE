package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.data.local.DataStoreManager // THÊM IMPORT
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.ItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
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
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemState>(ItemState.Loading)
    val uiState: StateFlow<ItemState> = _uiState.asStateFlow()

    val currentUserId: StateFlow<String?> = dataStore.userId
        .map { it?.toString() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun load(itemId: Long) {
        viewModelScope.launch {
            _uiState.value = ItemState.Loading

            val itemInfoDeferred = async { itemRepository.getItemInfo(itemId) }
            val itemImagesDeferred = async { itemRepository.getItemImages(itemId) }

            val itemInfoResult = itemInfoDeferred.await()
            val itemImagesResult = itemImagesDeferred.await()

            if (itemInfoResult is ApiResult.Success) {
                val itemInfo = itemInfoResult.data
                val itemImages = (itemImagesResult as? ApiResult.Success)?.data ?: emptyList()
                val itemDetail = ItemDetail(item = itemInfo, images = itemImages)

                _uiState.value = ItemState.Success(itemDetail)
            } else if (itemInfoResult is ApiResult.Error) {
                _uiState.value = ItemState.Error(itemInfoResult.error.message ?: "Failed to load item")
            }
        }
    }
}