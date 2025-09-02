package com.bxt.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.ItemRequest // Giả sử bạn đã tạo DTO này
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.ItemRepository // Giả sử bạn có repository này
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _searchedItems = MutableStateFlow<List<ItemResponse>>(emptyList())
    val searchedItems = _searchedItems.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            _searchText
                .debounce(400L)
                .filter { it.length > 1 }
                .distinctUntilChanged()
                .collectLatest { query ->
                    searchItems(query)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchText.value = query
        if (query.isBlank()) {
            _searchedItems.value = emptyList()
        }
    }

    private fun searchItems(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val request = ItemRequest(title = query)
                val resultPage = itemRepository.searchItems(request, 0)
                _searchedItems.value = resultPage.content
            } catch (e: Exception) {
                // Xử lý lỗi, ví dụ: hiển thị thông báo
                _searchedItems.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}