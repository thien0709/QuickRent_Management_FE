// Dán toàn bộ code này vào file CategoryViewModel.kt

package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.CategoryState // Import State của bạn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CategoryState>(CategoryState.Loading)
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = CategoryState.Loading // Reset về loading state
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    _state.value = CategoryState.Success(
                        categories = result.data,
                        products = emptyList(),
                        selectedCategory = null,
                        isLoadingProducts = false
                    )
                }
                is ApiResult.Error -> {
                    _state.value = CategoryState.Error("Không thể tải danh sách danh mục: ${result.error.message}")
                }
            }
        }
    }

    fun loadCategoryData(categoryId: Long) {
        viewModelScope.launch {
            _state.value = CategoryState.Loading // Reset về loading state

            // Load categories first
            when (val categoryResult = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = categoryResult.data
                    val selectedCategory = categories.find { it.id == categoryId }

                    // Then load products for the category
                    when (val itemResult = itemRepository.getItemsByCategory(categoryId)) {
                        is ApiResult.Success -> {
                            val products = itemResult.data.content ?: emptyList()
                            _state.value = CategoryState.Success(
                                categories = categories,
                                products = products,
                                selectedCategory = selectedCategory,
                                isLoadingProducts = false
                            )
                        }
                        is ApiResult.Error -> {
                            _state.value = CategoryState.Success(
                                categories = categories,
                                products = emptyList(),
                                selectedCategory = selectedCategory,
                                isLoadingProducts = false
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    _state.value = CategoryState.Error("Không thể tải danh sách danh mục: ${categoryResult.error.message}")
                }
            }
        }
    }

    fun onCategorySelected(category: CategoryResponse) {
        val currentState = _state.value
        if (currentState is CategoryState.Success) {
            val categoryId = category.id ?: return

            viewModelScope.launch {
                // Update state to show loading for products
                _state.value = currentState.copy(
                    selectedCategory = category,
                    products = emptyList(),
                    isLoadingProducts = true
                )

                when (val itemResult = itemRepository.getItemsByCategory(categoryId)) {
                    is ApiResult.Success -> {
                        val products = itemResult.data.content ?: emptyList()
                        _state.value = currentState.copy(
                            products = products,
                            isLoadingProducts = false
                        )
                    }
                    is ApiResult.Error -> {
                        _state.value = currentState.copy(
                            products = emptyList(),
                            isLoadingProducts = false
                        )
                    }
                }
            }
        }
    }
}