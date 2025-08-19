package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.state.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState

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
        fetchHomeData()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            val categories = categoryRepository.getCategories()
            val items = itemRepository.getAvailableItem()

            (categories as? ApiResult.Error)?.let {
                _homeState.value = HomeState.Error(it.error.message ?: "Lỗi khi tải danh mục.")
                ErrorPopupManager.showError(it.error.message, false)
                return@launch
            }

            (items as? ApiResult.Error)?.let {
                _homeState.value = HomeState.Error(it.error.message ?: "Lỗi khi tải sản phẩm.")
                ErrorPopupManager.showError(it.error.message, false)
                return@launch
            }

            if (categories is ApiResult.Success && items is ApiResult.Success) {
                _homeState.value = HomeState.Success(
                    categories = categories.data ?: emptyList(),
                    popularItems = items.data?.content ?: emptyList()
                )
            }
        }
    }
}
