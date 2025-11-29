package com.bxt.fcm

import android.Manifest
import androidx.annotation.RequiresPermission
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.FcmRepository
import com.bxt.util.NotificationHelper
import com.bxt.util.NotificationRenderer
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var repo: FcmRepository
    @Inject lateinit var dataStore: DataStoreManager
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            val userId = dataStore.userId.firstOrNull() ?: return@launch
            runCatching { repo.registerToken(token, userId) }
        }
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data

        val type  = data["notification_type"]
        val title = data["title"] ?: message.notification?.title ?: "QuickRent"
        val body  = data["message"] ?: message.notification?.body
        val refType = data["reference_type"]
        val refId   = data["reference_id"]?.toLongOrNull()

        val channelId = when (type) {
            "CHAT" -> NotificationHelper.CH_CHAT
            "PAYMENT", "RENTAL_REQUEST", "RENTAL_UPDATE", "TRANSPORT_REQUEST", "TRANSPORT_UPDATE" ->
                NotificationHelper.CH_ORDER
            else -> NotificationHelper.CH_SYSTEM
        }

        NotificationRenderer.show(
            ctx = this,
            channelId = channelId,
            title = title ?: "QuickRent",
            body = body,
            referenceType = refType,
            referenceId = refId
        )
    }
}
