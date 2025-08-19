package com.bxt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.AddItemState
import com.bxt.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

// UI state cho danh mục
sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryResponse>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    // Trạng thái tạo item
    private val _uiState = MutableStateFlow<AddItemState>(AddItemState.Idle)
    val uiState: StateFlow<AddItemState> = _uiState.asStateFlow()

    // User
    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    private val _isUserLoading = MutableStateFlow(true)
    val isUserLoading: StateFlow<Boolean> = _isUserLoading.asStateFlow()

    // Danh mục
    private val _categoriesState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val categoriesState: StateFlow<CategoriesUiState> = _categoriesState.asStateFlow()

    init {
        // Lấy userId từ DataStore
        viewModelScope.launch {
            dataStore.userId.collect { id ->
                _userId.value = id ?: 0L
                _isUserLoading.value = false
            }
        }
        // Tải danh mục lần đầu
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesUiState.Loading
            when (val res = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    val list = res.data.orEmpty()
                    _categoriesState.value = CategoriesUiState.Success(list)
                }
                is ApiResult.Error -> {
                    _categoriesState.value = CategoriesUiState.Error(
                        res.error.message ?: "Không tải được danh mục"
                    )
                }
            }
        }
    }

    fun addItem(
        context: Context,
        req: ItemRequest,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = AddItemState.Submitting

                // Chuẩn bị multipart ảnh theo tuần tự để cập nhật tiến độ chính xác
                val total = imageUris.size
                val parts = mutableListOf<MultipartBody.Part>()
                imageUris.forEachIndexed { index, uri ->
                    val part = withContext(Dispatchers.IO) {
                        // FileUtils.uriToMultipart trả về MultipartBody.Part?
                        FileUtils.uriToMultipart(context, uri, "images")
                    }
                    part?.let { parts.add(it) }

                    // Cập nhật progress chuẩn bị ảnh
                    _uiState.value = AddItemState.Uploading(
                        uploaded = index + 1,
                        total = total
                    )
                }

                // Gọi API tạo item
                when (val res = itemRepository.addItem(req, parts)) {
                    is ApiResult.Success -> {
                        _uiState.value = AddItemState.Success(res.data)
                    }
                    is ApiResult.Error -> {
                        _uiState.value = AddItemState.Error(
                            res.error.message ?: "Không thể thêm sản phẩm"
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.value = AddItemState.Error(t.message ?: "Có lỗi xảy ra")
            }
        }
    }
}
