package com.bxt.data.api.dto.response;



data class LoginResponse(
    val token: String,
    val username: String,
    val role: String
)