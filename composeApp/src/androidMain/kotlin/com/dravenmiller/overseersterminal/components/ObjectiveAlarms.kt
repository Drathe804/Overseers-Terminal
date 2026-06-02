package com.dravenmiller.overseersterminal.components

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dravenmiller.overseersterminal.MainActivity

actual fun scheduleObjectiveNotification(id: Int, title: String, message: String, delayMs: Long) {
    val context = MainActivity.appContext
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "pipboy_objectives"

    val triggerTime = System.currentTimeMillis() + delayMs

    // 1. CREATE THE CHANNEL (Required for both notifications)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Wasteland Directives", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    // 2. CREATE THE CLICKABLE LAUNCH INTENT
    val mainIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val mainPendingIntent = PendingIntent.getActivity(
        context, id, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3. POST THE LIVE COUNTDOWN NOTIFICATION IMMEDIATELY
    val activeNotification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(">>> COUNTDOWN INITIATED <<<")
        .setContentText(message) // Shows what objective is waiting
        .setPriority(NotificationCompat.PRIORITY_LOW) // Keeps it quiet so it doesn't beep when it starts
        .setOngoing(true) // Prevents you from swiping it away accidentally!
        .setUsesChronometer(true) // Turns on the math engine
        .setChronometerCountDown(true) // Forces it to count backwards
        .setWhen(triggerTime) // Tells it what time to hit 00:00
        .setContentIntent(mainPendingIntent)
        .build()

    notificationManager.notify(id, activeNotification)

    // 4. SET THE OS ALARM TO OVERWRITE IT WHEN FINISHED
    val intent = Intent(context, ObjectiveAlarmReceiver::class.java).apply {
        putExtra("NOTIF_ID", id)
        putExtra("NOTIF_TITLE", title)
        putExtra("NOTIF_MESSAGE", message)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}


class ObjectiveAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("NOTIF_ID", 0)
        val title = intent.getStringExtra("NOTIF_TITLE") ?: "Pip-Boy Alert"
        val message = intent.getStringExtra("NOTIF_MESSAGE") ?: "Objective Updated"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pipboy_objectives"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wasteland Directives",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // --- THE FIX: Creates the key to launch your app! ---
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            // THE FIX: Automatically grabs your Pip-Boy app icon!
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // THE FIX: Makes the notification clickable!
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
