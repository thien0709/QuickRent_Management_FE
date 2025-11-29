package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AddressRepository
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.state.HomeState
import com.bxt.util.extractDistrictOrWard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
    private val dataStore: DataStoreManager,
    private val addressRepository: AddressRepository

) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    val itemAddresses: StateFlow<Map<Long, String>> = addressRepository.itemAddresses

    private var currentPage = 0
    private var endReached = false

    val isDarkModeEnabled: StateFlow<Boolean> =
        dataStore.isDarkModeEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDarkMode(enabled) }
    }

    init {
        viewModelScope.launch { initialLoad() }
    }

    private suspend fun initialLoad() {
        _homeState.value = HomeState.Loading
        fetchCategories()
        currentPage = 0
        endReached = false
        fetchItems(reset = true)
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            endReached = false
            fetchCategories()
            fetchItems(reset = true)
            _isRefreshing.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || endReached) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchItems(reset = false)
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchCategories() {
        when (val categories = categoryRepository.getCategories()) {
            is ApiResult.Success -> {
                val prev = _homeState.value as? HomeState.Success
                _homeState.value = HomeState.Success(
                    categories = categories.data ?: emptyList(),
                    popularItems = prev?.popularItems ?: emptyList(),
                    currentPage = prev?.currentPage ?: 0,
                    totalPages = prev?.totalPages ?: 0,
                    isLastPage = prev?.isLastPage ?: false,
                    totalElements = prev?.totalElements ?: 0
                )
            }
            is ApiResult.Error -> {
                _homeState.value = HomeState.Error(categories.error.message ?: "Lỗi khi tải danh mục.")
                ErrorPopupManager.showError(categories.error.message, false)
            }
        }
    }

    private suspend fun fetchItems(reset: Boolean) {
        when (val itemsResult = itemRepository.getAvailableItem(page = currentPage)) {
            is ApiResult.Success -> {
                val newItems = itemsResult.data?.content ?: emptyList()
                if (newItems.isEmpty()) {
                    endReached = true
                }
                val currentState = _homeState.value as? HomeState.Success
                val allItems = if (reset) newItems else currentState?.popularItems.orEmpty() + newItems

                _homeState.value = HomeState.Success(
                    categories = currentState?.categories ?: emptyList(),
                    popularItems = allItems,
                    currentPage = currentPage,
                    isLastPage = endReached,
                    totalElements = allItems.size.toLong()
                )

                if (newItems.isNotEmpty()) {
                    currentPage++
                    addressRepository.loadAddressesForItems(newItems)
                }
            }
            is ApiResult.Error -> {
                _homeState.value = HomeState.Error(itemsResult.error.message)
                ErrorPopupManager.showError(itemsResult.error.message, true)
            }
        }
    }

 }
