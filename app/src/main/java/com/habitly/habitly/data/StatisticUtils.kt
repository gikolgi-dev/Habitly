/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data

import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.Database.getEffectiveStartDateMillis
import com.habitly.habitly.data.Database.normalizeToStartOfDay
import com.habitly.habitly.data.Database.normalizeToEndOfDay
import com.habitly.habitly.data.Database.getDailyTarget
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class HabitStatistics(
    val longestStreak: Int,
    val completionRatio: Int,
    val averageCompletionTime: String,
    val timeSinceCreation: Long,
    val daysSinceLongestStreak: Long,
    val currentStreak: Int,
    val totalCompletions: Int,
    val bestDayOfWeek: String,
    val rateLast30Days: Int
)

data class MonthlyCompletion(
    val monthLabel: String,
    val year: Int,
    val percentage: Float
)

fun getTotalEffectiveCompletions(habit: Habit, completions: List<Completion>, now: Long = System.currentTimeMillis()): Int {
    val startDate = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
    val today = normalizeToEndOfDay(now)
    if (startDate > today) return 0

    val dailyTarget = habit.getDailyTarget()

    if (!habit.isInverse) {
        val dbAmountsByDay = mutableMapOf<Long, Int>()
        completions.forEach {
            val dStart = normalizeToStartOfDay(it.date)
            dbAmountsByDay[dStart] = (dbAmountsByDay[dStart] ?: 0) + it.amountOfCompletions
        }
        return dbAmountsByDay.filterKeys { it in startDate..today }.values.sumOf { minOf(it, dailyTarget) }
    }

    val slipsByDay = mutableMapOf<Long, Int>()
    completions.forEach {
        val dStart = normalizeToStartOfDay(it.date)
        slipsByDay[dStart] = (slipsByDay[dStart] ?: 0) + it.amountOfCompletions
    }

    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
    var total = 0
    while (cal.timeInMillis <= today) {
        val dStart = normalizeToStartOfDay(cal.timeInMillis)
        val slips = slipsByDay[dStart] ?: 0
        total += (dailyTarget - slips).coerceAtLeast(0)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return total
}

fun getCompletedDays(habit: Habit, completions: List<Completion>, now: Long = System.currentTimeMillis()): Set<Long> {
    val startDate = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
    val today = normalizeToStartOfDay(now)
    val target = if (habit.intervalUnit == "day") {
        if (habit.completionsPerDay > 1) {
            habit.completionsPerInterval.coerceIn(1, habit.getDailyTarget())
        } else {
            1
        }
    } else {
        habit.getDailyTarget()
    }

    val dbAmountsByDay = mutableMapOf<Long, Int>()
    completions.forEach {
        val dStart = normalizeToStartOfDay(it.date)
        dbAmountsByDay[dStart] = (dbAmountsByDay[dStart] ?: 0) + it.amountOfCompletions
    }

    val completed = mutableSetOf<Long>()
    if (habit.isInverse) {
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        while (cal.timeInMillis <= today) {
            val dStart = normalizeToStartOfDay(cal.timeInMillis)
            val slips = dbAmountsByDay[dStart] ?: 0
            val effective = (target - slips).coerceAtLeast(0)
            if (effective >= target) {
                completed.add(dStart)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        dbAmountsByDay.forEach { (dStart, amount) ->
            if (amount >= target && dStart >= startDate && dStart <= today) {
                completed.add(dStart)
            }
        }
    }
    return completed
}

fun getEffectiveCompletionsByDay(habit: Habit, completions: List<Completion>, now: Long = System.currentTimeMillis()): Map<Long, Int> {
    val startDate = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
    val today = normalizeToStartOfDay(now)
    val target = habit.getDailyTarget()

    val dbAmountsByDay = mutableMapOf<Long, Int>()
    completions.forEach {
        val dStart = normalizeToStartOfDay(it.date)
        dbAmountsByDay[dStart] = (dbAmountsByDay[dStart] ?: 0) + it.amountOfCompletions
    }

    val result = mutableMapOf<Long, Int>()
    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
    while (cal.timeInMillis <= today) {
        val dStart = normalizeToStartOfDay(cal.timeInMillis)
        val count = if (habit.isInverse) {
            val slips = dbAmountsByDay[dStart] ?: 0
            (target - slips).coerceAtLeast(0)
        } else {
            val amount = dbAmountsByDay[dStart] ?: 0
            minOf(amount, target)
        }
        if (count > 0) {
            result[dStart] = count
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}

fun calculateStatistics(habitWithCompletions: HabitWithCompletions): HabitStatistics {
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions

    // 1. Longest Streak
    val (maxStreak, maxStreakEndDate) = calculateLongestStreak(habit, completions)

    // 2. Completion Ratio
    val now = System.currentTimeMillis()
    val totalCompletions = getTotalEffectiveCompletions(habit, completions, now)
    val effectiveStartDate = habit.getEffectiveStartDateMillis()
    val daysSinceCreation = max(1L, TimeUnit.MILLISECONDS.toDays(now - effectiveStartDate) + 1)

    val maxPossible = when(habit.intervalUnit) {
        "day" -> daysSinceCreation * habit.getDailyTarget()
        "week" -> (daysSinceCreation / 7 + 1) * habit.completionsPerInterval
        "month" -> (daysSinceCreation / 30 + 1) * habit.completionsPerInterval
        else -> daysSinceCreation * habit.getDailyTarget()
    }
    
    val ratio = if (maxPossible > 0) (totalCompletions.toFloat() / maxPossible) * 100 else 0f

    // Days since longest streak
    val daysSinceLongestStreak = if (maxStreak > 0) {
        val c1 = Calendar.getInstance()
        c1.timeInMillis = now
        val c2 = Calendar.getInstance()
        c2.timeInMillis = maxStreakEndDate
        
        c1.set(Calendar.HOUR_OF_DAY, 0)
        c1.set(Calendar.MINUTE, 0)
        c1.set(Calendar.SECOND, 0)
        c1.set(Calendar.MILLISECOND, 0)

        c2.set(Calendar.HOUR_OF_DAY, 0)
        c2.set(Calendar.MINUTE, 0)
        c2.set(Calendar.SECOND, 0)
        c2.set(Calendar.MILLISECOND, 0)
        
        val diff = c1.timeInMillis - c2.timeInMillis
        max(0L, TimeUnit.MILLISECONDS.toDays(diff))
    } else {
        0L
    }

    // 3. Average Completion Time (using circular mean)
    val avgTimeStr = if (completions.isNotEmpty()) {
        var sinSum = 0.0
        var cosSum = 0.0
        
        completions.forEach { completion ->
            val localTime = completion.date + (completion.timezoneOffsetInMinutes * 60 * 1000)
            val millisInDay = localTime % (24 * 60 * 60 * 1000)
            val angle = (millisInDay.toDouble() / (24 * 60 * 60 * 1000)) * 2 * Math.PI
            sinSum += sin(angle)
            cosSum += cos(angle)
        }
        
        val avgSin = sinSum / completions.size
        val avgCos = cosSum / completions.size
        
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

    val currentStreak = calculateCurrentStreak(habit, completions)
    val totalCompletionsVal = totalCompletions
    val bestDayOfWeek = calculateBestDayOfWeek(habit, completions)
    val rateLast30Days = calculateRateLast30Days(habit, completions)

    return HabitStatistics(
        longestStreak = maxStreak,
        completionRatio = ratio.toInt(),
        averageCompletionTime = avgTimeStr,
        timeSinceCreation = maxPossible,
        daysSinceLongestStreak = daysSinceLongestStreak,
        currentStreak = currentStreak,
        totalCompletions = totalCompletionsVal,
        bestDayOfWeek = bestDayOfWeek,
        rateLast30Days = rateLast30Days
    )
}

private fun calculateLongestStreak(habit: Habit, completions: List<Completion>, now: Long = System.currentTimeMillis()): Pair<Int, Long> {
    val completedDays = getCompletedDays(habit, completions, now)
    if (completedDays.isEmpty()) return 0 to 0L
    return when (habit.intervalUnit) {
        "day" -> {
             val sortedDays = completedDays.sorted()
             var maxStreak = 0
             var maxStreakEnd = 0L
             var currentStreak = 0
             var currentStreakEnd = 0L
             val minDate = sortedDays.first()
             val maxDate = sortedDays.last()
             
             val c = Calendar.getInstance()
             c.timeInMillis = minDate
             
             while (c.timeInMillis <= maxDate) {
                 if (completedDays.contains(c.timeInMillis)) {
                     currentStreak++
                     currentStreakEnd = c.timeInMillis
                 } else {
                     if (currentStreak > maxStreak) {
                         maxStreak = currentStreak
                         maxStreakEnd = currentStreakEnd
                     }
                     currentStreak = 0
                 }
                 c.add(Calendar.DAY_OF_YEAR, 1)
             }
             if (currentStreak > maxStreak) {
                 maxStreak = currentStreak
                 maxStreakEnd = currentStreakEnd
             }
             maxStreak to maxStreakEnd
        }
        "week" -> {
            calculatePeriodStreak(habit, completions, Calendar.WEEK_OF_YEAR, now)
        }
        "month" -> {
            calculatePeriodStreak(habit, completions, Calendar.MONTH, now)
        }
        else -> 0 to 0L
    }
}

private fun calculatePeriodStreak(habit: Habit, completions: List<Completion>, calendarField: Int, now: Long = System.currentTimeMillis()): Pair<Int, Long> {
    val dailyCompletions = getEffectiveCompletionsByDay(habit, completions, now)
    if (dailyCompletions.isEmpty()) return 0 to 0L
    
    val minDate = dailyCompletions.keys.minOrNull()!!
    
    val c = Calendar.getInstance()
    c.timeInMillis = minDate
    
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    
    if (calendarField == Calendar.WEEK_OF_YEAR) {
        val firstDayOfWeek = c.firstDayOfWeek
        while (c.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
    } else if (calendarField == Calendar.MONTH) {
        c.set(Calendar.DAY_OF_MONTH, 1)
    }
    
    var maxStreak = 0
    var maxStreakEnd = 0L
    var currentStreak = 0
    var currentStreakEnd = 0L
    
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val dailyTarget = habit.getDailyTarget()
    
    while (c.timeInMillis <= nowCal.timeInMillis) {
        val startOfPeriod = c.timeInMillis
        val nextPeriod = c.clone() as Calendar
        nextPeriod.add(calendarField, 1)
        val endOfPeriod = nextPeriod.timeInMillis - 1
        
        var completionsInPeriod = 0
        val checkCal = c.clone() as Calendar
        while (checkCal.timeInMillis <= endOfPeriod) {
            completionsInPeriod += (dailyCompletions[checkCal.timeInMillis] ?: 0)
            checkCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val target = habit.completionsPerInterval
        val isCurrentPeriod = (now in startOfPeriod..endOfPeriod)
        
        if (isCurrentPeriod) {
             val diff = now - startOfPeriod
             val daysPassed = TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
             val capacityPassed = daysPassed * dailyTarget
             val currentMisses = capacityPassed - completionsInPeriod
             
             val totalDaysInPeriod = if (calendarField == Calendar.WEEK_OF_YEAR) 7 else c.getActualMaximum(Calendar.DAY_OF_MONTH)
             val totalCapacity = totalDaysInPeriod * dailyTarget
             val allowedMisses = totalCapacity - target
             
             if (currentMisses <= allowedMisses) {
                 currentStreak += completionsInPeriod
                 currentStreakEnd = now 
             } else {
                 if (currentStreak > maxStreak) {
                     maxStreak = currentStreak
                     maxStreakEnd = currentStreakEnd
                 }
                 
                 var tail = 0
                 val scanC = Calendar.getInstance().apply { timeInMillis = now }
                 var counting = false
                 var tailEnd = 0L
                 
                 while (scanC.timeInMillis >= startOfPeriod) {
                      val count = dailyCompletions[scanC.timeInMillis] ?: 0
                      if (count > 0) {
                          if (!counting) {
                              tailEnd = scanC.timeInMillis
                          }
                          counting = true
                          tail += count
                      } else {
                          if (counting) break
                      }
                      scanC.add(Calendar.DAY_OF_YEAR, -1)
                 }
                 currentStreak = tail
                 if (tail > 0) currentStreakEnd = tailEnd
             }
        } else {
            if (completionsInPeriod >= target) {
                currentStreak += completionsInPeriod
                currentStreakEnd = endOfPeriod
            } else {
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                    maxStreakEnd = currentStreakEnd
                }
                currentStreak = 0 
            }
        }
        
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak
            maxStreakEnd = currentStreakEnd
        }
        c.add(calendarField, 1)
    }
    
    return maxStreak to maxStreakEnd
}


fun calculateMonthlyStats(habitWithCompletions: HabitWithCompletions): List<MonthlyCompletion> {
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions
    val effectiveStartDate = habit.getEffectiveStartDateMillis()

    val calendar = Calendar.getInstance()
    val nowCalendar = Calendar.getInstance()
    
    // Start of today
    val startOfTodayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = startOfTodayCal.timeInMillis

    calendar.timeInMillis = effectiveStartDate
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val stats = mutableListOf<MonthlyCompletion>()

    while (true) {
        val loopYear = calendar.get(Calendar.YEAR)
        val loopMonth = calendar.get(Calendar.MONTH)
        
        if (loopYear > nowCalendar.get(Calendar.YEAR) || 
            (loopYear == nowCalendar.get(Calendar.YEAR) && loopMonth > nowCalendar.get(Calendar.MONTH))) {
            break
        }

        val isCurrentMonth = loopYear == nowCalendar.get(Calendar.YEAR) && loopMonth == nowCalendar.get(Calendar.MONTH)
        
        val daysToCount: Int
        val effectiveEndOfRange: Long

        if (isCurrentMonth) {
            daysToCount = nowCalendar.get(Calendar.DAY_OF_MONTH) - 1
            if (daysToCount <= 0) {
                // If it's the first day of the month, don't show the current month as requested
                break
            }
            effectiveEndOfRange = startOfToday - 1
        } else {
            daysToCount = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val nextMonthCal = calendar.clone() as Calendar
            nextMonthCal.add(Calendar.MONTH, 1)
            effectiveEndOfRange = nextMonthCal.timeInMillis - 1
        }

        val startOfMonth = calendar.timeInMillis
        
        val completionsInMonth = if (!habit.isInverse) {
            completions.filter { 
                it.date in startOfMonth..effectiveEndOfRange 
            }.sumOf { it.amountOfCompletions }
        } else {
            val habitStart = normalizeToStartOfDay(effectiveStartDate)
            val dayCal = calendar.clone() as Calendar
            var sum = 0
            val target = habit.completionsPerInterval.coerceAtLeast(1)
            val slipsMap = mutableMapOf<Long, Int>()
            completions.forEach {
                val d = normalizeToStartOfDay(it.date)
                slipsMap[d] = (slipsMap[d] ?: 0) + it.amountOfCompletions
            }
            while (dayCal.timeInMillis <= effectiveEndOfRange) {
                val d = normalizeToStartOfDay(dayCal.timeInMillis)
                if (d >= habitStart) {
                    val slips = slipsMap[d] ?: 0
                    sum += (target - slips).coerceAtLeast(0)
                }
                dayCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            sum
        }

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

fun calculateCurrentStreak(habit: Habit, completions: List<Completion>, now: Long = System.currentTimeMillis()): Int {
    val completedDays = getCompletedDays(habit, completions, now)
    val dailyCompletions = getEffectiveCompletionsByDay(habit, completions, now)
    if (dailyCompletions.isEmpty()) return 0

    val today = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    if (habit.intervalUnit == "day") {
        val hasCompletedToday = completedDays.contains(today)
        val hasCompletedYesterday = completedDays.contains(yesterday)
        val startDay = when {
            hasCompletedToday -> today
            hasCompletedYesterday -> yesterday
            else -> return 0
        }
        var streakDays = 0
        val c = Calendar.getInstance().apply { timeInMillis = startDay }
        while (completedDays.contains(c.timeInMillis)) {
            streakDays++
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streakDays
    } else {
        val calendarField = if (habit.intervalUnit == "week") Calendar.WEEK_OF_YEAR else Calendar.MONTH
        var currentStreak = 0
        val c = Calendar.getInstance().apply { timeInMillis = today }
        val dailyTarget = habit.getDailyTarget()
        
        while (true) {
            if (calendarField == Calendar.WEEK_OF_YEAR) {
                val firstDayOfWeek = c.firstDayOfWeek
                while (c.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                    c.add(Calendar.DAY_OF_YEAR, -1)
                }
            } else {
                c.set(Calendar.DAY_OF_MONTH, 1)
            }
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            
            val startOfPeriod = c.timeInMillis
            val nextPeriod = c.clone() as Calendar
            nextPeriod.add(calendarField, 1)
            val endOfPeriod = nextPeriod.timeInMillis - 1
            
            var completionsInPeriod = 0
            val checkCal = c.clone() as Calendar
            while (checkCal.timeInMillis <= endOfPeriod) {
                completionsInPeriod += (dailyCompletions[checkCal.timeInMillis] ?: 0)
                checkCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val target = habit.completionsPerInterval
            val isCurrentPeriod = (now in startOfPeriod..endOfPeriod)
            
            if (isCurrentPeriod) {
                val diff = now - startOfPeriod
                val daysPassed = TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
                val capacityPassed = daysPassed * dailyTarget
                val currentMisses = capacityPassed - completionsInPeriod
                val totalDaysInPeriod = if (calendarField == Calendar.WEEK_OF_YEAR) 7 else c.getActualMaximum(Calendar.DAY_OF_MONTH)
                val totalCapacity = totalDaysInPeriod * dailyTarget
                val allowedMisses = totalCapacity - target
                
                if (currentMisses <= allowedMisses) {
                    currentStreak += completionsInPeriod
                } else {
                    var tail = 0
                    val scanC = Calendar.getInstance().apply { timeInMillis = today }
                    var counting = false
                    while (scanC.timeInMillis >= startOfPeriod) {
                        val count = dailyCompletions[scanC.timeInMillis] ?: 0
                        if (count > 0) {
                            counting = true
                            tail += count
                        } else {
                            if (counting) break
                        }
                        scanC.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    return tail
                }
            } else {
                if (completionsInPeriod >= target) {
                    currentStreak += completionsInPeriod
                } else {
                    break
                }
            }
            
            c.add(calendarField, -1)
            
            val minDate = habit.getEffectiveStartDateMillis()
            if (c.timeInMillis < minDate - (31L * 24 * 3600 * 1000)) {
                break
            }
        }
        return currentStreak
    }
}

fun calculateBestDayOfWeek(habit: Habit, completions: List<Completion>): String {
    if (!habit.isInverse) {
        if (completions.isEmpty()) return "N/A"
        val dayCounts = IntArray(8)
        val c = Calendar.getInstance()
        completions.forEach { completion ->
            c.timeInMillis = completion.date
            val day = c.get(Calendar.DAY_OF_WEEK)
            dayCounts[day] += completion.amountOfCompletions
        }
        var bestDay = Calendar.SUNDAY
        var maxCount = -1
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            if (dayCounts[day] > maxCount) {
                maxCount = dayCounts[day]
                bestDay = day
            }
        }
        if (maxCount <= 0) return "N/A"
        c.set(Calendar.DAY_OF_WEEK, bestDay)
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "N/A"
    } else {
        val startDate = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val cleanDayCounts = IntArray(8)
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        val slipsByDay = mutableMapOf<Long, Int>()
        completions.forEach {
            val d = normalizeToStartOfDay(it.date)
            slipsByDay[d] = (slipsByDay[d] ?: 0) + it.amountOfCompletions
        }
        while (cal.timeInMillis <= today) {
            val d = normalizeToStartOfDay(cal.timeInMillis)
            if ((slipsByDay[d] ?: 0) == 0) {
                cleanDayCounts[cal.get(Calendar.DAY_OF_WEEK)]++
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        var bestDay = Calendar.SUNDAY
        var maxCount = -1
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            if (cleanDayCounts[day] > maxCount) {
                maxCount = cleanDayCounts[day]
                bestDay = day
            }
        }
        if (maxCount <= 0) return "N/A"
        cal.set(Calendar.DAY_OF_WEEK, bestDay)
        return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "N/A"
    }
}

fun calculateRateLast30Days(habit: Habit, completions: List<Completion>): Int {
    val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
    val target = habit.getDailyTarget()
    if (!habit.isInverse) {
        val completionsLast30 = completions.filter { it.date >= thirtyDaysAgo }.sumOf { it.amountOfCompletions }
        val maxPossible = 30f * target
        val rate = (completionsLast30.toFloat() / maxPossible) * 100f
        return rate.coerceIn(0f, 100f).roundToInt()
    } else {
        val startDate = normalizeToStartOfDay(habit.getEffectiveStartDateMillis())
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        var total = 0
        var daysCounted = 0
        val cal = Calendar.getInstance().apply { timeInMillis = maxOf(startDate, normalizeToStartOfDay(thirtyDaysAgo)) }
        while (cal.timeInMillis <= today) {
            val dStart = normalizeToStartOfDay(cal.timeInMillis)
            val dEnd = normalizeToEndOfDay(cal.timeInMillis)
            val slips = completions.filter { it.date in dStart..dEnd }.sumOf { it.amountOfCompletions }
            total += (target - slips).coerceAtLeast(0)
            daysCounted++
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val maxPossible = maxOf(1, daysCounted) * target
        val rate = (total.toFloat() / maxPossible.toFloat()) * 100f
        return rate.coerceIn(0f, 100f).roundToInt()
    }
}
