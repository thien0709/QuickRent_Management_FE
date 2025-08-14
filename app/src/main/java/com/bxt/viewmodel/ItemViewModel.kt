package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.data.api.dto.response.ItemImageResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.impl.ItemImageRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.ItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val imageRepository: ItemImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemState>(ItemState.Loading)
    val uiState = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = ItemState.Loading

            supervisorScope {
                val itemDef   = async { itemRepository.getItemDetail(id) }
                val imagesDef = async { imageRepository.getItemImages(id) }

                when (val itemRes = itemDef.await()) {
                    is ApiResult.Success -> {
                        val item: ItemResponse = itemRes.data

                        val images: List<String> = when (val imgRes = imagesDef.await()) {
                            is ApiResult.Success -> imgRes.data
                            is ApiResult.Error   -> emptyList()
                        }

                        _uiState.value = ItemState.Success(
                            ItemDetail(item = item, images = images)
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = ItemState.Error(
                            message = itemRes.error.message ?: "Lỗi không xác định khi tải chi tiết sản phẩm."
                        )
                    }
                }
            }
        }
    }
//    fun addItemToCart(item: ItemResponse) {
//        viewModelScope.launch {
//            itemRepository.addItemToCart(item)
//        }
//    }
//
//    fun removeItemFromCart(item: ItemResponse) {
//        viewModelScope.launch {
//            itemRepository.removeItemFromCart(item)
//        }
//    }

}
