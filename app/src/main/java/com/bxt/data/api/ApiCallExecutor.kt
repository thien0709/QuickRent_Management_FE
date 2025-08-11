package com.bxt.data.api

import com.bxt.di.*
import com.bxt.ui.screen.ErrorPopupManager
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ApiCallExecutor {
    suspend fun <T> execute(
        showErrorPopup: Boolean = true,
        retryAction: (() -> Unit)? = null,
        apiCall: suspend () -> T
    ): ApiResult<T> {
        return try {
            ApiResult.Success(apiCall())
        } catch (e: CustomApiException) {
            if (showErrorPopup) ErrorPopupManager.showError(e.error.message, e.error.canRetry, retryAction)
            ApiResult.Error(e.error)
        } catch (e: Exception) {
            val error = ErrorParser.parseException(e)
            if (showErrorPopup) ErrorPopupManager.showError(error.message, error.canRetry, retryAction)
            ApiResult.Error(error)
        }
    }
}
