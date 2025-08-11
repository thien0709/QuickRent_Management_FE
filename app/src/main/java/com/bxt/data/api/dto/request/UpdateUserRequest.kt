package com.bxt.data.api.dto.request

data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
)