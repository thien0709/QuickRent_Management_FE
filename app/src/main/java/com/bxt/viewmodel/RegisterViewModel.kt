package com.bxt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.RegisterRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.AuthRepository
import com.bxt.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bxt.di.ApiResult
import com.bxt.ui.state.RegisterState

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    var username by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var fullName by mutableStateOf("")
        private set
    var phoneNumber by mutableStateOf("")
        private set
    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var uiState by mutableStateOf<RegisterState>(RegisterState.Idle)
        private set

    fun onAvatarChanged(uri: Uri?) {
        avatarUri = uri
    }

    fun onUsernameChanged(value: String) { username = value }
    fun onEmailChanged(value: String) { email = value }
    fun onPasswordChanged(value: String) { password = value }
    fun onFullNameChanged(value: String) { fullName = value }
    fun onPhoneNumberChanged(value: String) { phoneNumber = value }

    fun onRegisterClick(context: Context) {
        // 1) Debounce
        if (isLoading) return

        viewModelScope.launch {
            // 2) Validate cơ bản
            val name = username.trim()
            val mail = email.trim()
            val pass = password.trim()
            val full = fullName.trim()
            val phone = phoneNumber.trim()

            if (avatarUri == null) {
                val msg = "Vui lòng chọn ảnh đại diện trước khi đăng ký."
                errorMessage = msg
                uiState = RegisterState.Error(msg)
                return@launch
            }
            if (name.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
                val msg = "Tên đăng nhập, email và mật khẩu không được để trống."
                errorMessage = msg
                uiState = RegisterState.Error(msg)
                return@launch
            }
            // TODO: kiểm tra định dạng email, độ dài password nếu cần

            isLoading = true
            errorMessage = null
            uiState = RegisterState.Loading

            try {
                val avatarPart = FileUtils.uriToMultipart(context, avatarUri, "avatar")
                if (avatarPart == null) {
                    val msg = "Không thể đọc file ảnh. Vui lòng chọn lại."
                    errorMessage = msg
                    uiState = RegisterState.Error(msg)
                    return@launch
                }

                val request = RegisterRequest(
                    username = name,
                    email = mail,
                    password = pass,
                    fullName = full,
                    phoneNumber = phone,
                )

                when (val res = authRepository.register(request, avatarPart)) {
                    is ApiResult.Success -> {
                        // Ví dụ: lưu token/user nếu response có
                        // dataStoreManager.saveToken(res.data.token)
                        uiState = RegisterState.Success
                    }
                    is ApiResult.Error -> {
                        val msg = res.error?.toString()
                            ?: "Đăng ký thất bại"
                        errorMessage = msg
                        uiState = RegisterState.Error(msg)
                    }
                    // Nếu ApiResult là sealed và chỉ có 2 case thì KHÔNG cần else
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Có lỗi xảy ra"
                errorMessage = msg
                uiState = RegisterState.Error(msg)
            } finally {
                isLoading = false
            }
        }
    }


    fun resetState() {
        errorMessage = null
        uiState = RegisterState.Idle
    }
}
