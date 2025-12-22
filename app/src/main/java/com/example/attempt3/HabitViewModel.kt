package com.example.attempt3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface HabitsUiState {
    data object Loading : HabitsUiState
    data class Success(val habits: List<HabitWithCompletions>) : HabitsUiState
}

class HabitViewModel(private val habitDao: HabitDao) : ViewModel() {

    private val optimisticCompletionChanges = MutableStateFlow<Map<String, Pair<Boolean, Long>>>(emptyMap())

    val habitsUiState: StateFlow<HabitsUiState> =
        habitDao.getHabitsWithCompletions().combine(optimisticCompletionChanges) { habits, changes ->
            if (changes.isEmpty()) {
                habits
            } else {
                habits.map { habitWithCompletions ->
                    val habitId = habitWithCompletions.habit.id
                    val change = changes[habitId]
                    if (change == null) {
                        habitWithCompletions
                    } else {
                        val (isCompleted, date) = change
                        val now = Calendar.getInstance()
                        val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()

                        val startOfDay = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        val endOfDay = Calendar.getInstance().apply { timeInMillis = date; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis


                        if (isCompleted) {
                            val alreadyCompleted = habitWithCompletions.completions.any { it.date in startOfDay..endOfDay }
                            if (alreadyCompleted) habitWithCompletions else {
                                val newCompletion = Completion(
                                    id = UUID.randomUUID().toString(),
                                    habitId = habitId,
                                    date = date,
                                    timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                                    amountOfCompletions = 1
                                )
                                habitWithCompletions.copy(completions = habitWithCompletions.completions + newCompletion)
                            }
                        } else {
                            habitWithCompletions.copy(completions = habitWithCompletions.completions.filterNot { it.date in startOfDay..endOfDay })
                        }
                    }
                }
            }
        }
            .map { HabitsUiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HabitsUiState.Loading
            )

    val archivedHabitsUiState: StateFlow<HabitsUiState> =
        habitDao.getArchivedHabitsWithCompletions()
            .map { HabitsUiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HabitsUiState.Loading
            )

    fun toggleCompletion(habit: Habit, date: Calendar, isCompleted: Boolean) {
        val habitId = habit.id
        val dateInMillis = date.timeInMillis

        val now = Calendar.getInstance()
        val startOfToday = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val endOfToday = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis

        if (dateInMillis in startOfToday..endOfToday) {
            optimisticCompletionChanges.update { it + (habitId to Pair(!isCompleted, dateInMillis)) }
        }

        viewModelScope.launch {
            val startOfDay = (date.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val endOfDay = (date.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
            val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()

            if (!isCompleted) {
                habitDao.insertCompletion(
                    Completion(
                        id = UUID.randomUUID().toString(),
                        habitId = habitId,
                        date = dateInMillis,
                        timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                        amountOfCompletions = 1
                    )
                )
            } else {
                habitDao.deleteCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)
            }
        }
    }

    fun reorderHabits(habits: List<Habit>) {
        viewModelScope.launch {
            habitDao.updateHabits(habits)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
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
