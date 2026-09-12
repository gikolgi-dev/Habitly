/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.welcome

import com.habitly.habitly.BuildConfig
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.Database.normalizeToStartOfDay
import com.habitly.habitly.data.calculateMonthlyStats
import com.habitly.habitly.data.calculateStatistics
import com.habitly.habitly.data.calculateStreakData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Context passed into rule conditions to evaluate candidate welcome messages.
 */
data class WelcomeCardContext(
    val habits: List<HabitWithCompletions>,
    val todayMillis: Long,
    val firstDayOfWeek: Int = Calendar.MONDAY
)

/**
 * Result of a rule condition evaluation indicating a matched category and optional target habit.
 */
data class WelcomeCategoryMatch(
    val categoryId: String,
    val habitId: String? = null,
    val variables: Map<String, String> = emptyMap()
)

/**
 * Modular definition of a welcome card category / rule.
 * Easily modifiable: just conditions, priority, templates with variables, and variable extraction.
 */
data class WelcomeCategory(
    val id: String,
    val priority: Int,
    val templates: List<String>,
    val condition: (context: WelcomeCardContext) -> WelcomeCategoryMatch?,
    val extractVariables: (context: WelcomeCardContext, habitId: String?) -> Map<String, String>
)

/**
 * Final resolved message for display on the welcome card.
 */
data class WelcomeCardResolution(
    val dateKey: String,
    val categoryId: String,
    val habitId: String?,
    val templateIndex: Int,
    val text: String,
    val variables: Map<String, String>,
    val isNewSelection: Boolean
)

object WelcomeCardEngine {

    const val CATEGORY_LONGEST_STREAK_RECORD = "longest_streak_record"
    const val CATEGORY_MONTHLY_TREND = "monthly_trend"
    const val CATEGORY_HABIT_QUIT_MILESTONE = "habit_quit_milestone"
    const val CATEGORY_LONG_TIME_TRACKING = "long_time_tracking"
    const val CATEGORY_HIGH_COMPLETION_RATE = "high_completion_rate"
    const val CATEGORY_CURRENT_STREAK = "current_streak"
    const val CATEGORY_FALLBACK = "fallback"

    /**
     * Checks if a streak number represents a milestone / round number.
     */
    fun isRoundStreak(streak: Int): Boolean {
        if (streak <= 0) return false
        val milestoneDays = setOf(
            3, 5, 7, 10, 14, 21, 30, 45, 50, 60, 75, 90, 100, 150, 200, 250, 300, 365, 500, 750, 1000
        )
        return streak in milestoneDays || streak % 50 == 0 || streak % 100 == 0
    }

    /**
     * Checks if tracking days count represents a notable milestone.
     */
    fun isRoundTrackingDays(days: Long): Boolean {
        if (days <= 0) return false
        val milestoneDays = setOf(
            30L, 60L, 90L, 100L, 180L, 200L, 300L, 365L, 500L, 730L, 1000L
        )
        return days in milestoneDays || (days >= 100 && days % 100 in 0L..2L)
    }

