package com.example.attempt3

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object HabitNotificationScheduler {

    fun scheduleNotification(context: Context, habit: Habit) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "habit_notification_${habit.id}"

        if (!habit.notificationsEnabled || habit.notificationTime.isNullOrEmpty()) {
            // Cancel any existing notification if notifications are disabled or time is not set
            cancelNotification(context, habit.id)
            return
        }

        val timeParts = habit.notificationTime.split(":")
        if (timeParts.size != 2) {
            // Invalid time format, cancel notification
            cancelNotification(context, habit.id)
            return
        }

        val hour = timeParts[0].toIntOrNull()
        val minute = timeParts[1].toIntOrNull()

        if (hour == null || minute == null) {
            // Invalid time format, cancel notification
            cancelNotification(context, habit.id)
            return
        }

        val data = Data.Builder()
            .putString(HabitNotificationWorker.KEY_HABIT_ID, habit.id)
            .putString(HabitNotificationWorker.KEY_HABIT_NAME, habit.name)
            .build()

        val now = Calendar.getInstance()
        val reminderTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (reminderTime.before(now)) {
            reminderTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = reminderTime.timeInMillis - now.timeInMillis

        val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelNotification(context: Context, habitId: String) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "habit_notification_${habitId}"
        workManager.cancelUniqueWork(workTag)
    }
}