package com.example.attempt3

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
