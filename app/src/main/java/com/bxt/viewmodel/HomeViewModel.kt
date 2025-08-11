package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.impl.CategoryRepositoryImpl
import com.bxt.data.repository.impl.ItemRepositoryImpl
import com.bxt.di.ApiResult
import com.bxt.ui.screen.ErrorPopupManager
import com.bxt.ui.state.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepositoryImpl,
    private val itemRepository: ItemRepositoryImpl,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn



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
                ErrorPopupManager.showError(it.error.message, false)
                return@launch
            }

            (items as? ApiResult.Error)?.let {
                ErrorPopupManager.showError(it.error.message, false)
                return@launch
            }

            if (categories is ApiResult.Success && items is ApiResult.Success) {
                _homeState.value = HomeState.Success(categories.data, items.data.content)
            }
        }
    }

}