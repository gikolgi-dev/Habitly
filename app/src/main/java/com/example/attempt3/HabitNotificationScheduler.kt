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

        if (!habit.notificationsEnabled || habit.notificationTime.isNullOrEmpty() || habit.notificationDays.isNullOrEmpty()) {
            // Cancel any existing notification if notifications are disabled, time is not set, or no days are selected
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
            .putString(HabitNotificationWorker.KEY_HABIT_NOTIFICATION_DAYS, habit.notificationDays)
            .putString(HabitNotificationWorker.KEY_HABIT_NOTIFICATION_TIME, habit.notificationTime)
            .build()

        val now = Calendar.getInstance()
        val nextReminderTime = getNextReminderTime(now, hour, minute, habit.notificationDays.split(',').toSet())

        if (nextReminderTime == null) {
            // No upcoming notification day, cancel any existing one.
            cancelNotification(context, habit.id)
            return
        }

        val initialDelay = nextReminderTime.timeInMillis - now.timeInMillis

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

    private fun getNextReminderTime(now: Calendar, hour: Int, minute: Int, notificationDays: Set<String>): Calendar? {
        val dayMapping = mapOf(
            "SUN" to Calendar.SUNDAY,
            "MON" to Calendar.MONDAY,
            "TUE" to Calendar.TUESDAY,
            "WED" to Calendar.WEDNESDAY,
            "THU" to Calendar.THURSDAY,
            "FRI" to Calendar.FRIDAY,
            "SAT" to Calendar.SATURDAY
        )

        val selectedDaysOfWeek = notificationDays.mapNotNull { dayMapping[it] }.toSortedSet()
        if (selectedDaysOfWeek.isEmpty()) return null

        val reminderTimeToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        if (selectedDaysOfWeek.contains(currentDayOfWeek) && reminderTimeToday.after(now)) {
            return reminderTimeToday
        }

        // Find the next selected day in the week
        for (day in selectedDaysOfWeek) {
            if (day > currentDayOfWeek) {
                return (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, day - currentDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
            }
        }

        // If no day in the current week is found, take the first day of the next week
        val firstDayOfWeek = selectedDaysOfWeek.first()
        return (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 7 - currentDayOfWeek + firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
    }
}