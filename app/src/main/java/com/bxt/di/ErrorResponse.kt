package com.bxt.di

enum class ErrorType {
    NETWORK, UNAUTHORIZED, FORBIDDEN, SERVER,
    INVALID_CREDENTIALS, SESSION_EXPIRED, UNKNOWN, TIMEOUT,
    NOT_FOUND, CONFLICT, BAD_REQUEST
}

data class ErrorResponse(
    val message: String,
    val canRetry: Boolean,
    val type: ErrorType,
    val code: Int? = null,
    val errorCode: String? = null
)