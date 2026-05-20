/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.habitly.habitly.MainActivity
import com.habitly.habitly.R
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

const val GENERAL_NOTIFICATION_ID = "general_notification"
const val GENERAL_NOTIFICATION_REQUEST_CODE = 1001

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(habit: Habit) {
        if (habit.archived || !habit.notificationsEnabled || habit.notificationTime == null) {
            cancelNotification(habit)
            return
        }

        val days = habit.notificationDays?.split(",")?.filter { it.isNotBlank() }?.toSet()
        if (days.isNullOrEmpty()) {
            // If no days are selected, ensure the notification is cancelled
            cancelNotification(habit)
            return
        }

        // Prepare the intent that will trigger our NotificationReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", habit.id)
            putExtra("habitName", habit.name)
            putExtra("notificationTime", habit.notificationTime)
            putExtra("notificationDays", days.toTypedArray())
        }

        // FLAG_UPDATE_CURRENT: Updates the existing PendingIntent with the latest Intent extras.
        // FLAG_IMMUTABLE: Required for apps targeting Android 12+ (API 31+) for security.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse "HH:mm" time format safely
        val (hour, minute) = habit.notificationTime.split(":").map { it.toInt() }

        val timeInMillis = getNextAlarmTime(hour, minute, days)

        if (timeInMillis != null) {
            scheduleAlarm(alarmManager, timeInMillis, pendingIntent)
        } else {
            cancelNotification(habit)
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
        // Canceling the pending intent removes the scheduled alarm from AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleGeneralNotification(time: String, days: Set<String>) {
        // If no days are selected, ensure the notification is cancelled to prevent ghost alarms
        if (days.isEmpty()) {
            cancelGeneralNotification()
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", GENERAL_NOTIFICATION_ID)
            putExtra("notificationTime", time)
            putExtra("notificationDays", days.toTypedArray()) // Intent extras don't support Sets natively
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = time.split(":").map { it.toInt() }

        // Find the next occurrence matching the selected days of the week
        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent)
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

    suspend fun rescheduleAll() {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val habits = dao.getAllHabitsSnapshot()
        for (habit in habits) {
            scheduleNotification(habit)
        }

        val settingsDataStore = SettingsDataStore(context)
        val globalEnabled = settingsDataStore.globalNotificationsEnabled.first()
        if (globalEnabled) {
            val globalTime = settingsDataStore.globalNotificationTime.first()
            val globalDays = settingsDataStore.globalNotificationDays.first()
            scheduleGeneralNotification(globalTime, globalDays)
        } else {
            cancelGeneralNotification()
        }
    }

    suspend fun cancelAllNotifications() {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val habits = dao.getAllHabitsSnapshot()
        for (habit in habits) {
            cancelNotification(habit)
        }
        cancelGeneralNotification()
    }
}

/**
 * Helper to centralize exact alarm scheduling logic, falling back to inexact
 * alarms if exact alarms are not permitted.
 */
