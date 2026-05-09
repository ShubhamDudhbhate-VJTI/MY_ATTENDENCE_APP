package com.example.dbms_shubham_application.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.dbms_shubham_application.MainActivity
import com.example.dbms_shubham_application.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "attendx_urgent_v4"
        const val CHANNEL_NAME = "Urgent Notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"]

        if (title != null || message != null) {
            showNotification(title, message)
            
            // Send local broadcast for in-app popup
            val intent = Intent("com.example.attendx.NOTIFICATION_RECEIVED").apply {
                putExtra("title", title)
                putExtra("message", message)
            }
            sendBroadcast(intent)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts and attendance updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = System.currentTimeMillis().toInt()
        
        // Add Delete Action
        val deleteIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "ACTION_DELETE_NOTIFICATION"
            putExtra("notification_id", notificationId)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            this, notificationId, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentTitle(title ?: "AttendX Notification")
            .setContentText(message ?: "You have a new update")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setPriority(NotificationCompat.PRIORITY_MAX) 
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Delete", deletePendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
}
