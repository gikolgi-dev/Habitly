package com.example.attempt3

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object StreakCalculator {

    fun calculateStreak(habit: Habit, completions: List<Completion>): Int {
        if (completions.isEmpty()) return 0

        // Determine how many days one interval represents
        val intervalInDays = when (habit.intervalUnit.lowercase()) {
            "day" -> habit.intervalValue.toLong()
            "week" -> habit.intervalValue * 7L
            else -> return 0 // Only day/week supported
        }

        if (intervalInDays <= 0) return 0

        // Target completions per day
        val target = habit.completionsPerInterval.toDouble() / intervalInDays

        // Group completions by their local date
        val completionsByDay = completions.groupBy {
            val zoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(it.timezoneOffsetInMinutes * -60))
            Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate()
        }.mapValues { (_, list) -> list.sumOf { it.amountOfCompletions } }

        // Determine the date of the most recent completion
        val latestCompletion = completions.maxByOrNull { it.date } ?: return 0
        val latestZone = ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(latestCompletion.timezoneOffsetInMinutes * -60))
        val latestDate = Instant.ofEpochMilli(latestCompletion.date).atZone(latestZone).toLocalDate()

        // Now calculate streak
        var streakDays = 0
        var totalCompletions = 0

        // Limit to 10 years just in case (performance safety)
        for (i in 1..(365 * 10)) {
            val dateToCheck = latestDate.minusDays(i.toLong() - 1)
            totalCompletions += completionsByDay.getOrDefault(dateToCheck, 0)

            val average = totalCompletions.toDouble() / i
            if (average < target) {
                streakDays = i - 1
                break
            }

            // If we reach the end of the loop without dropping below target
            streakDays = i
        }

        return streakDays
    }
}
