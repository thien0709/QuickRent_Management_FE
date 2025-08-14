package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.screen.ErrorPopupManager
import com.bxt.ui.state.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled

    fun setDarkModeEnabled(enabled: Boolean) {
        _isDarkModeEnabled.value = enabled
    }
    init {
        observeLoginState()
        fetchHomeData()
    }


    private fun observeLoginState() {
        viewModelScope.launch {
            dataStore.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }
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
                    popularItems = items.data?.content ?: emptyList() // ✅ luôn đảm bảo không null
                )
            }
        }
    }


}