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
            _state.value = CategoryState.Loading
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = result.data
                     if (categories.isNotEmpty()) {
                        val firstCategory = categories.first()
                        onCategorySelected(firstCategory, categories)
                    } else {
                        _state.value = CategoryState.Success(
                            categories = emptyList(),
                            products = emptyList(),
                            selectedCategory = null,
                            isLoadingProducts = false
                        )
                    }
                    }
                is ApiResult.Error -> {
                    _state.value = CategoryState.Error("Không thể tải danh sách danh mục: ${result.error.message}")
                }
            }
        }
    }

    fun loadCategoryData(categoryId: Long) {
        viewModelScope.launch {
            _state.value = CategoryState.Loading

            // Load categories first
            when (val categoryResult = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = categoryResult.data
                    val selectedCategory = categories.find { it.id == categoryId }

                    // Then load products for the category
                    if (selectedCategory != null) {
                        onCategorySelected(selectedCategory, categories)
                    } else {
                        _state.value = CategoryState.Error("Không tìm thấy danh mục với ID: $categoryId")
                    }
                }
                is ApiResult.Error -> {
                    _state.value = CategoryState.Error("Không thể tải danh sách danh mục: ${categoryResult.error.message}")
                }
            }
        }
    }

    fun onCategorySelected(category: CategoryResponse, allCategories: List<CategoryResponse>? = null) {
        val currentState = _state.value
        val currentCategories = allCategories ?: (currentState as? CategoryState.Success)?.categories ?: return
        val categoryId = category.id ?: return

        viewModelScope.launch {
            _state.value = CategoryState.Success(
                categories = currentCategories,
                products = emptyList(),
                selectedCategory = category,
                isLoadingProducts = true
            )

            when (val itemResult = itemRepository.getItemsByCategory(categoryId)) {
                is ApiResult.Success -> {
                    val products = itemResult.data.content ?: emptyList()
                    _state.update {
                        (it as? CategoryState.Success)?.copy(
                            products = products,
                            isLoadingProducts = false
                        ) ?: it
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        (it as? CategoryState.Success)?.copy(
                            products = emptyList(),
                            isLoadingProducts = false
                        ) ?: it
                    }
                }
            }
        }
    }
}