internal fun scheduleAlarm(alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // Fallback to inexact alarm. Note: Inexact alarms might be delayed by the OS to batch wakeups.
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    } else {
        // Using setExact instead of setExactAndAllowWhileIdle to preserve battery.
        // This means it might not fire immediately during Doze mode, but will fire shortly after.
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // goAsync allows doing async work (like DB queries) inside the BroadcastReceiver
        // without keeping the main thread blocked and risking an ANR
        val pendingResult = goAsync()

        val habitId = intent.getStringExtra("habitId") ?: run {
            // Terminate early if habitId is missing, preventing crashes downstream
            pendingResult.finish()
            return
        }

        // --- Handle direct action from the Notification: "Snooze" ---
        if (intent.action == "ACTION_SNOOZE_NOTIFICATION") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsDataStore = SettingsDataStore(context)
                    val snoozeDurationMinutes = settingsDataStore.snoozeDurationMinutes.first()
                    val is24Hour = settingsDataStore.is24Hour.first()

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)

                    // Create pending intent for snoozed notification
                    val rescheduleIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "ACTION_SHOW_SNOOZED_NOTIFICATION"
                        putExtra("habitId", habitId)
                        putExtra("habitName", intent.getStringExtra("habitName"))
                        putExtra("targetDate", targetDate)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        habitId.hashCode() + 3, // Unique request code
                        rescheduleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val snoozeTime = Calendar.getInstance().timeInMillis + (snoozeDurationMinutes * 60 * 1000L)
                    scheduleAlarm(alarmManager, snoozeTime, pendingIntent)

                    // Format the snooze time based on settings
                    val sdf = if (is24Hour) {
                        SimpleDateFormat("HH:mm", Locale.getDefault())
                    } else {
                        SimpleDateFormat("h:mm a", Locale.getDefault())
                    }
                    val formattedTime = sdf.format(Date(snoozeTime))

                    // Update the SAME notification to show "Snoozed until xx:xx"
                    val isGeneralNotification = habitId == GENERAL_NOTIFICATION_ID
                    val title = if (isGeneralNotification) "Daily Reminder" else "Completion Reminder"
                    val contentText = "Snoozed until $formattedTime"

                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val activityPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        activityIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val snoozedNotification = NotificationCompat.Builder(context, "habit_reminders")
                        .setContentTitle(title)
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentIntent(activityPendingIntent)
                        .setAutoCancel(true)
                        .build()

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(habitId.hashCode(), snoozedNotification)

                    // Wait 3 seconds and then dismiss the notification
                    delay(2000L)
                    notificationManager.cancel(habitId.hashCode())
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // --- Handle direct action from the Notification: "Complete Habit" ---
        if (intent.action == "ACTION_COMPLETE_HABIT") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = HabitDatabase.getDatabase(context).habitDao()

                    val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)
                    val dateCal = Calendar.getInstance().apply { timeInMillis = targetDate }
                    val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(dateCal.timeZone.rawOffset.toLong()).toInt()

                    // Insert completion directly into the database
                    dao.insertCompletion(
                        com.habitly.habitly.data.Database.Completion(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = targetDate,
                            timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                            amountOfCompletions = 1
                        )
                    )

                    // Dismiss the notification now that the habit is marked complete
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(habitId.hashCode())
                } finally {
                    // Always finish pendingResult to signal the OS we're done
                    pendingResult.finish()
                }
            }
            return
        }

        val isSnoozedTrigger = intent.action == "ACTION_SHOW_SNOOZED_NOTIFICATION"
        val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)

        // --- Handle showing the notification & scheduling the next one ---
        val notificationTime = intent.getStringExtra("notificationTime")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var shouldShow = true
                val isGeneralNotification = habitId == GENERAL_NOTIFICATION_ID
                val settingsDataStore = SettingsDataStore(context)
                val snoozeEnabled = settingsDataStore.snoozeEnabled.first()

                if (!isGeneralNotification) {
                    // Check if the user has opted to skip notifications for habits already completed today
                    val skipCompleted = settingsDataStore.skipCompletedHabitNotifications.first()

                    if (skipCompleted) {
                        val dao = HabitDatabase.getDatabase(context).habitDao()

                        // Define bounds for "today" to check for existing completions
                        val startOfDay = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val endOfDay = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val completionsCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
                        if (completionsCount > 0) {
                            shouldShow = false // Habit was already completed, skip this notification
                        }
                    }
                }

                if (shouldShow) {
                    showNotification(context, intent, habitId, isGeneralNotification, snoozeEnabled, targetDate)
                }

                // Since AlarmManager only schedules the alarm once, we must re-schedule
                // the next occurrence manually. We don't reschedule if this is just a snoozed trigger.
                if (!isSnoozedTrigger && notificationTime != null) {
                    val days = intent.getStringArrayExtra("notificationDays")?.toSet()
                    if (isGeneralNotification) {
                        if (days != null) {
                            rescheduleGeneralAlarm(context, notificationTime, days)
                        }
                    } else {
                        val habitName = intent.getStringExtra("habitName")
                        rescheduleHabitAlarm(context, habitId, habitName, notificationTime, days)
                    }
                }
            } finally {
                // Ensure receiver lifecycle finishes appropriately
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        intent: Intent,
        habitId: String,
        isGeneralNotification: Boolean,
        snoozeEnabled: Boolean,
        targetDate: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel (required for API 26+)
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

        // Tapping the notification opens the main activity
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "habit_reminders")
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name) // Icon displayed in the status bar
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true) // Dismiss the notification automatically when tapped

        // Add the inline "Complete" action for single habits
        if (!isGeneralNotification) {
            val actionIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_COMPLETE_HABIT"
                putExtra("habitId", habitId)
                putExtra("targetDate", targetDate)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 1, // Offset the hash code to avoid intent collisions
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0, // 0 means no icon is specified (action icons are largely ignored on modern Android anyway)
                "Complete",
                actionPendingIntent
            )
        }

        // Add the inline "Snooze" action
        if (snoozeEnabled) {
            val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_SNOOZE_NOTIFICATION"
                putExtra("habitId", habitId)
                putExtra("habitName", intent.getStringExtra("habitName"))
                putExtra("targetDate", targetDate)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 2, // Unique request code
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                "Snooze",
                snoozePendingIntent
            )
        }

        // Use habitId.hashCode() to ensure each habit gets a unique notification,
        // or general notifications are stacked under a single ID
        notificationManager.notify(habitId.hashCode(), builder.build())
    }

    private fun rescheduleHabitAlarm(context: Context, habitId: String, habitName: String?, notificationTime: String, days: Set<String>?) {
        if (days.isNullOrEmpty()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = habitId.hashCode()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", habitId)
            putExtra("notificationTime", notificationTime)
            putExtra("habitName", habitName)
            putExtra("notificationDays", days.toTypedArray())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = notificationTime.split(":").map { it.toInt() }

        val timeInMillis = getNextAlarmTime(hour, minute, days)

        if (timeInMillis != null) {
            scheduleAlarm(alarmManager, timeInMillis, pendingIntent)
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

        val (hour, minute) = time.split(":").map { it.toInt() }

        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent)
    }
}

/**
 * Calculates the next occurrence timestamp matching a specific set of days.
 */
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

    // Check up to 7 days ahead to find the next active day for the alarm
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
            // Must be strictly in the future
            if (calendar.timeInMillis > now.timeInMillis) {
                return calendar.timeInMillis
            }
        }
    }

    return null
}
