package com.bxt.data.api.dto.request


data class UpdateProfileRequest(
    val username: String,
    val email: String,
    val fullName: String,
    val phone: String,
)