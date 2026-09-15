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
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.data.Database.getEffectiveStartDateMillis
import com.habitly.habitly.data.Database.normalizeToStartOfDay
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
import com.habitly.habitly.data.welcome.WelcomeCardEngine
import com.habitly.habitly.data.welcome.WelcomeCardContext

const val GENERAL_NOTIFICATION_ID = "general_notification"
const val GENERAL_NOTIFICATION_REQUEST_CODE = 1001
const val WELCOME_CARD_NOTIFICATION_ID = "welcome_card_notification"
const val WELCOME_CARD_NOTIFICATION_REQUEST_CODE = 1002

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(habit: Habit, exactMode: Boolean? = null) {
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
            putExtra("isInverse", habit.isInverse)
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
        val timeParts = habit.notificationTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            cancelNotification(habit)
            return
        }

        val timeInMillis = getNextAlarmTime(hour, minute, days)

        if (timeInMillis != null) {
            if (exactMode != null) {
                scheduleAlarm(alarmManager, timeInMillis, pendingIntent, exactMode)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val isExact = SettingsDataStore(context).exactAlarms.first()
                    scheduleAlarm(alarmManager, timeInMillis, pendingIntent, isExact)
                }
            }
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

    fun scheduleGeneralNotification(time: String, days: Set<String>, exactMode: Boolean? = null) {
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

        val timeParts = time.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            cancelGeneralNotification()
            return
        }

        // Find the next occurrence matching the selected days of the week
        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        if (exactMode != null) {
            scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, exactMode)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val isExact = SettingsDataStore(context).exactAlarms.first()
                scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, isExact)
            }
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

    fun scheduleWelcomeCardNotification(time: String, days: Set<String>, exactMode: Boolean? = null) {
        if (days.isEmpty()) {
            cancelWelcomeCardNotification()
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", WELCOME_CARD_NOTIFICATION_ID)
            putExtra("notificationTime", time)
            putExtra("notificationDays", days.toTypedArray())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WELCOME_CARD_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeParts = time.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            cancelWelcomeCardNotification()
            return
        }

        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        if (exactMode != null) {
            scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, exactMode)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val isExact = SettingsDataStore(context).exactAlarms.first()
                scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, isExact)
            }
        }
    }

    fun cancelWelcomeCardNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WELCOME_CARD_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun rescheduleAll() {
        val settingsDataStore = SettingsDataStore(context)
        val exactMode = settingsDataStore.exactAlarms.first()

        val dao = HabitDatabase.getDatabase(context).habitDao()
        val habits = dao.getAllHabitsSnapshot()
        for (habit in habits) {
            scheduleNotification(habit, exactMode)
        }

        val globalEnabled = settingsDataStore.globalNotificationsEnabled.first()
        if (globalEnabled) {
            val globalTime = settingsDataStore.globalNotificationTime.first()
            val globalDays = settingsDataStore.globalNotificationDays.first()
            scheduleGeneralNotification(globalTime, globalDays, exactMode)
        } else {
            cancelGeneralNotification()
        }

        val welcomeCardEnabled = settingsDataStore.welcomeCardNotificationEnabled.first()
        if (welcomeCardEnabled) {
            val wcTime = settingsDataStore.welcomeCardNotificationTime.first()
            val wcDays = settingsDataStore.welcomeCardNotificationDays.first()
            scheduleWelcomeCardNotification(wcTime, wcDays, exactMode)
        } else {
            cancelWelcomeCardNotification()
        }
    }

    suspend fun cancelAllNotifications() {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val habits = dao.getAllHabitsSnapshot()
        for (habit in habits) {
            cancelNotification(habit)
        }
        cancelGeneralNotification()
        cancelWelcomeCardNotification()
    }
}

/**
 * Helper to centralize exact alarm scheduling logic, supporting both high-accuracy
 * (allow while idle) mode and battery-saving mode, falling back to inexact alarms
 * if exact alarms are not permitted.
 */
