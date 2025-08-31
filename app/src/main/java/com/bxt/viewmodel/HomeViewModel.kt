package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.CategoryRepository
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.components.ErrorPopupManager
import com.bxt.ui.state.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // Giữ nguyên cách expose như file cũ để không thay đổi interface phía UI
    val homeState: StateFlow<HomeState> = _homeState

    // Thêm state cho refresh & load-more
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Biến phân trang
    private var currentPage = 0
    private var endReached = false

    val isDarkModeEnabled: StateFlow<Boolean> =
        dataStore.isDarkModeEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDarkMode(enabled) }
    }

    init {
        viewModelScope.launch { initialLoad() }
    }

    /** Tải lần đầu: categories + trang 0 items */
    private suspend fun initialLoad() {
        _homeState.value = HomeState.Loading
        fetchCategories()
        // reset trang
        currentPage = 0
        endReached = false
        fetchItems(reset = true)
    }

    /** Kéo xuống để làm mới */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            endReached = false
            fetchCategories()
            fetchItems(reset = true)
            _isRefreshing.value = false
        }
    }

    /** Cuộn tới cuối để nạp thêm */
    fun loadNextPage() {
        if (_isLoadingMore.value || endReached) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchItems(reset = false)
            _isLoadingMore.value = false
        }
    }

    /** Tải danh mục, không làm hỏng danh sách items đang có */
    private suspend fun fetchCategories() {
        when (val categories = categoryRepository.getCategories()) {
            is ApiResult.Success -> {
                val prev = _homeState.value as? HomeState.Success
                _homeState.value = HomeState.Success(
                    categories = categories.data ?: emptyList(),
                    popularItems = prev?.popularItems ?: emptyList(),
                    currentPage = prev?.currentPage ?: 0,
                    totalPages = prev?.totalPages ?: 0,
                    isLastPage = prev?.isLastPage ?: false,
                    totalElements = prev?.totalElements ?: 0
                )
            }
            is ApiResult.Error -> {
                _homeState.value = HomeState.Error(categories.error.message ?: "Lỗi khi tải danh mục.")
                ErrorPopupManager.showError(categories.error.message, false)
            }
        }
    }

    /** Tải items theo trang. reset=true thì thay thế danh sách, ngược lại nối thêm */
    private suspend fun fetchItems(reset: Boolean) {
        when (val items = itemRepository.getAvailableItem(page = currentPage)) {
            is ApiResult.Success -> {
                val content = items.data?.content ?: emptyList()
                if (content.isEmpty()) {
                    endReached = true
                }
                val prev = _homeState.value as? HomeState.Success
                val merged = if (reset) content else prev?.popularItems.orEmpty() + content

                _homeState.value = HomeState.Success(
                    categories = prev?.categories
                        ?: ( _homeState.value as? HomeState.Success )?.categories
                        ?: emptyList(),
                    popularItems = merged,
                    currentPage = currentPage,
                    totalPages = prev?.totalPages ?: 0, // nếu server không trả meta có thể để 0
                    isLastPage = endReached,
                    totalElements = merged.size.toLong()
                )

                if (content.isNotEmpty()) {
                    currentPage += 1
                }
            }
            is ApiResult.Error -> {
                _homeState.value = HomeState.Error(items.error.message ?: "Lỗi khi tải sản phẩm.")
                ErrorPopupManager.showError(items.error.message, false)
            }
        }
    }
}
