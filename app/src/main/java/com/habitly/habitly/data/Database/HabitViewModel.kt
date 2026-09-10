/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.Database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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

    private val optimisticCompletionChanges = MutableStateFlow<Map<String, List<Completion>>>(emptyMap())

    val habitsUiState: StateFlow<HabitsUiState> =
        habitDao.getHabitsWithCompletions().combine(optimisticCompletionChanges) { habits, changes ->
            if (changes.isEmpty()) {
                habits
            } else {
                habits.map { habitWithCompletions ->
                    val override = changes[habitWithCompletions.habit.id]
                    if (override != null) {
                        habitWithCompletions.copy(completions = override)
                    } else {
                        habitWithCompletions
                    }
                }
            }
        }
            .flowOn(Dispatchers.Default)
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

    fun toggleCompletion(
        habit: Habit,
        date: Calendar,
        isCompleted: Boolean? = null,
        decrement: Boolean = false
    ) {
        val habitId = habit.id
        val dateInMillis = date.timeInMillis
        val now = Calendar.getInstance()
        val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()

        val startOfDay = normalizeToStartOfDay(dateInMillis)
        val endOfDay = normalizeToEndOfDay(dateInMillis)

        val habitStart = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
        val isBeforeStart = startOfDay < habitStart

        viewModelScope.launch {
            var currentHabit = habit
            if (isBeforeStart) {
                val updatedHabit = habit.copy(startDate = startOfDay.toString())
                habitDao.updateHabit(updatedHabit)
                currentHabit = updatedHabit
            }

            val target = currentHabit.getDailyTarget()
            val currentDbCompletions = habitDao.countCompletionsForHabitOnDay(habitId, startOfDay, endOfDay)

            val currentEffective = if (currentHabit.isInverse) {
                if (isBeforeStart) {
                    target
                } else {
                    (target - currentDbCompletions).coerceAtLeast(0)
                }
            } else {
                currentDbCompletions
            }

            val newEffective = if (currentHabit.isInverse) {
                if (decrement) {
                    if (currentEffective < target) currentEffective + 1 else 0
                } else {
                    if (currentEffective > 0) currentEffective - 1 else target
                }
            } else {
                if (decrement) {
                    (currentEffective - 1).coerceAtLeast(0)
                } else if (isCompleted != null && target == 1) {
                    if (isCompleted) 0 else target
                } else {
                    if (currentEffective < target) {
                        currentEffective + 1
                    } else {
                        0
                    }
                }
            }

            if (currentHabit.isInverse) {
                val newSlips = (target - newEffective).coerceAtLeast(0)
                val completion = if (newSlips > 0) {
                    Completion(
                        id = UUID.randomUUID().toString(),
                        habitId = habitId,
                        date = dateInMillis,
                        timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                        amountOfCompletions = newSlips
                    )
                } else null
                habitDao.setCompletionsForHabitOnDay(habitId, startOfDay, endOfDay, completion)
            } else {
                val completion = if (newEffective > 0) {
                    Completion(
                        id = UUID.randomUUID().toString(),
                        habitId = habitId,
                        date = dateInMillis,
                        timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                        amountOfCompletions = newEffective
                    )
                } else null
                habitDao.setCompletionsForHabitOnDay(habitId, startOfDay, endOfDay, completion)
            }
        }
    }

    fun updateHabitWithConversion(oldHabit: Habit, updatedHabit: Habit) {
        viewModelScope.launch {
            if (oldHabit.isInverse != updatedHabit.isInverse) {
                convertHabitCompletions(oldHabit, updatedHabit)
            } else {
                habitDao.updateHabit(updatedHabit)
            }
        }
    }

    private suspend fun convertHabitCompletions(oldHabit: Habit, newHabit: Habit) {
        val habitId = oldHabit.id
        val oldTarget = oldHabit.getDailyTarget()
        val newTarget = newHabit.getDailyTarget()

        val startDate = normalizeToStartOfDay(oldHabit.getEffectiveStartDateMillis())
        val todayEnd = normalizeToEndOfDay(System.currentTimeMillis())

        val existingCompletions = habitDao.getCompletionsForHabitSnapshot(habitId)

        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        val newCompletions = mutableListOf<Completion>()
        val now = Calendar.getInstance()
        val tzOffset = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()

        while (cal.timeInMillis <= todayEnd) {
            val dayStart = normalizeToStartOfDay(cal.timeInMillis)
            val dayEnd = normalizeToEndOfDay(cal.timeInMillis)

            val dbAmount = existingCompletions
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.amountOfCompletions }

            val oldEffective = if (oldHabit.isInverse) {
                (oldTarget - dbAmount).coerceAtLeast(0)
            } else {
                dbAmount
            }

            if (newHabit.isInverse) {
                val slips = (newTarget - oldEffective).coerceAtLeast(0)
                if (slips > 0) {
                    newCompletions.add(
                        Completion(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = dayStart + 12 * 3600 * 1000L,
                            timezoneOffsetInMinutes = tzOffset,
                            amountOfCompletions = slips
                        )
                    )
                }
            } else {
                val completions = oldEffective.coerceAtMost(newTarget)
                if (completions > 0) {
                    newCompletions.add(
                        Completion(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = dayStart + 12 * 3600 * 1000L,
                            timezoneOffsetInMinutes = tzOffset,
                            amountOfCompletions = completions
                        )
                    )
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        habitDao.deleteCompletionsForHabit(habitId)
        if (newCompletions.isNotEmpty()) {
            habitDao.insertCompletions(newCompletions)
        }
        habitDao.updateHabit(newHabit)
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

    fun updateHabitStatsLayout(habit: Habit, layout: String?) {
        viewModelScope.launch {
            habitDao.updateHabit(habit.copy(statsLayout = layout))
        }
    }

    fun applyStatsLayoutToAll(layout: String?) {
        viewModelScope.launch {
            val habits = habitDao.getAllHabitsSnapshot()
            val updatedHabits = habits.map { it.copy(statsLayout = layout) }
            habitDao.updateHabits(updatedHabits)
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