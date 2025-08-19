package com.bxt.ui.state

import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemDetail
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

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val error: String) : RegisterState()
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
    data class Error(val message: String) : CategoryState()
    data class Success(
        val categories: List<CategoryResponse>,
        val products: List<ItemResponse> = emptyList(),
        val selectedCategory: CategoryResponse? = null,
        val isLoadingProducts: Boolean = false
    ) : CategoryState()
}

sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: Pair<Double, Double>, val address: String? = null) : LocationState()
    data class Error(val message: String, val location: Pair<Double, Double>) : LocationState()
    data class PermissionRequired(val shouldShowRationale: Boolean) : LocationState()
    data class GpsDisabled(val isEnabled: Boolean) : LocationState()
}
sealed interface ItemState {
    data object Loading : ItemState
    data class Error(val message: String?) : ItemState
    data class Success(val data: ItemResponse) : ItemState
}

sealed interface AddItemState {
    data object Idle : AddItemState
    data object Submitting : AddItemState
    data class Uploading(val uploaded: Int, val total: Int) : AddItemState
    data class Success(val data: ItemResponse, val warning: String? = null) : AddItemState
    data class Error(val message: String) : AddItemState
}


sealed interface RentalState {
    data object Idle : RentalState
    data object Submitting : RentalState
    data class Success(val id: Long? = null) : RentalState
    data class Error(val message: String) : RentalState
}