package com.bxt.data.repository

import com.bxt.data.api.dto.response.NotificationResponse
import com.bxt.di.ApiResult

interface NotificationRepository {
    suspend fun getAllNotifications(): ApiResult<List<NotificationResponse>>
    suspend fun markAsRead(notificationId: Long): ApiResult<Unit>
    suspend fun markAllAsRead(): ApiResult<Unit>
}