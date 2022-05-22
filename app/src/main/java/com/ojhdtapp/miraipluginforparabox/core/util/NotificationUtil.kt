package com.ojhdtapp.miraipluginforparabox.core.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ojhdtapp.miraipluginforparabox.R

class NotificationUtil(val context: Service) {
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
    }


    fun createNotificationChannel(channelName: String, channelDescription: String) {
        val channel = NotificationChannel(NotificationUtil.FOREGROUND_SERVICE_CHANNEL_ID,
        channelName,
        NotificationManager.IMPORTANCE_LOW).apply {
            description = channelDescription
        }
        notificationManager.createNotificationChannel(channel)
    }

    inline fun <reified T> startForegroundService() {
        val pendingIntent: PendingIntent = Intent(context, T::class.java).let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        createNotificationChannel("服务状态", "服务状态")
        val notification: Notification = Notification.Builder(context, NotificationUtil.FOREGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("服务正在启动")
            .setContentText("尝试以默认账户登录")
            .setContentIntent(pendingIntent)
            .build()
        context.startForeground(NotificationUtil.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }
}