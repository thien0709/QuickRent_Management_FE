// File: com/bxt/di/ApiResult.kt (hoặc thêm vào cuối file ErrorResponse.kt)
package com.bxt.di

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val error: ErrorResponse) : ApiResult<Nothing>()
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    inline fun onError(action: (ErrorResponse) -> Unit): ApiResult<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }
}