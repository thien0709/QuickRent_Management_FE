package com.bxt.data.api.dto.request

data class RegisterTokenRequest(
    val fcmToken: String,
    val deviceInfo: String,
    val userId: Long? = null  // khi đã có auth, có thể bỏ field này
)
