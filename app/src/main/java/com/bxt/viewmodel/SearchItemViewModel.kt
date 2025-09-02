package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _searchedItems = MutableStateFlow<List<ItemResponse>>(emptyList())
    val searchedItems: StateFlow<List<ItemResponse>> = _searchedItems.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _itemAddresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val itemAddresses: StateFlow<Map<Long, String>> = _itemAddresses.asStateFlow()

    init {
        viewModelScope.launch {
            _searchText
                .debounce(400L)
                .filter { it.length > 1 }
                .distinctUntilChanged()
                .collectLatest { query -> searchItems(query) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchText.value = query
        if (query.isBlank()) {
            _searchedItems.value = emptyList()
            _itemAddresses.value = emptyMap()
        }
    }

    private fun searchItems(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val request = ItemRequest(title = query)
                val page0 = itemRepository.searchItems(request, 0)
                val items = page0.content.orEmpty()
                _searchedItems.value = items

                // reverse-geocode địa chỉ cho batch kết quả
                prefetchAddresses(items)
            } catch (_: Exception) {
                _searchedItems.value = emptyList()
                _itemAddresses.value = emptyMap()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun prefetchAddresses(items: List<ItemResponse>) {
        items.forEach { item ->
            val id = item.id ?: return@forEach
            if (_itemAddresses.value.containsKey(id)) return@forEach

            val lat = item.lat?.toDouble() ?: return@forEach
            val lng = item.lng?.toDouble() ?: return@forEach

            viewModelScope.launch(Dispatchers.IO) {
                val addr = locationRepository.getAddressFromLatLng(lat, lng).getOrNull()
                val fallback = String.format(Locale.US, "%.6f, %.6f", lat, lng)
                _itemAddresses.update { it + (id to (addr ?: fallback)) }
            }
        }
    }
}
