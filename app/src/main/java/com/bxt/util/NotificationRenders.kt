package com.bxt.util

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bxt.MainActivity
import com.bxt.R
import androidx.core.net.toUri

object NotificationRenderer {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(
        ctx: Context,
        channelId: String,
        title: String,
        body: String?,
        referenceType: String?,
        referenceId: Long?
    ) {
        val route = buildRoute(referenceType, referenceId)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("deeplink_route", route)
            data = buildUri(referenceType, referenceId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getActivity(ctx, route.hashCode(), intent, flags)

        val builder = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (title.isBlank()) "QuickRent" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == NotificationHelper.CH_CHAT)
                    NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )

        NotificationManagerCompat.from(ctx)
            .notify((route + System.currentTimeMillis()).hashCode(), builder.build())
    }

    private fun buildRoute(type: String?, id: Long?): String {
        return when (type) {
            "ITEM"        -> if (id != null) "item_detail/$id"        else "home"
            "RENTAL"      -> if (id != null) "transaction_detail/$id" else "rental_service"
            "TRANSACTION" -> if (id != null) "transaction_detail/$id" else "rental_service"
            "TRANSPORT"   -> if (id != null) "transport_detail/$id"   else "transport_service"
            "USER"        -> if (id != null) "chat_screen/$id"        else "chat_list"
            else          -> "home"
        }
    }

    private fun buildUri(type: String?, id: Long?): Uri {
        val path = buildRoute(type, id)
        return "quickrent://$path".toUri()
    }
}
