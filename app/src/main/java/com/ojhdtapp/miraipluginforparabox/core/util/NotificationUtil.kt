package com.ojhdtapp.miraipluginforparabox.core.util

import android.Manifest
import android.util.Log
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.ojhdtapp.miraipluginforparabox.MainActivity
import com.ojhdtapp.miraipluginforparabox.R
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtil.SERVICE_STATE_CHANNEL_ID
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtil.FOREGROUND_SERVICE_NOTIFICATION_ID
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtil.NORMAL_NOTIFICATION_ID

object NotificationUtil {
    const val SERVICE_STATE_CHANNEL_ID = "service_state"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
    const val NORMAL_NOTIFICATION_ID = 2
}

class NotificationUtilForService(val service: Service) {
    private val notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createNotificationChannel(channelName: String, channelDescription: String) {
        val channel = NotificationChannel(
            SERVICE_STATE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = channelDescription
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun startForegroundService() {
        Log.d("parabox", "startForegroundService")
        val pendingIntent: PendingIntent = Intent(service, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                service, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        createNotificationChannel("服务状态", "服务状态")
        val notification: Notification =
            Notification.Builder(service, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_outline_extension_24)
                .setContentTitle(service.getString(R.string.mirai_running))
                .setContentIntent(pendingIntent)
                .build()
        service.startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    fun updateForegroundServiceNotification(title: String) {
        val pendingIntent: PendingIntent = Intent(service, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                service, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification: Notification =
            Notification.Builder(service, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_outline_extension_24)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager.notify(
            FOREGROUND_SERVICE_NOTIFICATION_ID,
            notification
        )
    }

    fun stopForegroundService() {
        notificationManager.cancel(FOREGROUND_SERVICE_NOTIFICATION_ID)
        service.stopForeground(true)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}

class NotificationUtilForActivity(val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createNotificationChannel(channelName: String, channelDescription: String) {
        val channel = NotificationChannel(
            SERVICE_STATE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = channelDescription
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(title: String, text: String) {
        createNotificationChannel("服务状态", "服务状态")
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification: Notification =
            Notification.Builder(context, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_outline_extension_24)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).run {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(NORMAL_NOTIFICATION_ID, notification)
        }
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}