    /**
     * Converts timestamp to "yyyy-MM-dd" date key.
     */
    fun getFormattedDateKey(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(millis))
    }

    /**
     * Extracts completed monthly statistics up to the preceding month for evaluating trends.
     */
    fun getCompletedMonthlyStats(hwc: HabitWithCompletions, now: Long): List<com.habitly.habitly.data.MonthlyCompletion> {
        val stats = calculateMonthlyStats(hwc, now)
        if (stats.isEmpty()) return emptyList()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentMonthLabel = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
        val currentYear = cal.get(Calendar.YEAR)
        return if (stats.last().monthLabel == currentMonthLabel && stats.last().year == currentYear) {
            stats.dropLast(1)
        } else {
            stats
        }
    }

    /**
     * Replaces variable tokens (e.g. {habitName}, {streak}) with their string values.
     */
    fun formatTemplate(template: String, variables: Map<String, String>): String {
        var result = template
        for ((key, value) in variables) {
            result = result.replace("{$key}", value)
        }
        return result
    }

    /**
     * Default list of categories and conditions, ordered by priorities.
     */
    val DEFAULT_WELCOME_CATEGORIES: List<WelcomeCategory> = listOf(
        // 1. Longest Streak Record (Very High Priority: 100)
        WelcomeCategory(
            id = CATEGORY_LONGEST_STREAK_RECORD,
            priority = 100,
            templates = listOf(
                "New personal best! You've reached a {streak}-day streak on {habitName}.",
                "You're at an all-time high: {streak} consecutive days of {habitName}.",
                "{streak} days and counting—this is your longest streak ever for {habitName}.",
                "Record-setting consistency! {habitName} is at its longest streak yet.",
                "Unstoppable: {streak} days on {habitName}, your best run so far."
            ),
            condition = { context ->
                val activeHabits = context.habits.filter { !it.habit.archived }
                val startOfToday = normalizeToStartOfDay(context.todayMillis)

                var bestHabit: HabitWithCompletions? = null
                var bestStreak = 0

                for (hwc in activeHabits) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    val currentStreak = streakData.currentStreak
                    val longestStreak = streakData.longestStreak

                    if (currentStreak >= 3 && currentStreak >= longestStreak) {
                        // Avoid over-displaying daily: show when achieving record recently or on round/milestone days
                        val isRecentlyAchieved = streakData.longestStreakEndDate >= (startOfToday - 86400000L)
                        val isMilestoneDay = isRoundStreak(currentStreak)
                        if (isMilestoneDay || isRecentlyAchieved) {
                            if (currentStreak > bestStreak) {
                                bestStreak = currentStreak
                                bestHabit = hwc
                            }
                        }
                    }
                }

                bestHabit?.let {
                    WelcomeCategoryMatch(
                        categoryId = CATEGORY_LONGEST_STREAK_RECORD,
                        habitId = it.habit.id
                    )
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "streak" to streakData.currentStreak.coerceAtLeast(1).toString()
                    )
                } else emptyMap()
            }
        ),

        // 2. Monthly Trend Improvement (Slightly Lower High Priority: 85)
        WelcomeCategory(
            id = CATEGORY_MONTHLY_TREND,
            priority = 85,
            templates = listOf(
                "Strong progress: your consistency with {habitName} improved for {consecutive} consecutive months.",
                "Upward trend: your completion rate for {habitName} has been climbing for {consecutive} months in a row.",
                "A great start to the month: your {habitName} consistency has risen for {consecutive} consecutive months.",
                "Building momentum: your consistency with {habitName} has grown for {consecutive} months in a row.",
                "Great start to the month: your {habitName} consistency jumped to {lastMonthRate}% last month.",
                "Looking back at last month, your completion rate for {habitName} improved significantly."
            ),
            condition = { context ->
                val cal = Calendar.getInstance().apply { timeInMillis = context.todayMillis }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                // Only evaluate during the first week of the month
                if (dayOfMonth > 7) {
                    null
                } else {
                    val activeHabits = context.habits.filter { !it.habit.archived }
                    var bestHabit: HabitWithCompletions? = null
                    var bestConsecutive = 0

                    for (hwc in activeHabits) {
                        val completedStats = getCompletedMonthlyStats(hwc, context.todayMillis)
                        if (completedStats.size >= 2) {
                            var consecutive = 0
                            for (i in completedStats.indices.reversed()) {
                                if (i > 0 && completedStats[i].percentage > completedStats[i - 1].percentage && completedStats[i].percentage > 0f) {
                                    consecutive++
                                } else {
                                    break
                                }
                            }
                            val lastDiff = completedStats.last().percentage - completedStats[completedStats.size - 2].percentage
                            if (consecutive >= 2 || (consecutive == 1 && lastDiff >= 10f)) {
                                if (consecutive > bestConsecutive || bestHabit == null) {
                                    bestConsecutive = consecutive
                                    bestHabit = hwc
                                }
                            }
                        }
                    }

                    bestHabit?.let {
                        WelcomeCategoryMatch(
                            categoryId = CATEGORY_MONTHLY_TREND,
                            habitId = it.habit.id
                        )
                    }
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val completedStats = getCompletedMonthlyStats(hwc, context.todayMillis)
                    var consecutive = 0
                    if (completedStats.size >= 2) {
                        for (i in completedStats.indices.reversed()) {
                            if (i > 0 && completedStats[i].percentage > completedStats[i - 1].percentage && completedStats[i].percentage > 0f) {
                                consecutive++
                            } else {
                                break
                            }
                        }
                    }
                    val consecutiveCount = if (consecutive >= 2) consecutive + 1 else 2
                    val lastRate = completedStats.lastOrNull()?.percentage?.roundToInt() ?: 0
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "consecutive" to consecutiveCount.toString(),
                        "lastMonthRate" to lastRate.toString()
                    )
                } else emptyMap()
            }
        ),

        // 3. Habit Quitting Milestone (Priority: 75)
        WelcomeCategory(
            id = CATEGORY_HABIT_QUIT_MILESTONE,
            priority = 75,
            templates = listOf(
                "Staying strong: {streak} days free from {habitName}.",
                "You've gone {streak} consecutive days without {habitName}. Keep up the great work.",
                "{streak} days clean on {habitName}. You are in control.",
                "Milestone reached: {streak} days staying away from {habitName}.",
                "Quiet discipline: {streak} days free from {habitName}."
            ),
            condition = { context ->
                val quitHabits = context.habits.filter { !it.habit.archived && it.habit.isInverse }
                var bestHabit: HabitWithCompletions? = null
                var bestStreak = 0

                for (hwc in quitHabits) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    val streak = streakData.currentStreak
                    if (streak >= 3) {
                        if (isRoundStreak(streak) || streak % 7 == 0 || streak >= streakData.longestStreak) {
                            if (streak > bestStreak) {
                                bestStreak = streak
                                bestHabit = hwc
                            }
                        }
                    }
                }

                bestHabit?.let {
                    WelcomeCategoryMatch(
                        categoryId = CATEGORY_HABIT_QUIT_MILESTONE,
                        habitId = it.habit.id
                    )
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "streak" to streakData.currentStreak.toString()
                    )
                } else emptyMap()
            }
        ),

        // 4. Long-Time Tracking Milestone (Priority: 65)
        WelcomeCategory(
            id = CATEGORY_LONG_TIME_TRACKING,
            priority = 65,
            templates = listOf(
                "Dedication in action: you've been tracking {habitName} for {days} days now.",
                "{days} days of tracking {habitName}. Building long-term habits takes time, and you're doing it.",
                "You've stayed committed to {habitName} for over {days} days.",
                "Milestone: {habitName} has been part of your journey for {days} days.",
                "Consistency over time: {days} days of showing up for {habitName}."
            ),
            condition = { context ->
                val activeHabits = context.habits.filter { !it.habit.archived }
                var bestHabit: HabitWithCompletions? = null
                var maxDays = 0L

                for (hwc in activeHabits) {
                    val stats = calculateStatistics(hwc, context.firstDayOfWeek, context.todayMillis)
                    val days = stats.timeSinceCreation
                    if (days >= 30 && isRoundTrackingDays(days) && stats.totalCompletions > 0) {
                        if (days > maxDays) {
                            maxDays = days
                            bestHabit = hwc
                        }
                    }
                }

                bestHabit?.let {
                    WelcomeCategoryMatch(
                        categoryId = CATEGORY_LONG_TIME_TRACKING,
                        habitId = it.habit.id
                    )
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val stats = calculateStatistics(hwc, context.firstDayOfWeek, context.todayMillis)
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "days" to stats.timeSinceCreation.toString()
                    )
                } else emptyMap()
            }
        ),

        // 5. High Completion Rate / Improved Average (Priority: 55)
        WelcomeCategory(
            id = CATEGORY_HIGH_COMPLETION_RATE,
            priority = 55,
            templates = listOf(
                "Remarkable consistency: you've completed {rate}% of {habitName} goals over the last 30 days.",
                "{rate}% consistency on {habitName} this past month. Keep setting the standard.",
                "Your 30-day completion rate for {habitName} is at {rate}%. Superb follow-through.",
                "Solid routine: {rate}% success rate on {habitName} over the last month."
            ),
            condition = { context ->
                val activeHabits = context.habits.filter { !it.habit.archived }
                var bestHabit: HabitWithCompletions? = null
                var bestRate = 0

                for (hwc in activeHabits) {
                    val stats = calculateStatistics(hwc, context.firstDayOfWeek, context.todayMillis)
                    val rate = stats.rateLast30Days
                    if (stats.timeSinceCreation >= 14 && rate >= 80) {
                        if (rate > bestRate) {
                            bestRate = rate
                            bestHabit = hwc
                        }
                    }
                }

                bestHabit?.let {
                    WelcomeCategoryMatch(
                        categoryId = CATEGORY_HIGH_COMPLETION_RATE,
                        habitId = it.habit.id
                    )
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val stats = calculateStatistics(hwc, context.firstDayOfWeek, context.todayMillis)
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "rate" to stats.rateLast30Days.toString()
                    )
                } else emptyMap()
            }
        ),

        // 6. Longest Current Streak (Priority: 40)
        WelcomeCategory(
            id = CATEGORY_CURRENT_STREAK,
            priority = 40,
            templates = listOf(
                "You're on a {streak}-day streak with {habitName}. Keep the momentum going!",
                "Current streak: {streak} days of {habitName}. One day at a time adds up.",
                "{habitName} is on a roll with a {streak}-day streak.",
                "Keep moving forward—{streak} consecutive days completed for {habitName}.",
                "{streak} days in a row for {habitName}. You're building a solid habit."
            ),
            condition = { context ->
                val activeHabits = context.habits.filter { !it.habit.archived }
                var bestHabit: HabitWithCompletions? = null
                var maxStreak = 0

                for (hwc in activeHabits) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    val currentStreak = streakData.currentStreak
                    if (currentStreak >= 2 && currentStreak > maxStreak) {
                        maxStreak = currentStreak
                        bestHabit = hwc
                    }
                }

                bestHabit?.let {
                    WelcomeCategoryMatch(
                        categoryId = CATEGORY_CURRENT_STREAK,
                        habitId = it.habit.id
                    )
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                if (hwc != null) {
                    val streakData = calculateStreakData(hwc.habit, hwc.completions, context.todayMillis, context.firstDayOfWeek)
                    mapOf(
                        "habitName" to hwc.habit.name,
                        "streak" to streakData.currentStreak.toString()
                    )
                } else emptyMap()
            }
        ),

        // 7. Fallback / Motivational Quotes (Priority: 0)
        WelcomeCategory(
            id = CATEGORY_FALLBACK,
            priority = 0,
            templates = listOf(
                "Track your habits, build your future.",
                "The secret of your future is hidden in your daily routine.",
                "Consistency is the key to success.",
                "Motivation is what gets you started. Habit is what keeps you going.",
                "A little progress each day adds up to big results.",
                "Small daily improvements over time lead to stunning results.",
                "Focus on the process, and the results will follow.",
                "Every day is a fresh opportunity to build better routines.",
                "Showing up every day is what turns intention into identity."
            ),
            condition = {
                WelcomeCategoryMatch(categoryId = CATEGORY_FALLBACK)
            },
            extractVariables = { _, _ -> emptyMap() }
        )
    )

    var categories: List<WelcomeCategory> = DEFAULT_WELCOME_CATEGORIES

    /**
     * Evaluates all categories in priority order and returns the best matching candidate.
     */
    fun selectCategoryForDay(context: WelcomeCardContext): WelcomeCategoryMatch {
        val sortedCategories = categories.sortedByDescending { it.priority }
        for (category in sortedCategories) {
            val match = category.condition(context)
            if (match != null) {
                return match
            }
        }
        return WelcomeCategoryMatch(categoryId = CATEGORY_FALLBACK)
    }

    /**
     * Resolves the welcome message for display.
     *
     * Rules:
     * - Stays on the same category and habit throughout the day once chosen.
     * - Live updates variables (streaks, numbers) whenever habits are modified/completed.
     * - If the habit was deleted or the day changed, performs a fresh priority selection.
     */
    fun resolveMessage(
        context: WelcomeCardContext,
        savedDateKey: String?,
        savedCategoryId: String?,
        savedHabitId: String?,
        savedTemplateIndex: Int?,
        ignoreDailyLock: Boolean = BuildConfig.IS_DEVELOPER_MODE
    ): WelcomeCardResolution {
        val dateKey = getFormattedDateKey(context.todayMillis)
        val categoryMap = categories.associateBy { it.id }

        val isSameDay = !ignoreDailyLock && savedDateKey == dateKey && !savedCategoryId.isNullOrEmpty()
        val savedCategory = if (isSameDay) categoryMap[savedCategoryId] else null
        val habitStillExists = savedHabitId.isNullOrEmpty() || context.habits.any { it.habit.id == savedHabitId }

        val categoryToUse: WelcomeCategory
        val habitIdToUse: String?
        val templateIndexToUse: Int
        val isNewSelection: Boolean

        if (isSameDay && savedCategory != null && habitStillExists) {
            // Keep the exact same category and habit for today!
            categoryToUse = savedCategory
            habitIdToUse = savedHabitId
            val templates = categoryToUse.templates
            templateIndexToUse = (savedTemplateIndex ?: 0).coerceIn(0, (templates.size - 1).coerceAtLeast(0))
            isNewSelection = false
        } else {
            // Select new category for today (highest priority candidate matching condition)
            val match = selectCategoryForDay(context)
            categoryToUse = categoryMap[match.categoryId] ?: categoryMap[CATEGORY_FALLBACK] ?: categories.first()
            habitIdToUse = match.habitId
            val templates = categoryToUse.templates
            templateIndexToUse = if (templates.isNotEmpty()) {
                val seed = abs((dateKey + categoryToUse.id).hashCode())
                seed % templates.size
            } else 0
            isNewSelection = !ignoreDailyLock
        }

        // Live extract variables for current state
        val variables = categoryToUse.extractVariables(context, habitIdToUse)
        val template = categoryToUse.templates.getOrElse(templateIndexToUse) {
            categoryToUse.templates.firstOrNull() ?: ""
        }
        val text = formatTemplate(template, variables)

        return WelcomeCardResolution(
            dateKey = dateKey,
            categoryId = categoryToUse.id,
            habitId = habitIdToUse,
            templateIndex = templateIndexToUse,
            text = text,
            variables = variables,
            isNewSelection = isNewSelection
        )
    }
}
