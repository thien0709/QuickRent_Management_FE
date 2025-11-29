package com.bxt.data.api.dto.response


data class NotificationResponse(
    val id: Long,
    val notificationType: String,
    val title: String,
    val message: String?,
    val referenceType: String?,
    val referenceId: Long?,
    val isRead: Boolean,
    val createdAt: String
)