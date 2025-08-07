package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.impl.CategoryRepository
import com.bxt.data.repository.impl.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeScreenState {
    data class Success(
        val categories: List<CategoryResponse>,
        val popularItems: List<ItemResponse>
    ) : HomeScreenState()

    data class Error(val message: String) : HomeScreenState()
    object Loading : HomeScreenState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(

    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val homeState: StateFlow<HomeScreenState> = _homeState
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
            try {
                _homeState.value = HomeScreenState.Loading

                val categoriesDeferred = async { categoryRepository.getCategories() }
                val itemsDeferred = async { itemRepository.getAvailableItem() }

                val categories = categoriesDeferred.await()
                val pagedItems = itemsDeferred.await()
                val items = pagedItems.content

                _homeState.value = HomeScreenState.Success(
                    categories = categories,
                    popularItems = items
                )

            } catch (e: Exception) {
                _homeState.value = HomeScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