internal fun scheduleAlarm(
    alarmManager: AlarmManager,
    timeInMillis: Long,
    pendingIntent: PendingIntent,
    exactMode: Boolean = false
) {
    if (exactMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback if exact alarm permission is not granted on Android 12+
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // High accuracy mode: wake up even during Doze mode at the exact time
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } catch (e2: SecurityException) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    } else {
        // Battery-saving (default) mode:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback to inexact alarm. Note: Inexact alarms might be delayed by the OS to batch wakeups.
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            try {
                // Using setExact instead of setExactAndAllowWhileIdle to preserve battery.
                // This means it might not fire immediately during Doze mode, but will fire shortly after.
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        }
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
                    val exactMode = settingsDataStore.exactAlarms.first()

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)

                    val isInverse = intent.getBooleanExtra("isInverse", false)

                    // Create pending intent for snoozed notification
                    val rescheduleIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "ACTION_SHOW_SNOOZED_NOTIFICATION"
                        putExtra("habitId", habitId)
                        putExtra("habitName", intent.getStringExtra("habitName"))
                        putExtra("targetDate", targetDate)
                        putExtra("isInverse", isInverse)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        habitId.hashCode() + 3, // Unique request code
                        rescheduleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val snoozeTime = Calendar.getInstance().timeInMillis + (snoozeDurationMinutes * 60 * 1000L)
                    scheduleAlarm(alarmManager, snoozeTime, pendingIntent, exactMode)

                    // Format the snooze time based on settings
                    val sdf = if (is24Hour) {
                        SimpleDateFormat("HH:mm", Locale.getDefault())
                    } else {
                        SimpleDateFormat("h:mm a", Locale.getDefault())
                    }
                    val formattedTime = sdf.format(Date(snoozeTime))

                    // Update the SAME notification to show "Snoozed until xx:xx"
                    val isGeneralNotification = habitId == GENERAL_NOTIFICATION_ID
                    val isQuit = isInverse || (HabitDatabase.getDatabase(context).habitDao()
                        .getHabit(habitId)?.isInverse == true)
                    val title = if (isGeneralNotification) "Daily Reminder" else getHabitNotificationTitle(isQuit)
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

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                    val habit = dao.getHabit(habitId)
                    if (habit?.isInverse == true) {
                        // Quit habits are not completed via this action
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(habitId.hashCode())
                        return@launch
                    }

                    val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)
                    val dateCal = Calendar.getInstance().apply { timeInMillis = targetDate }
                    val timezoneOffsetInMinutes =
                        TimeUnit.MILLISECONDS.toMinutes(dateCal.timeZone.rawOffset.toLong()).toInt()

                    val (startOfDay, endOfDay) = getDayBounds(dateCal)
                    if (habit != null) {
                        val habitStart = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
                        if (startOfDay < habitStart) {
                            dao.updateHabit(habit.copy(startDate = startOfDay.toString()))
                        }
                    }
                    val target = habit?.getDailyTarget() ?: 1
                    val currentCompletions = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
                    val newCompletions = (currentCompletions + 1).coerceAtMost(target)

                    dao.setCompletionsForHabitOnDay(
                        habitId,
                        startOfDay,
                        endOfDay,
                        com.habitly.habitly.data.Database.Completion(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = targetDate,
                            timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                            amountOfCompletions = newCompletions
                        )
                    )

                    // Dismiss the notification now that the habit is marked complete
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(habitId.hashCode())
                } finally {
                    // Always finish pendingResult to signal the OS we're done
                    pendingResult.finish()
                }
            }
            return
        }

        // --- Handle direct action from the Notification: "Uncomplete Habit" ---
        if (intent.action == "ACTION_UNCOMPLETE_HABIT") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = HabitDatabase.getDatabase(context).habitDao()
                    val habit = dao.getHabit(habitId)

                    val targetDate = intent.getLongExtra("targetDate", Calendar.getInstance().timeInMillis)
                    val dateCal = Calendar.getInstance().apply { timeInMillis = targetDate }
                    val timezoneOffsetInMinutes =
                        TimeUnit.MILLISECONDS.toMinutes(dateCal.timeZone.rawOffset.toLong()).toInt()

                    val (startOfDay, endOfDay) = getDayBounds(dateCal)
                    if (habit != null) {
                        val habitStart = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
                        if (startOfDay < habitStart) {
                            dao.updateHabit(habit.copy(startDate = startOfDay.toString()))
                        }
                    }
                    val target = habit?.getDailyTarget() ?: 1
                    val currentSlips = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
                    val newSlips = (currentSlips + 1).coerceAtMost(target)

                    dao.setCompletionsForHabitOnDay(
                        habitId,
                        startOfDay,
                        endOfDay,
                        com.habitly.habitly.data.Database.Completion(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = targetDate,
                            timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                            amountOfCompletions = newSlips
                        )
                    )

                    // Dismiss the notification now that the habit is marked uncompleted
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(habitId.hashCode())
                } finally {
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
                val isWelcomeCardNotification = habitId == WELCOME_CARD_NOTIFICATION_ID
                val settingsDataStore = SettingsDataStore(context)
                val snoozeEnabled = settingsDataStore.snoozeEnabled.first()
                val exactMode = settingsDataStore.exactAlarms.first()
                if (!isGeneralNotification && !isWelcomeCardNotification) {
                    // Check if the user has opted to skip notifications for habits already completed today
                    val skipCompleted = settingsDataStore.skipCompletedHabitNotifications.first()

                    if (skipCompleted) {
                        val dao = HabitDatabase.getDatabase(context).habitDao()

                        // Define bounds for "today" to check for existing completions
                        val (startOfDay, endOfDay) = getDayBounds()

                        val completionsCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
                        val habit = dao.getHabit(habitId)
                        val isInverse = habit?.isInverse ?: intent.getBooleanExtra("isInverse", false)
                        val target = habit?.getDailyTarget() ?: 1
                        shouldShow = shouldShowHabitNotification(skipCompleted, completionsCount, isInverse, target)
                    }
                }

                if (shouldShow) {
                    showNotification(
                        context,
                        intent,
                        habitId,
                        isGeneralNotification,
                        isWelcomeCardNotification,
                        snoozeEnabled,
                        targetDate
                    )
                }

                // Since AlarmManager only schedules the alarm once, we must re-schedule
                // the next occurrence manually. We don't reschedule if this is just a snoozed trigger.
                if (!isSnoozedTrigger && notificationTime != null) {
                    val days = intent.getStringArrayExtra("notificationDays")?.toSet()
                    if (isWelcomeCardNotification) {
                        if (days != null) {
                            rescheduleWelcomeCardAlarm(context, notificationTime, days, exactMode)
                        }
                    } else if (isGeneralNotification) {
                        if (days != null) {
                            rescheduleGeneralAlarm(context, notificationTime, days, exactMode)
                        }
                    } else {
                        val habitName = intent.getStringExtra("habitName")
                        val isInverse = intent.getBooleanExtra("isInverse", false)
                        rescheduleHabitAlarm(context, habitId, habitName, notificationTime, days, exactMode, isInverse)
                    }
                }
            } finally {
                // Ensure receiver lifecycle finishes appropriately
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(
        context: Context,
        intent: Intent,
        habitId: String,
        isGeneralNotification: Boolean,
        isWelcomeCardNotification: Boolean,
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

        val dao = HabitDatabase.getDatabase(context).habitDao()
        val habit = if (!isGeneralNotification && !isWelcomeCardNotification) dao.getHabit(habitId) else null
        val isInverse = habit?.isInverse ?: intent.getBooleanExtra("isInverse", false)

        if (isWelcomeCardNotification) {
            val habits = dao.getAllHabitsWithCompletionsSnapshot()
            val settingsDataStore = SettingsDataStore(context)
            val firstDayOfWeek = settingsDataStore.firstDayOfWeekCalendar.first()
            val savedDate = settingsDataStore.welcomeCardDate.first().ifEmpty { null }
            val savedCat = settingsDataStore.welcomeCardCategoryId.first().ifEmpty { null }
            val savedHabit = settingsDataStore.welcomeCardHabitId.first().ifEmpty { null }
            val savedTemplate = settingsDataStore.welcomeCardTemplateIndex.first()
            val ignoreWelcomeEngine = settingsDataStore.ignoreWelcomeEngine.first()

            val welcomeContext = WelcomeCardContext(
                habits = habits,
                todayMillis = System.currentTimeMillis(),
                firstDayOfWeek = firstDayOfWeek
            )
            val resolved = WelcomeCardEngine.resolveMessage(
                context = welcomeContext,
                savedDateKey = savedDate,
                savedCategoryId = savedCat,
                savedHabitId = savedHabit,
                savedTemplateIndex = savedTemplate,
                ignoreWelcomeEngine = ignoreWelcomeEngine
            )
            if (resolved.isNewSelection) {
                settingsDataStore.saveWelcomeCardState(
                    dateKey = resolved.dateKey,
                    categoryId = resolved.categoryId,
                    habitId = resolved.habitId,
                    templateIndex = resolved.templateIndex
                )
            }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            title = when (hour) {
                in 1..5 -> "It's a beautiful night!"
                in 6..14 -> "Good morning"
                in 15..19 -> "Good afternoon"
                else -> "Good evening"
            }
            contentText = resolved.text
        } else if (isGeneralNotification) {
            title = "Daily Reminder"
            contentText = "Time to log your habit completions!"
        } else {
            val habitName = habit?.name ?: intent.getStringExtra("habitName") ?: "your habit"
            title = getHabitNotificationTitle(isInverse)
            contentText = getHabitNotificationContent(habitName, isInverse)
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

        // Add the inline action for single habits ("Uncomplete" for quit habits, "Complete" for build habits)
        if (!isGeneralNotification && !isWelcomeCardNotification) {
            val (startOfDay, endOfDay) = getDayBounds(Calendar.getInstance().apply { timeInMillis = targetDate })
            val currentCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
            val target = habit?.getDailyTarget() ?: 1

            if (isInverse) {
                if (canOfferUncomplete(isInverse, currentCount, target)) {
                    val actionIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "ACTION_UNCOMPLETE_HABIT"
                        putExtra("habitId", habitId)
                        putExtra("targetDate", targetDate)
                    }
                    val actionPendingIntent = PendingIntent.getBroadcast(
                        context,
                        habitId.hashCode() + 1,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(
                        0,
                        getHabitNotificationActionLabel(isInverse = true),
                        actionPendingIntent
                    )
                }
            } else {
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
                    getHabitNotificationActionLabel(isInverse = false),
                    actionPendingIntent
                )
            }
        }

        // Add the inline "Snooze" action
        if (snoozeEnabled) {
            val habitName = habit?.name ?: intent.getStringExtra("habitName")
            val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_SNOOZE_NOTIFICATION"
                putExtra("habitId", habitId)
                putExtra("habitName", habitName)
                putExtra("targetDate", targetDate)
                putExtra("isInverse", isInverse)
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

    private fun rescheduleHabitAlarm(
        context: Context,
        habitId: String,
        habitName: String?,
        notificationTime: String,
        days: Set<String>?,
        exactMode: Boolean,
        isInverse: Boolean = false
    ) {
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
            putExtra("isInverse", isInverse)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeParts = notificationTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return
        }

        val timeInMillis = getNextAlarmTime(hour, minute, days)

        if (timeInMillis != null) {
            scheduleAlarm(alarmManager, timeInMillis, pendingIntent, exactMode)
        }
    }

    private fun rescheduleGeneralAlarm(context: Context, time: String, days: Set<String>, exactMode: Boolean) {
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
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return
        }

        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, exactMode)
    }

    private fun rescheduleWelcomeCardAlarm(context: Context, time: String, days: Set<String>, exactMode: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habitId", WELCOME_CARD_NOTIFICATION_ID)
            putExtra("notificationTime", time)
            putExtra("notificationDays", days.toTypedArray())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WELCOME_CARD_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeParts = time.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return
        }

        val nextAlarmTime = getNextAlarmTime(hour, minute, days) ?: return

        scheduleAlarm(alarmManager, nextAlarmTime, pendingIntent, exactMode)
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

