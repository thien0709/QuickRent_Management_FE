package com.bxt.di

import android.content.Context
import com.bxt.R
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ErrorParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson // <-- inject luôn Gson qua Hilt cho test dễ hơn
) {
    private data class BackendErrorResponse(
        val errorCode: String?,
        val message: String?
    )

    fun parseException(exception: Exception): ErrorResponse {
        if (exception is CancellationException) throw exception

        return when (exception) {
            is HttpException -> parseHttpError(exception.response()?.errorBody(), exception.code())
            is SocketTimeoutException -> finalError(
                msgRes = R.string.error_timeout,
                type = ErrorType.TIMEOUT,
                canRetry = true
            )
            is UnknownHostException -> finalError(
                msgRes = R.string.error_network,
                type = ErrorType.NETWORK,
                canRetry = true
            )
            is ConnectException -> finalError(
                msgRes = R.string.error_network,
                type = ErrorType.NETWORK,
                canRetry = true
            )
            is SSLHandshakeException -> finalError(
                msgRes = R.string.error_network,
                type = ErrorType.NETWORK,
                canRetry = false
            )
            is IOException -> finalError(
                msgRes = R.string.error_network,
                type = ErrorType.NETWORK,
                canRetry = true
            )
            else -> finalError(
                msgRes = R.string.error_unknown,
                type = ErrorType.UNKNOWN,
                canRetry = false
            )
        }
    }

    private fun parseHttpError(errorBody: ResponseBody?, httpStatus: Int): ErrorResponse {
        val (msgRes, type) = mapHttpStatusCodeToAppError(httpStatus)
        val canRetry = httpStatus == 408 || httpStatus in 500..599

        val raw = try { errorBody?.string() } catch (_: Exception) { null }
        val be: BackendErrorResponse? = try {
            if (!raw.isNullOrBlank()) gson.fromJson(raw, BackendErrorResponse::class.java) else null
        } catch (_: Exception) { null }

        val displayMsg = when {
            !be?.message.isNullOrBlank() -> be!!.message!! // ưu tiên message backend
            else -> context.getString(msgRes)              // fallback sang resource
        }

        val (overrideRes, overrideType) =
            mapErrorCodeToAppError(be?.errorCode) ?: (msgRes to type)


        return ErrorResponse(
            message = displayMsg,
            canRetry = canRetry,
            type = overrideType,
            code = httpStatus,
            errorCode = be?.errorCode
        )
    }

    private fun mapErrorCodeToAppError(errorCode: String?): Pair<Int, ErrorType>? = when (errorCode) {
        "BAD_REQUEST_001" -> R.string.error_bad_request to ErrorType.BAD_REQUEST
        "AUTH_001"        -> R.string.error_invalid_credentials to ErrorType.INVALID_CREDENTIALS
        "AUTH_004"        -> R.string.error_session_expired to ErrorType.SESSION_EXPIRED
        "AUTH_002"        -> R.string.error_account_blocked to ErrorType.FORBIDDEN
        "AUTH_003"        -> R.string.error_forbidden to ErrorType.FORBIDDEN
        "NOT_FOUND_001"   -> R.string.error_user_not_found to ErrorType.NOT_FOUND
        "NOT_FOUND_002"   -> R.string.error_item_not_found to ErrorType.NOT_FOUND
        "CONFLICT_001"    -> R.string.error_username_existed to ErrorType.CONFLICT
        "CONFLICT_002"    -> R.string.error_email_existed to ErrorType.CONFLICT
        "SERVER_001", "SERVER_002", "SERVER_003" -> R.string.error_server to ErrorType.SERVER
        "SERVER_999"      -> R.string.error_unknown to ErrorType.UNKNOWN
        else -> null
    }

    private fun mapHttpStatusCodeToAppError(httpStatus: Int): Pair<Int, ErrorType> = when (httpStatus) {
        400 -> R.string.error_bad_request to ErrorType.BAD_REQUEST
        401 -> R.string.error_session_expired to ErrorType.UNAUTHORIZED
        403 -> R.string.error_forbidden to ErrorType.FORBIDDEN
        404 -> R.string.error_item_not_found to ErrorType.NOT_FOUND
        408 -> R.string.error_timeout to ErrorType.TIMEOUT
        409 -> R.string.error_conflict to ErrorType.CONFLICT
        in 500..599 -> R.string.error_server to ErrorType.SERVER
        else -> R.string.error_unknown to ErrorType.UNKNOWN
    }

    private fun finalError(
        msgRes: Int,
        type: ErrorType,
        canRetry: Boolean,
        httpCode: Int? = null,
        errorCode: String? = null
    ) = ErrorResponse(
        message = context.getString(msgRes),
        canRetry = canRetry,
        type = type,
        code = httpCode,
        errorCode = errorCode
    )
}
