package com.example.attempt3.data

import com.example.attempt3.data.Database.HabitWithCompletions
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class HabitStatistics(
    val longestStreak: Int,
    val completionRatio: Int,
    val averageCompletionTime: String,
    val timeSinceCreation: Long
)

data class MonthlyCompletion(
    val monthLabel: String,
    val year: Int,
    val percentage: Float
)

fun calculateStatistics(habitWithCompletions: HabitWithCompletions): HabitStatistics {
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions

    // 1. Longest Streak
    val completionsByDay = completions.groupBy { completion ->
        val localTime = completion.date + (completion.timezoneOffsetInMinutes * 60 * 1000)
        TimeUnit.MILLISECONDS.toDays(localTime)
    }

    val successfulDays = completionsByDay.filter { (_, dayCompletions) ->
        dayCompletions.sumOf { it.amountOfCompletions } >= habit.completionsPerInterval
    }.keys.sorted()

    var maxStreak = 0
    var currentStreak = 0
    var lastDay: Long? = null

    for (day in successfulDays) {
        if (lastDay != null && day == lastDay + 1) {
            currentStreak++
        } else {
            currentStreak = 1
        }
        maxStreak = max(maxStreak, currentStreak)
        lastDay = day
    }

    // 2. Completion Ratio
    val totalCompletions = completions.sumOf { it.amountOfCompletions }
    val createdAt = habit.createdAt.toLongOrNull() ?: System.currentTimeMillis()
    val now = System.currentTimeMillis()
    val firstCompletionDate = completions.minOfOrNull { it.date }
    
    // Use the earlier of creation date or first completion date to handle historical data
    val effectiveStartDate = if (firstCompletionDate != null) {
        min(createdAt, firstCompletionDate)
    } else {
        createdAt
    }
    
    val daysSinceCreation = max(1L, TimeUnit.MILLISECONDS.toDays(now - effectiveStartDate) + 1)

    val maxPossible = when(habit.intervalUnit) {
        "day" -> daysSinceCreation * habit.completionsPerInterval
        "week" -> (daysSinceCreation / 7 + 1) * habit.completionsPerInterval
        "month" -> (daysSinceCreation / 30 + 1) * habit.completionsPerInterval
        else -> daysSinceCreation * habit.completionsPerInterval
    }
    
    val ratio = if (maxPossible > 0) (totalCompletions.toFloat() / maxPossible) * 100 else 0f

    // 3. Average Completion Time (using circular mean)
    val avgTimeStr = if (completions.isNotEmpty()) {
        var sinSum = 0.0
        var cosSum = 0.0
        
        completions.forEach { completion ->
            // Convert time of day to radians
            // We use local time for the "time of day"
            val localTime = completion.date + (completion.timezoneOffsetInMinutes * 60 * 1000)
            val millisInDay = localTime % (24 * 60 * 60 * 1000)
            // Normalize to [0, 2*PI]
            // 24h = 2*PI
            val angle = (millisInDay.toDouble() / (24 * 60 * 60 * 1000)) * 2 * Math.PI
            sinSum += sin(angle)
            cosSum += cos(angle)
        }
        
        val avgSin = sinSum / completions.size
        val avgCos = cosSum / completions.size
        
        // Convert back to angle
        var avgAngle = atan2(avgSin, avgCos)
        if (avgAngle < 0) avgAngle += 2 * Math.PI
        
        val avgMillisInDay = (avgAngle / (2 * Math.PI)) * (24 * 60 * 60 * 1000)
        val avgMillis = avgMillisInDay.toLong()
        
        val hours = TimeUnit.MILLISECONDS.toHours(avgMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(avgMillis) % 60
        String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    } else {
        "N/A"
    }

    return HabitStatistics(maxStreak, ratio.toInt(), avgTimeStr,  maxPossible)
}

fun calculateMonthlyStats(habitWithCompletions: HabitWithCompletions): List<MonthlyCompletion> {
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions
    val createdAt = habit.createdAt.toLongOrNull() ?: System.currentTimeMillis()
    val firstCompletionDate = completions.minOfOrNull { it.date }
    
    val effectiveStartDate = if (firstCompletionDate != null) {
        min(createdAt, firstCompletionDate)
    } else {
        createdAt
    }

    val calendar = Calendar.getInstance()
    val nowCalendar = Calendar.getInstance()
    
    calendar.timeInMillis = effectiveStartDate
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val stats = mutableListOf<MonthlyCompletion>()

    while (true) {
        if (calendar.get(Calendar.YEAR) > nowCalendar.get(Calendar.YEAR) || 
            (calendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) > nowCalendar.get(Calendar.MONTH))) {
            break
        }

        val loopYear = calendar.get(Calendar.YEAR)
        val loopMonth = calendar.get(Calendar.MONTH)
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysToCount = if (loopYear == nowCalendar.get(Calendar.YEAR) && loopMonth == nowCalendar.get(Calendar.MONTH)) {
            nowCalendar.get(Calendar.DAY_OF_MONTH)
        } else {
            daysInMonth
        }

        val startOfMonth = calendar.timeInMillis
        val nextMonthCal = calendar.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)
        val endOfMonth = nextMonthCal.timeInMillis - 1

        val completionsInMonth = completions.filter { 
            it.date in startOfMonth..endOfMonth 
        }.sumOf { it.amountOfCompletions }

        val possibleCompletions = if (habit.intervalUnit == "day") {
            daysToCount * habit.completionsPerInterval
        } else {
            daysToCount
        }

        val percentage = if (possibleCompletions > 0) {
            (completionsInMonth.toFloat() / possibleCompletions) * 100f
        } else {
            0f
        }
        
        stats.add(MonthlyCompletion(
            monthLabel = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "",
            year = loopYear,
            percentage = percentage.coerceAtMost(100f)
        ))

        calendar.add(Calendar.MONTH, 1)
    }
    return stats
}
