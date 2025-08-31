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
            title = "Welcome to BXT App",
            description = "An app that helps you manage your work efficiently and effortlessly."
        ),
        OnboardingPage(
            title = "Effortless Management",
            description = "Track progress, set goals, and complete tasks in a smart, organized way."
        ),
        OnboardingPage(
            title = "Sync Everywhere",
            description = "Your data stays synced and secure across all your devices."
        ),
        OnboardingPage(
            title = "Get Started",
            description = "Create an account or sign in to begin your experience."
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