package com.bxt.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CH_CHAT = "chat"
    const val CH_ORDER = "order"
    const val CH_SYSTEM = "system"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)

            val chat = NotificationChannel(
                CH_CHAT, "Chat", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Tin nhắn, cuộc trò chuyện" }

            val order = NotificationChannel(
                CH_ORDER, "Đơn hàng / Thuê vận chuyển", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Cập nhật yêu cầu thuê, vận chuyển, thanh toán" }

            val system = NotificationChannel(
                CH_SYSTEM, "Hệ thống", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Thông báo hệ thống chung" }

            nm.createNotificationChannel(chat)
            nm.createNotificationChannel(order)
            nm.createNotificationChannel(system)
        }
    }
}
