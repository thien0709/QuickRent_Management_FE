package com.bxt.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.RetrofitClient
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
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
class HomeScreenViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {


    private val _categories = MutableStateFlow<List<CategoryResponse>>(emptyList())
    val categories: StateFlow<List<CategoryResponse>> = _categories

    private val _homescreen = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val homescreen: StateFlow<HomeScreenState> = _homescreen

    fun getDataDefault() {
        viewModelScope.launch {
            try {
                _homescreen.value = HomeScreenState.Loading

                val categoriesDeferred = async { categoryRepository.getCategories() }
                val popularItemsDeferred = async { itemRepository.getAvailableItem() }

                val categories = categoriesDeferred.await()
                val popularItems = popularItemsDeferred.await()

                _categories.value = categories

                _homescreen.value = HomeScreenState.Success(
                    categories = categories,
                    popularItems = popularItems
                )
                Log.e("HomeScreenViewModel", "Categories: $categories")
                Log.e("HomeScreenViewModel", "Popular Items: $popularItems")
            } catch (e: Exception) {
                _homescreen.value = HomeScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
