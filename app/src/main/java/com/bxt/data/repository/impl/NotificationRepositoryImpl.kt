package com.bxt.data.repository.impl

import com.bxt.data.api.ApiCallExecutor
import com.bxt.data.api.ApiService
import com.bxt.data.api.dto.response.NotificationResponse
import com.bxt.data.repository.NotificationRepository
import com.bxt.di.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val apiCallExecutor: ApiCallExecutor
) : NotificationRepository {

    override suspend fun getAllNotifications(): ApiResult<List<NotificationResponse>> {
        return apiCallExecutor.execute {
            apiService.getNotifications()
        }
    }

    override suspend fun markAsRead(notificationId: Long): ApiResult<Unit> {
        return apiCallExecutor.execute {
            apiService.markNotificationAsRead(notificationId)
        }
    }

    override suspend fun markAllAsRead(): ApiResult<Unit> {
        return apiCallExecutor.execute {
            apiService.markAllNotificationsAsRead()
        }
    }
}