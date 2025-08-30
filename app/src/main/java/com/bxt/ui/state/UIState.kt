package com.bxt.ui.state

import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.di.ErrorResponse
import com.bxt.viewmodel.ChatThreadUi
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction

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
        val categories: List<CategoryResponse> = emptyList(),
        val popularItems: List<ItemResponse> = emptyList(),
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val isLastPage: Boolean = false,
        val totalElements: Long = 0
    ) : HomeState()

    data class Error(
        val message: String
    ) : HomeState()
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
    data class Success(val data: ItemDetail) : ItemState
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

sealed interface RentalRequestsState {
    data object Loading : RentalRequestsState
    data class Success(val data: List<RentalRequestResponse>) : RentalRequestsState
    data class Error(val message: ErrorResponse) : RentalRequestsState
}

sealed interface TransportServiceListState {
    data object Loading : TransportServiceListState
    data class Success(val services: List<TransportServiceResponse>) : TransportServiceListState
    data class Error(val message: String) : TransportServiceListState
}

sealed interface AddTransportServiceState {
    data object Idle : AddTransportServiceState
    data object Submitting : AddTransportServiceState
    data class Success(val message: String) : AddTransportServiceState
    data class Error(val message: String) : AddTransportServiceState
}

data class LocationPickerState(
    val searchQuery: String = "",
    val predictions: List<AutocompletePrediction> = emptyList(),
    val selectedLocation: LatLng? = null
)


sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryResponse>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Success(val threads: List<ChatThreadUi>) : ChatListUiState
    data class Error(val message: String) : ChatListUiState
}