package com.bxt.data.api.dto.response

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("model")
    val model: String?
)