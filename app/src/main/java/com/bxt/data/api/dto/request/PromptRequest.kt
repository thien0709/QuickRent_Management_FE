package com.bxt.data.api.dto.request

import com.google.gson.annotations.SerializedName

data class PromptRequest(
    @SerializedName("prompt")
    val prompt: String
)