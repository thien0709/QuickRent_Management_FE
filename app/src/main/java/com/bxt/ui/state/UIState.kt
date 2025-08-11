package com.bxt.ui.state

import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.PagedResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val loginResponse: LoginResponse) : LoginState()
    data class Error(val error: String) : LoginState()
}

sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val categories: List<CategoryResponse>,
        val popularItems: List<ItemResponse>
    ) : HomeState()

    data class Error(val error: String) : HomeState()
}


data class UserState(
    val user: ApiResult<UserResponse>? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val shouldNavigateToLogin: Boolean = false
)

sealed class CategoryState {
    object Loading : CategoryState()
    data class Success(
        val items: List<ItemResponse>,
        val isLoadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null
    ) : CategoryState()
    data class Error(val error: String) : CategoryState()
}