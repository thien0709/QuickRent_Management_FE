package com.bxt.di

import android.content.Context
import com.bxt.R // Hãy đảm bảo bạn đã import R từ đúng package của project
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorParser @Inject constructor(
     @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private data class BackendErrorResponse(
        val errorCode: String?,
        val message: String?
    )

    fun parseException(exception: Exception): ErrorResponse {
        return when (exception) {
            is HttpException -> parseHttpError(exception.response()?.errorBody(), exception.code())
            is SocketTimeoutException -> createFinalError(R.string.error_timeout, ErrorType.TIMEOUT, canRetry = true)
            is IOException -> createFinalError(R.string.error_network, ErrorType.NETWORK, canRetry = true)
            else -> createFinalError(R.string.error_unknown, ErrorType.UNKNOWN, canRetry = false)
        }
    }

    private fun parseHttpError(errorBody: ResponseBody?, httpStatusCode: Int): ErrorResponse {
        return try {
            val backendError = gson.fromJson(errorBody?.string(), BackendErrorResponse::class.java)
            val errorCode = backendError.errorCode

            val (stringResId, errorType) = mapErrorCodeToAppError(errorCode)
                ?: mapHttpStatusCodeToAppError(httpStatusCode)

            val canRetry = httpStatusCode in 500..599

            createFinalError(stringResId, errorType, httpStatusCode, errorCode, canRetry)

        } catch (e: Exception) {
            val (stringResId, errorType) = mapHttpStatusCodeToAppError(httpStatusCode)
            val canRetry = httpStatusCode in 500..599
            createFinalError(stringResId, errorType, httpStatusCode, canRetry = canRetry)
        }
    }

    private fun mapErrorCodeToAppError(errorCode: String?): Pair<Int, ErrorType>? {
        return when (errorCode) {
            "BAD_REQUEST_001" -> R.string.error_bad_request to ErrorType.BAD_REQUEST
            "AUTH_001" -> R.string.error_invalid_credentials to ErrorType.INVALID_CREDENTIALS
            "AUTH_004" -> R.string.error_session_expired to ErrorType.SESSION_EXPIRED
            "AUTH_002" -> R.string.error_account_blocked to ErrorType.FORBIDDEN
            "AUTH_003" -> R.string.error_forbidden to ErrorType.FORBIDDEN
            "NOT_FOUND_001" -> R.string.error_user_not_found to ErrorType.NOT_FOUND
            "NOT_FOUND_002" -> R.string.error_item_not_found to ErrorType.NOT_FOUND
            "CONFLICT_001" -> R.string.error_username_existed to ErrorType.CONFLICT
            "CONFLICT_002" -> R.string.error_email_existed to ErrorType.CONFLICT

            "SERVER_001", "SERVER_002", "SERVER_003" -> R.string.error_server to ErrorType.SERVER
            "SERVER_999" -> R.string.error_unknown to ErrorType.UNKNOWN

            else -> null // Trả về null nếu không có errorCode nào khớp
        }
    }

    private fun mapHttpStatusCodeToAppError(httpStatusCode: Int): Pair<Int, ErrorType> {
        return when (httpStatusCode) {
            400 -> R.string.error_bad_request to ErrorType.BAD_REQUEST
            401 -> R.string.error_session_expired to ErrorType.UNAUTHORIZED
            403 -> R.string.error_forbidden to ErrorType.FORBIDDEN
            404 -> R.string.error_item_not_found to ErrorType.NOT_FOUND
            409 -> R.string.error_conflict to ErrorType.CONFLICT
            in 500..599 -> R.string.error_server to ErrorType.SERVER
            else -> R.string.error_unknown to ErrorType.UNKNOWN
        }
    }

    private fun createFinalError(
        stringResId: Int,
        type: ErrorType,
        httpCode: Int? = null,
        errorCode: String? = null,
        canRetry: Boolean = false
    ): ErrorResponse {
        return ErrorResponse(
            message = context.getString(stringResId),
            canRetry = canRetry,
            type = type,
            code = httpCode,
            errorCode = errorCode
        )
    }
}