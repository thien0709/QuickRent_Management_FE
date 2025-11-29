package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.AddressRepository
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.CategoryState
import com.bxt.util.extractDistrictOrWard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CategoryState>(CategoryState.Loading)
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    val itemAddresses: StateFlow<Map<Long, String>> = addressRepository.itemAddresses


    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = CategoryState.Loading
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = result.data ?: emptyList()
                    if (categories.isNotEmpty()) {
                         onCategorySelected(categories.first(), categories)
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

            when (val categoryResult = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = categoryResult.data ?: emptyList()
                    val selectedCategory = categories.find { it.id == categoryId }

                    if (selectedCategory != null) {
                        // Tải sản phẩm cho danh mục đã chọn
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
            val oldProducts = (currentState as? CategoryState.Success)?.products ?: emptyList()
            _state.value = CategoryState.Success(
                categories = currentCategories,
                products = oldProducts,
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
                   addressRepository.loadAddressesForItems(products)
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