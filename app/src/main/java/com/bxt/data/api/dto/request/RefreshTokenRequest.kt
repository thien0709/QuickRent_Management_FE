// File: com/bxt/data/api/dto/request/RefreshTokenRequest.kt
package com.bxt.data.api.dto.request

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(

    @SerializedName("refreshToken")
    val refreshToken: String

)