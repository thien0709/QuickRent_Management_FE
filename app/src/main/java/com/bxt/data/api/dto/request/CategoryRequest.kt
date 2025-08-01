package com.bxt.data.api.dto.request

import okhttp3.MultipartBody
import okhttp3.RequestBody

data class CategoryRequest(
    val name: RequestBody,
    val description: RequestBody,
    val imageFile: MultipartBody.Part
)
