package com.bxt.data.api.dto.response;



data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val role: String
)