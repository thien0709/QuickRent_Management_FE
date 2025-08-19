// File: com/bxt/data/api/ApiCallExecutor.kt

package com.bxt.data.api

import com.bxt.di.ApiResult
import com.bxt.di.ErrorParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCallExecutor @Inject constructor(
    private val errorParser: ErrorParser
) {
    suspend fun <T> execute(apiCall: suspend () -> T): ApiResult<T> {
        return try {
            val result = apiCall()
            ApiResult.Success(result)
        } catch (e: Exception) {
            ApiResult.Error(errorParser.parseException(e))
        }
    }
}