// bxt/viewmodel/UserViewModel.kt
package com.bxt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.UpdateProfileRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.UpdateProfileState
import com.bxt.ui.state.UserState
import com.bxt.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userProfileState = MutableStateFlow(UserState(isLoading = true))
    val userProfileState: StateFlow<UserState> = _userProfileState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateProfileState())
    val updateState: StateFlow<UpdateProfileState> = _updateState.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            _userProfileState.value = UserState(isLoading = true)
            try {
                if (dataStoreManager.accessToken.first().isNullOrEmpty()) {
                    _userProfileState.value = UserState(isLoading = false, shouldNavigateToLogin = true)
                    return@launch
                }
                val result = userRepository.getUserInfo()
                _userProfileState.value = UserState(isLoading = false, user = result, error = if (result is ApiResult.Error) result.error.message else null)
            } catch (e: Exception) {
                _userProfileState.value = UserState(isLoading = false, error = "Lỗi khi tải thông tin: ${e.message}")
            }
        }
    }

    fun updateUserProfile(request: UpdateProfileRequest, avatarUri: Uri?) {
        viewModelScope.launch {
            _updateState.value = UpdateProfileState(isLoading = true)
            try {
                val avatarPart: MultipartBody.Part? = avatarUri?.let {
                    // ✅ SỬA LỖI: Đổi partName thành "avatar"
                    FileUtils.uriToMultipart(context, it, "avatar")
                }

                when (val result = userRepository.updateUserInfo(request, avatarPart)) {
                    is ApiResult.Success -> {
                        _updateState.value = UpdateProfileState(isLoading = false, isSuccess = true)
                        // ✅ SỬA LỖI: Gọi fetchUserProfile() BÊN TRONG khối success
                        fetchUserProfile()
                    }
                    is ApiResult.Error -> {
                        _updateState.value = UpdateProfileState(isLoading = false, error = result.error.message)
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateProfileState(isLoading = false, error = "Lỗi không xác định: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearAll()
            _userProfileState.value = UserState(shouldNavigateToLogin = true)
        }
    }

    fun onNavigationHandled() {
        _userProfileState.value = _userProfileState.value.copy(shouldNavigateToLogin = false)
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileState()
    }
}