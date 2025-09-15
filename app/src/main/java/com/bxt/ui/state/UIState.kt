package com.bxt.ui.state

import com.bxt.data.api.dto.response.CategoryResponse
import com.bxt.data.api.dto.response.ItemDetail
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.LoginResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.data.api.dto.response.UserResponse
import com.bxt.di.ApiResult
import com.bxt.viewmodel.Capabilities
import com.bxt.viewmodel.ChatThreadUi
import com.bxt.viewmodel.FullRentalDetails
import com.bxt.viewmodel.FullTransportDetails
import com.bxt.viewmodel.TransportTransactionDetails

//import com.google.android.gms.maps.model.LatLng
//import com.google.android.libraries.places.api.model.AutocompletePrediction

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
    val shouldNavigateToLogin: Boolean = false,
    val updateSuccess: Boolean = false
)

data class UpdateProfileState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

sealed interface CategoryState {
    object Loading : CategoryState
    data class Error(val message: String) : CategoryState
    data class Success(
        val categories: List<CategoryResponse>,
        val products: List<ItemResponse>,
        val selectedCategory: CategoryResponse?,
        val isLoadingProducts: Boolean = false
    ) : CategoryState
}

sealed class LocationState {
    data object Loading : LocationState()

    data class Success(
        val location: Pair<Double, Double>? = null,
        val address: String? = null
    ) : LocationState()

    data class Error(
        val message: String,
        val location: Pair<Double, Double>? = null
    ) : LocationState()

    data class PermissionRequired(val shouldShowRationale: Boolean) : LocationState()
    data class GpsDisabled(val shouldShowRationale: Boolean) : LocationState()
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

sealed class RentalServiceState {
    data object Loading : RentalServiceState()
    data class Success(val requests: List<RentalRequestResponse> = emptyList()) : RentalServiceState()
    data class Error(val message: String) : RentalServiceState()
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

//data class LocationPickerState(
//    val searchQuery: String = "",
//    val predictions: List<AutocompletePrediction> = emptyList(),
//    val selectedLocation: LatLng? = null
//)


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

sealed interface ItemDataState {
    object Loading : ItemDataState
    data class Success(val item: ItemResponse) : ItemDataState
    data class Error(val message: String) : ItemDataState
}

sealed interface TransactionDetailState {
    object Loading : TransactionDetailState
    data class Success(val details: FullRentalDetails, val caps: Capabilities) :
        TransactionDetailState

    data class Error(val message: String) : TransactionDetailState
}


sealed interface TransportDetailState {
    object Loading : TransportDetailState
    data class Success(val details: FullTransportDetails) : TransportDetailState
    data class Error(val message: String) : TransportDetailState
}

sealed class TransportTransactionState {
    object Loading : TransportTransactionState()
    data class Success(val details: TransportTransactionDetails) : TransportTransactionState()
    data class Error(val message: String) : TransportTransactionState()
}