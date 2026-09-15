/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitly.habitly.R
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar

class HabitNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: return Result.failure()
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.failure()

        val settingsDataStore = SettingsDataStore(context)
        val skipCompleted = settingsDataStore.skipCompletedHabitNotifications.first()
        val dao = HabitDatabase.getDatabase(context).habitDao()

        val habit = dao.getHabit(habitId)
        val isInverse = habit?.isInverse ?: inputData.getBoolean(KEY_HABIT_IS_INVERSE, false)
        val target = habit?.getDailyTarget() ?: 1

        var shouldShow = true
        if (skipCompleted) {
            val (startOfDay, endOfDay) = getDayBounds()
            val completionsCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
            shouldShow = shouldShowHabitNotification(skipCompleted, completionsCount, isInverse, target)
        }

        if (shouldShow) {
            createNotificationChannel(context)
            showNotification(context, habitName, habitId, isInverse, dao)
        }

        // Reschedule the notification for the next day
        if (habit != null) {
            HabitNotificationScheduler.scheduleNotification(context, habit)
        }

        return Result.success()
    }

    private suspend fun showNotification(
        context: Context,
        habitName: String,
        habitId: String,
        isInverse: Boolean,
        dao: com.habitly.habitly.data.Database.HabitDao
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = getHabitNotificationTitle(isInverse)
        val contentText = getHabitNotificationContent(habitName, isInverse)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // A default launcher icon
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val (startOfDay, endOfDay) = getDayBounds()
        val currentCount = dao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
        val habit = dao.getHabit(habitId)
        val target = habit?.getDailyTarget() ?: 1

        if (isInverse) {
            if (canOfferUncomplete(isInverse, currentCount, target)) {
                val actionIntent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                    action = "ACTION_UNCOMPLETE_HABIT"
                    putExtra("habitId", habitId)
                }
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    habitId.hashCode() + 1,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, getHabitNotificationActionLabel(isInverse = true), actionPendingIntent)
            }
        } else {
            val actionIntent = android.content.Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_COMPLETE_HABIT"
                putExtra("habitId", habitId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 1,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, getHabitNotificationActionLabel(isInverse = false), actionPendingIntent)
        }

        notificationManager.notify(habitId.hashCode(), builder.build())
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_NAME = "habit_name"
        const val KEY_HABIT_NOTIFICATION_DAYS = "habit_notification_days"
        const val KEY_HABIT_NOTIFICATION_TIME = "habit_notification_time"
        const val KEY_HABIT_IS_INVERSE = "habit_is_inverse"
        private const val CHANNEL_ID = "habit_reminders"
        private const val CHANNEL_NAME = "Habit Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications to remind you about your habits"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(soundUri, null)
                }
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}