package com.bxt.di

enum class ErrorType {
    NETWORK,             // Lỗi mạng
    UNAUTHORIZED,        // Lỗi 401
    FORBIDDEN,           // Lỗi 403
    SERVER,              // Lỗi 500-599
    INVALID_CREDENTIALS, // Lỗi thông tin đăng nhập không hợp lệ
    SESSION_EXPIRED,     // Lỗi validate
    UNKNOWN,             // Lỗi không xác định
    TIMEOUT,             // Lỗi kết nối timeout
    NOT_FOUND,           // Lỗi không tìm thấy
    CONFLICT             // Lỗi xung đột (ví dụ trùng tên đăng nhập)
}

data class ErrorResponse(
    val message: String,
    val canRetry: Boolean,
    val type: ErrorType,
    val code: Int? = null
)


sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val error: ErrorResponse) : ApiResult<Nothing>()

    fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (ErrorResponse) -> Unit): ApiResult<T> {
        if (this is Error) action(error)
        return this
    }
}
class CustomApiException(val error: ErrorResponse) : Exception(error.message)
