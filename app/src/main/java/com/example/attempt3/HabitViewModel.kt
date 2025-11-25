package com.example.attempt3

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HabitsUiState {
    data object Loading : HabitsUiState
    data class Success(val habits: List<HabitWithCompletions>) : HabitsUiState
}

class HabitViewModel(private val habitDao: HabitDao) : ViewModel() {

    val habitsUiState: StateFlow<HabitsUiState> =
        habitDao.getHabitsWithCompletions()
            .map {
                //delay(700)
                HabitsUiState.Success(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HabitsUiState.Loading
            )

    val archivedHabitsUiState: StateFlow<HabitsUiState> =
        habitDao.getArchivedHabitsWithCompletions()
            .map {
                //delay(700)
                HabitsUiState.Success(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HabitsUiState.Loading
            )
    fun reorderHabits(habits: List<Habit>) {
        viewModelScope.launch {
            habitDao.updateHabits(habits)
        }
    }

    fun showReminderNotification(context: Context, habitName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "habit_reminders")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Completion Reminder")
            .setContentText("Don't forget to complete $habitName today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }
}

class HabitViewModelFactory(private val habitDao: HabitDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(habitDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
