package com.example.dbms_shubham_application.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (action == "ACTION_DELETE_NOTIFICATION") {
            Log.d("NotificationReceiver", "Deleting notification: $notificationId")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationId != -1) {
                notificationManager.cancel(notificationId)
            }
            // Add any additional logic here, like calling an API to delete from backend
        }
    }
}
