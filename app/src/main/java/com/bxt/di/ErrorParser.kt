package com.bxt.di

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
object ErrorParser {
    private val gson = Gson()

    data class BackendErrorResponse(
        val timestamp: String?,
        val status: Int?,
        val error: String?,
        val message: String?,
        val path: String?
    )

    fun parseException(e: Exception): ErrorResponse {
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()
                parseHttpError(errorBody, e.code())
            }
            is SocketTimeoutException -> ErrorResponse(
                message = "Kết nối quá thời gian chờ. Vui lòng thử lại.",
                canRetry = true,
                type = ErrorType.TIMEOUT
            )
            is IOException -> ErrorResponse(
                message = "Không có kết nối mạng. Vui lòng kiểm tra lại.",
                canRetry = true,
                type = ErrorType.NETWORK
            )
            else -> ErrorResponse(
                message = e.localizedMessage ?: "Lỗi không xác định",
                canRetry = false,
                type = ErrorType.UNKNOWN
            )
        }
    }


    private fun parseHttpError(errorBody: ResponseBody?, httpStatusCode: Int): ErrorResponse {
        return try {
            val json = errorBody?.string()
            val backendError = gson.fromJson(json, BackendErrorResponse::class.java)

            // Map mã lỗi backend sang kiểu lỗi client
            val type = when (backendError.error) {
                "INVALID_CREDENTIALS" -> ErrorType.INVALID_CREDENTIALS
                "PASS_NOT_TRUE" -> ErrorType.INVALID_CREDENTIALS
                "INSUFFICIENT_PERMISSIONS" -> ErrorType.FORBIDDEN
                "ACCOUNT_BLOCKED" -> ErrorType.FORBIDDEN
                "USER_NOT_FOUND" -> ErrorType.NOT_FOUND
                "ITEM_NOT_FOUND" -> ErrorType.NOT_FOUND
                "USERNAME_EXISTED" -> ErrorType.CONFLICT
                "EMAIL_EXISTED" -> ErrorType.CONFLICT
                "TOKEN_GENERATION_ERROR" -> ErrorType.UNAUTHORIZED
                "TOKEN_VALIDATION_ERROR" -> ErrorType.UNAUTHORIZED
                else -> when (httpStatusCode) {
                    401 -> ErrorType.UNAUTHORIZED
                    403 -> ErrorType.FORBIDDEN
                    in 500..599 -> ErrorType.SERVER
                    else -> ErrorType.UNKNOWN
                }
            }

            ErrorResponse(
                message = backendError.message ?: "Lỗi máy chủ",
                canRetry = httpStatusCode in 500..599,
                type = type,
                code = backendError.status ?: httpStatusCode
            )
        } catch (ex: Exception) {
            ErrorResponse(
                message = "Lỗi máy chủ",
                canRetry = httpStatusCode in 500..599,
                type = if (httpStatusCode in 500..599) ErrorType.SERVER else ErrorType.UNKNOWN,
                code = httpStatusCode
            )
        }
    }
}
