package com.ojhdtapp.miraipluginforparabox.core.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ojhdtapp.miraipluginforparabox.MainActivity
import com.ojhdtapp.miraipluginforparabox.R

class NotificationUtil(val context: Service) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
    }


    fun createNotificationChannel(channelName: String, channelDescription: String) {
        val channel = NotificationChannel(
            NotificationUtil.FOREGROUND_SERVICE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = channelDescription
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun startForegroundService() {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        createNotificationChannel("服务状态", "服务状态")
        val notification: Notification =
            Notification.Builder(context, NotificationUtil.FOREGROUND_SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("服务正在启动")
                .setContentText("尝试以默认账户登录")
                .setContentIntent(pendingIntent)
                .build()
        context.startForeground(NotificationUtil.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    fun updateForegroundServiceNotification(title: String, text: String) {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification: Notification =
            Notification.Builder(context, NotificationUtil.FOREGROUND_SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager.notify(
            NotificationUtil.FOREGROUND_SERVICE_NOTIFICATION_ID,
            notification
        )
    }

    fun cancelForegroundServiceNotification() {
        notificationManager.cancel(NotificationUtil.FOREGROUND_SERVICE_NOTIFICATION_ID)
    }
}