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
        if (state.value !is CategoryState.Loading) return

        viewModelScope.launch {
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    _state.value = CategoryState.Success(categories = result.data)
                }
                is ApiResult.Error -> {
                    _state.value = CategoryState.Error("Không thể tải danh sách danh mục")
                }
            }
        }
    }

    fun onCategorySelected(category: CategoryResponse) {
        val currentState = _state.value
        if (currentState is CategoryState.Success) {
            val categoryId = category.id ?: return

            viewModelScope.launch {
                _state.update {
                    (it as CategoryState.Success).copy(
                        selectedCategory = category,
                        products = emptyList()
                    )
                }

                when (val itemResult = itemRepository.getItemsByCategory(categoryId)) {
                    is ApiResult.Success -> {
                        val products = itemResult.data.content ?: emptyList()

                        _state.update {
                            (it as CategoryState.Success).copy(
                                products = products
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        _state.update {
                            (it as CategoryState.Success).copy(
                                products = emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadCategoryData(categoryId: Long) {

    }

}