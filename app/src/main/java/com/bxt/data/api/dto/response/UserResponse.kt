package com.bxt.data.api.dto.response

import java.math.BigDecimal

data class UserResponse(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val avatarUrl: String? = null,
    val role: String? = null,
    val createdAt: String? = null,
    val isActive: Boolean? = null
)
