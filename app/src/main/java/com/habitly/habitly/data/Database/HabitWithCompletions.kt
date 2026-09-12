/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.Database

import androidx.room.Embedded
import androidx.room.Relation
import java.util.Calendar
import java.util.TimeZone

data class HabitWithCompletions(
    @Embedded val habit: Habit,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val completions: List<Completion>
)

fun normalizeToStartOfDay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val cal = Calendar.getInstance(timeZone)
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun normalizeToEndOfDay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val cal = Calendar.getInstance(timeZone)
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/**
 * Returns effective completion count for a habit on a given day.
 *
 * For a Build habit:
 *   effectiveCompletions = amount of completions stored in DB for this day.
 *
 * For a Quit habit (inverse habit):
 *   Lack of completion in DB = completed by default!
 *   Stored completions in DB represent slip-ups / relapses (amount of uncompletions).
 *   effectiveCompletions = (target - slips).coerceAtLeast(0).
 *
 * Before the habit's effective start date, completions are 0.
 * In the future, completions are 0.
 */
fun getEffectiveCompletionsForDay(
    habit: Habit,
    completions: List<Completion>,
    dayMillis: Long,
    currentDateMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): Int {
    val dayStart = normalizeToStartOfDay(dayMillis, timeZone)
    val todayStart = normalizeToStartOfDay(currentDateMillis, timeZone)
    val habitStart = normalizeToStartOfDay(habit.getEffectiveStartDateMillis(), timeZone)

    if (dayStart < habitStart || dayStart > todayStart) {
        return 0
    }

    val dayEnd = normalizeToEndOfDay(dayMillis, timeZone)
    val dbAmount = completions
        .filter { it.date in dayStart..dayEnd }
        .sumOf { it.amountOfCompletions }

    val target = habit.getDailyTarget()

    return if (habit.isInverse) {
        (target - dbAmount).coerceAtLeast(0)
    } else {
        minOf(dbAmount, target)
    }
}

fun isDayCompleted(
    habit: Habit,
    completions: List<Completion>,
    dayMillis: Long,
    currentDateMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): Boolean {
    val target = habit.getDailyTarget()
    val effective = getEffectiveCompletionsForDay(habit, completions, dayMillis, currentDateMillis, timeZone)
    return effective >= target
}

fun getCompletionRatioForDay(
    habit: Habit,
    completions: List<Completion>,
    dayMillis: Long,
    currentDateMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): Float {
    val dayStart = normalizeToStartOfDay(dayMillis, timeZone)
    val todayStart = normalizeToStartOfDay(currentDateMillis, timeZone)
    val habitStart = normalizeToStartOfDay(habit.getEffectiveStartDateMillis(), timeZone)

    if (dayStart < habitStart || dayStart > todayStart) {
        return 0f
    }

    val target = habit.getDailyTarget()
    val effective = getEffectiveCompletionsForDay(habit, completions, dayMillis, currentDateMillis, timeZone)
    return (effective.toFloat() / target).coerceIn(0f, 1f)
}

fun HabitWithCompletions.effectiveCompletionsOnDay(dayMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): Int =
    getEffectiveCompletionsForDay(habit, completions, dayMillis, currentDateMillis)

fun HabitWithCompletions.isDayCompleted(dayMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): Boolean =
    com.habitly.habitly.data.Database.isDayCompleted(habit, completions, dayMillis, currentDateMillis)

fun HabitWithCompletions.completionRatioOnDay(dayMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): Float =
    getCompletionRatioForDay(habit, completions, dayMillis, currentDateMillis)