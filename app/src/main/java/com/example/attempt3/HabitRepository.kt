package com.example.attempt3

import android.content.Context

class HabitRepository(context: Context) {

    private val habitDao = HabitDatabase.getDatabase(context).habitDao()

    suspend fun getHabit(habitId: String): Habit? {
        return habitDao.getHabit(habitId)
    }
}