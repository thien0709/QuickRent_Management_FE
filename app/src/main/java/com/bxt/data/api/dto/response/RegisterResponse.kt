package com.bxt.data.api.dto.response;

import java.math.BigDecimal

data class RegisterResponse(
    val id: Long?,
    val username: String?,
    val email: String?,
    val fullName: String?,
    val phoneNumber: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val avatarUrl: String?,
    val role: String?,
    val createdAt: String?,
    val isActive: Boolean?
)