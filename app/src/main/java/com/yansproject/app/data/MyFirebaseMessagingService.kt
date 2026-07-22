package com.yansproject.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yansproject.app.MainActivity
import com.yansproject.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "From: ${remoteMessage.from}")

        val category = remoteMessage.data["category"] ?: "Sistem"
        val targetTab = remoteMessage.data["target_tab"] ?: remoteMessage.data["targetTab"]

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM_SERVICE", "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Notification YANSPROJECT.ID", it.body ?: "", category, targetTab)
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM_SERVICE", "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Notification YANSPROJECT.ID"
            val body = remoteMessage.data["body"] ?: ""
            sendNotification(title, body, category, targetTab)
        }
    }

    private fun sendNotification(title: String, messageBody: String, category: String = "Sistem", targetTab: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (targetTab != null) {
                putExtra("TARGET_TAB", targetTab)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Save to in-app notification center
        com.yansproject.app.ui.AppSettings.addNotification(this, title, messageBody, category, targetTab)

        val channelId = "yans_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "YANSPROJECT.ID Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
