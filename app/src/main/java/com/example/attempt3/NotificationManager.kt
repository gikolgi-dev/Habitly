package com.example.attempt3

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

const val GENERAL_NOTIFICATION_ID = "general_notification"
const val GENERAL_NOTIFICATION_REQUEST_CODE = 1001

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(habit: Habit) {
        if (!habit.notificationsEnabled || habit.notificationTime == null) {
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", habit.id)
            putExtra("habitName", habit.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeParts = habit.notificationTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelNotification(habit: Habit) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleGeneralNotification(time: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", GENERAL_NOTIFICATION_ID)
            putExtra("habitName", "Check your habits!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelGeneralNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habitId") ?: return
        val habitName = intent.getStringExtra("habitName") ?: "your habit"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "habit_reminders",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "habit_reminders")
            .setContentTitle("Habit Reminder")
            .setContentText("Don't forget to complete your habit: $habitName")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }
}
