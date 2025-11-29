package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.R
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
    val imageRes: Int
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
            imageRes = R.drawable.ic_launcher_logo,
            title = "Welcome to QuickRent",
            description = "A shared-economy platform where you can rent items and share rides to save money every day."
        ),
        OnboardingPage(
            imageRes = R.drawable.ic_launcher_foreground,
            title = "Share idle items, earn extra income",
            description = "List cameras, laptops, travel gear and more when you don’t use them, and turn idle things into extra money."
        ),
        OnboardingPage(
            imageRes = R.drawable.ic_map_to,
            title = "Rent what you need, only when you need it",
            description = "Find nearby items for short-term use with clear prices and secure deposits, so you only pay for the time you really need."
        ),
        OnboardingPage(
            imageRes = R.drawable.ic_launcher_foreground,
            title = "Share rides, save money and emissions",
            description = "Match with people going the same way to split fuel costs, reduce traffic and travel more sustainably."
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