/**
 * Determines whether a habit notification should be shown based on user settings
 * and whether the habit has already been completed today.
 */
fun shouldShowHabitNotification(
    skipCompleted: Boolean,
    completionsCount: Int,
    isInverse: Boolean = false,
    target: Int = 1
): Boolean {
    if (!skipCompleted) return true
    return completionsCount < target
}

/**
 * Returns the notification title for a habit based on whether it is a quit (inverse) habit.
 */
fun getHabitNotificationTitle(isInverse: Boolean): String =
    if (isInverse) "Quit Reminder" else "Completion Reminder"

/**
 * Returns the notification text for a habit based on whether it is a quit (inverse) habit.
 */
fun getHabitNotificationContent(habitName: String, isInverse: Boolean): String =
    if (isInverse) "Did you succeed with $habitName today?" else "Don't forget to complete $habitName today."

/**
 * Returns the action label for a habit notification ("Uncomplete" for quit habits, "Complete" for build habits).
 */
fun getHabitNotificationActionLabel(isInverse: Boolean): String =
    if (isInverse) "Uncomplete" else "Complete"

/**
 * Checks if the "Uncomplete" action should be offered for a quit habit.
 */
fun canOfferUncomplete(isInverse: Boolean, currentSlips: Int, target: Int): Boolean =
    isInverse && currentSlips < target

/**
 * Returns the timestamp bounds (startOfDay, endOfDay) in milliseconds for the given calendar day.
 */
fun getDayBounds(calendar: Calendar = Calendar.getInstance()): Pair<Long, Long> {
    val startOfDay = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfDay = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    return Pair(startOfDay, endOfDay)
}
