package com.bxt.di

enum class ErrorType {
    NETWORK, UNAUTHORIZED, FORBIDDEN, SERVER,
    INVALID_CREDENTIALS, SESSION_EXPIRED, UNKNOWN, TIMEOUT,
    NOT_FOUND, CONFLICT, BAD_REQUEST
}

data class ErrorResponse(
    val message: String,          // Message thân thiện, lấy từ errors.xml
    val canRetry: Boolean,
    val type: ErrorType,
    val code: Int? = null,        // HTTP status code (ví dụ: 404, 500)
    val errorCode: String? = null // **Thêm trường này** để chứa mã lỗi từ BE (ví dụ: "ITEM_NOT_FOUND")
)