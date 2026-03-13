/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.attempt3.MainActivity
import com.example.attempt3.R
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            putExtra("notificationTime", habit.notificationTime)
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
            set(Calendar.MILLISECOND, 0)
        }
        
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Optionally, direct user to settings to grant permission
            // For now, we fall back to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
             // Using setExact instead of setExactAndAllowWhileIdle to preserve battery
             alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
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

    fun scheduleGeneralNotification(time: String, days: Set<String>) {
        if (days.isEmpty()) {
            cancelGeneralNotification()
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", GENERAL_NOTIFICATION_ID)
            putExtra("notificationTime", time)
            putExtra("notificationDays", days.toTypedArray())
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

        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        } else {
            // Using setExact instead of setExactAndAllowWhileIdle to preserve battery
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        }
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
        val pendingResult = goAsync()
        val habitId = intent.getStringExtra("habitId") ?: run {
            pendingResult.finish()
            return
        }
        val notificationTime = intent.getStringExtra("notificationTime")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var shouldShow = true
                val isGeneralNotification = habitId == GENERAL_NOTIFICATION_ID

                if (!isGeneralNotification) {
                    val settingsDataStore = SettingsDataStore(context)
                    val skipCompleted = settingsDataStore.skipCompletedHabitNotifications.first()

                    if (skipCompleted) {
                        val dao = HabitDatabase.getDatabase(context).habitDao()
                        val now = Calendar.getInstance()
                        val startOfDay = (now.clone() as Calendar).apply { 
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0) 
                        }.timeInMillis
                        val endOfDay = (now.clone() as Calendar).apply { 
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999) 
                        }.timeInMillis

                        val completionsCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
                        if (completionsCount > 0) {
                            shouldShow = false
                        }
                    }
                }

                if (shouldShow) {
                    showNotification(context, intent, habitId, isGeneralNotification)
                }

                // Reschedule for the next day/time
                if (notificationTime != null) {
                    if (isGeneralNotification) {
                        val days = intent.getStringArrayExtra("notificationDays")?.toSet()
                        if (days != null) {
                            rescheduleGeneralAlarm(context, notificationTime, days)
                        }
                    } else {
                        val habitName = intent.getStringExtra("habitName")
                        rescheduleHabitAlarm(context, habitId, habitName, notificationTime)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, intent: Intent, habitId: String, isGeneralNotification: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "habit_reminders",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title: String
        val contentText: String

        if (isGeneralNotification) {
            title = "Daily Reminder"
            contentText = "Time to log your habit completions!"
        } else {
            val habitName = intent.getStringExtra("habitName") ?: "your habit"
            title = "Completion Reminder"
            contentText = "Don't forget to complete $habitName today."
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "habit_reminders")
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name) // Replace with your app icon
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }

    private fun rescheduleHabitAlarm(context: Context, habitId: String, habitName: String?, notificationTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val requestCode = habitId.hashCode()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", habitId)
            putExtra("notificationTime", notificationTime)
            putExtra("habitName", habitName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val timeParts = notificationTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Set for the next day
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
             alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
             // Using setExact instead of setExactAndAllowWhileIdle to preserve battery
             alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun rescheduleGeneralAlarm(context: Context, time: String, days: Set<String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", GENERAL_NOTIFICATION_ID)
            putExtra("notificationTime", time)
            putExtra("notificationDays", days.toTypedArray())
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
        
        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
             alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        } else {
             // Using setExact instead of setExactAndAllowWhileIdle to preserve battery
             alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        }
    }
}

fun getNextAlarmTime(hour: Int, minute: Int, days: Set<String>): Long? {
    if (days.isEmpty()) return null

    val dayMap = mapOf(
        "SUN" to Calendar.SUNDAY,
        "MON" to Calendar.MONDAY,
        "TUE" to Calendar.TUESDAY,
        "WED" to Calendar.WEDNESDAY,
        "THU" to Calendar.THURSDAY,
        "FRI" to Calendar.FRIDAY,
        "SAT" to Calendar.SATURDAY
    )
    val enabledDays = days.mapNotNull { dayMap[it] }.toSet()

    val now = Calendar.getInstance()
    
    // Check up to 7 days ahead
    for (i in 0..7) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, i)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (enabledDays.contains(dayOfWeek)) {
            if (calendar.timeInMillis > now.timeInMillis) {
                return calendar.timeInMillis
            }
        }
    }
    
    return null
}