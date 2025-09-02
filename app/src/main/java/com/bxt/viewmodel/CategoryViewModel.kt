package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
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
    // *** THÊM 2 DÒNG NÀY ***
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CategoryState>(CategoryState.Loading)
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    // *** THÊM STATE MỚI ĐỂ LƯU ĐỊA CHỈ ***
    private val _itemAddresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val itemAddresses: StateFlow<Map<Long, String>> = _itemAddresses.asStateFlow()


    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = CategoryState.Loading
            _itemAddresses.value = emptyMap() // Xóa địa chỉ cũ
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val categories = result.data ?: emptyList()
                    if (categories.isNotEmpty()) {
                        // Tự động chọn danh mục đầu tiên và tải sản phẩm
                        onCategorySelected(categories.first(), categories)
                    } else {
                        // Cập nhật trạng thái thành công với danh sách rỗng
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
            _itemAddresses.value = emptyMap() // Xóa địa chỉ cũ

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
            _state.value = CategoryState.Success(
                categories = currentCategories,
                products = emptyList(), // Xóa sản phẩm cũ
                selectedCategory = category,
                isLoadingProducts = true
            )
            _itemAddresses.value = emptyMap() // Xóa địa chỉ cũ

            when (val itemResult = itemRepository.getItemsByCategory(categoryId)) {
                is ApiResult.Success -> {
                    val products = itemResult.data.content ?: emptyList()
                    _state.update {
                        (it as? CategoryState.Success)?.copy(
                            products = products,
                            isLoadingProducts = false
                        ) ?: it
                    }
                    loadAddressesForItems(products)
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

    private fun loadAddressesForItems(items: List<ItemResponse>) {
        viewModelScope.launch {
            items.forEach { item ->
                if (item.id != null && !_itemAddresses.value.containsKey(item.id)) {
                    item.ownerId?.let { ownerId ->
                        val locationResult = userRepository.getUserLocation(ownerId)
                        if (locationResult is ApiResult.Success) {
                            val locationMap = locationResult.data
                            val lat = locationMap["lat"]?.toDouble()
                            val lng = locationMap["lng"]?.toDouble()
                            if (lat != null && lng != null) {
                                val districtResult = locationRepository.getAddressFromLatLng(lat, lng)
                                val districtOrWard = extractDistrictOrWard(districtResult.getOrNull()) ?: "Location unknown"
                                _itemAddresses.update { currentMap ->
                                    currentMap + (item.id to districtOrWard)
                                }
                            } else {
                                _itemAddresses.update { currentMap ->
                                    currentMap + (item.id to "Owner location not updated")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}