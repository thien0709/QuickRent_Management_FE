package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int? = null
)

data class WelcomeUiState(
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val pages: List<OnboardingPage> = emptyList()
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    private val onboardingPages = listOf(
        OnboardingPage(
            title = "Chào mừng đến với BXT App",
            description = "Ứng dụng giúp bạn quản lý công việc một cách hiệu quả và dễ dàng"
        ),
        OnboardingPage(
            title = "Quản lý dễ dàng",
            description = "Theo dõi tiến độ công việc, đặt mục tiêu và hoàn thành nhiệm vụ một cách khoa học"
        ),
        OnboardingPage(
            title = "Đồng bộ mọi lúc",
            description = "Dữ liệu của bạn được đồng bộ và bảo mật trên tất cả thiết bị"
        ),
        OnboardingPage(
            title = "Bắt đầu ngay",
            description = "Hãy tạo tài khoản hoặc đăng nhập để bắt đầu trải nghiệm"
        )
    )

    init {
        _uiState.value = _uiState.value.copy(
            pages = onboardingPages,
            isLastPage = false
        )
    }

    fun nextPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage < onboardingPages.size - 1) {
            _uiState.value = _uiState.value.copy(
                currentPage = currentPage + 1,
                isLastPage = currentPage + 1 == onboardingPages.size - 1
            )
        }
    }

    fun previousPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage > 0) {
            _uiState.value = _uiState.value.copy(
                currentPage = currentPage - 1,
                isLastPage = false
            )
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            dataStoreManager.setFirstTimeCompleted()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStoreManager.setFirstTimeCompleted()
        }
    }
}