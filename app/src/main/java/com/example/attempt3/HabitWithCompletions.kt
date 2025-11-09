package com.example.attempt3

import androidx.room.Embedded
import androidx.room.Relation

data class HabitWithCompletions(
    @Embedded val habit: Habit,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val completions: List<Completion>
) {
    val streak: Int
        get() = StreakCalculator.calculateStreak(habit, completions)
}
