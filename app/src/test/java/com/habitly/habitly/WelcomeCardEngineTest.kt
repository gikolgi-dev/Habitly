/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly

import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.Database.normalizeToStartOfDay
import com.habitly.habitly.data.welcome.WelcomeCardContext
import com.habitly.habitly.data.welcome.WelcomeCardEngine
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_CURRENT_STREAK
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_FALLBACK
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_HABIT_QUIT_MILESTONE
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_HIGH_COMPLETION_RATE
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_LONGEST_STREAK_RECORD
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_LONG_TIME_TRACKING
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_MONTHLY_TREND
import com.habitly.habitly.data.welcome.WelcomeCategory
import com.habitly.habitly.data.welcome.WelcomeCategoryMatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class WelcomeCardEngineTest {

    private val originalCategories = WelcomeCardEngine.categories

    @Before
    fun setUp() {
        WelcomeCardEngine.categories = WelcomeCardEngine.DEFAULT_WELCOME_CATEGORIES
    }

    @After
    fun tearDown() {
        WelcomeCardEngine.categories = originalCategories
    }

    private fun createHabit(
        id: String = "habit-1",
        name: String = "Exercise",
        isInverse: Boolean = false,
        completionsPerInterval: Int = 1,
        startDate: Long = System.currentTimeMillis() - (10L * 24 * 3600 * 1000),
        completionsPerDay: Int = completionsPerInterval,
        intervalUnit: String = "day"
    ): Habit {
        return Habit(
            id = id,
            name = name,
            description = "Daily habit",
            icon = "Book",
            color = 0xFF0000,
            archived = false,
            orderIndex = 0,
            createdAt = startDate.toString(),
            startDate = startDate.toString(),
            isInverse = isInverse,
            emoji = null,
            completionsPerInterval = completionsPerInterval,
            intervalUnit = intervalUnit,
            completionsPerDay = completionsPerDay
        )
    }

    private fun createCompletion(habitId: String, dateMillis: Long, amount: Int = 1): Completion {
        return Completion(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            date = dateMillis,
            timezoneOffsetInMinutes = 0,
            amountOfCompletions = amount
        )
    }

    @Test
    fun testFallbackWhenNoHabits() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val context = WelcomeCardContext(
            habits = emptyList(),
            todayMillis = today
        )

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertEquals(CATEGORY_FALLBACK, resolution.categoryId)
        assertTrue(resolution.text.isNotBlank())
        assertTrue(WelcomeCardEngine.categories.first { it.id == CATEGORY_FALLBACK }.templates.contains(resolution.text))
        assertTrue(resolution.isNewSelection)
    }

    @Test
    fun testCurrentStreakSelection() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 15, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        val habit = createHabit(id = "habit-read", name = "Reading", startDate = today - (10L * 86400000L))
        // Completions for 4 consecutive days (today and past 3 days)
        val completions = listOf(
            createCompletion(habit.id, today),
            createCompletion(habit.id, today - 86400000L),
            createCompletion(habit.id, today - 2 * 86400000L),
            createCompletion(habit.id, today - 3 * 86400000L)
        )
        val hwc = HabitWithCompletions(habit, completions)

        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        // Since streak is 4 (not a round milestone for longest record), it matches current streak (or longest streak if recent)
        // Check that variable substitution for habitName and streak succeeded
        assertTrue(resolution.text.contains("Reading"))
        assertTrue(resolution.text.contains("4"))
        assertFalse(resolution.text.contains("{habitName}"))
        assertFalse(resolution.text.contains("{streak}"))
    }

    @Test
    fun testLongestStreakRecord_priorityOverCurrentStreak() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 15, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // 7 days consecutive streak -> 7 is a round milestone (isRoundStreak(7) == true)
        val habit = createHabit(id = "habit-run", name = "Running", startDate = today - (15L * 86400000L))
        val completions = (0..6).map { i ->
            createCompletion(habit.id, today - (i * 86400000L))
        }
        val hwc = HabitWithCompletions(habit, completions)

        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        val match = WelcomeCardEngine.selectCategoryForDay(context)
        assertEquals(CATEGORY_LONGEST_STREAK_RECORD, match.categoryId)
        assertEquals("habit-run", match.habitId)

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertEquals(CATEGORY_LONGEST_STREAK_RECORD, resolution.categoryId)
        assertTrue(resolution.text.contains("Running"))
        assertTrue(resolution.text.contains("7"))
    }

    @Test
    fun testMonthlyTrend_beginningOfMonth() {
        val cal = Calendar.getInstance()
        // Day 3 of March 2026 -> beginning of month (day <= 7)
        cal.set(2026, Calendar.MARCH, 3, 10, 0, 0)
        val today = cal.timeInMillis

        // Habit started Jan 1 2026
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        }
        val habit = createHabit(id = "meditate", name = "Meditation", startDate = startCal.timeInMillis)

        // Completions in January: 10 days (~32%)
        val janCompletions = (1..10).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }
        // Completions in February: 20 days (~71%) -> Strong improvement!
        val febCompletions = (1..20).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.FEBRUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }

        val hwc = HabitWithCompletions(habit, janCompletions + febCompletions)
        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_MONTHLY_TREND }
        val match = category.condition(context)
        assertNotNull(match)
        assertEquals("meditate", match?.habitId)

        val vars = category.extractVariables(context, match?.habitId)
        assertEquals("Meditation", vars["habitName"])
        assertNotNull(vars["lastMonthRate"])
    }

    @Test
    fun testMonthlyTrend_notEvaluatedLaterInMonth() {
        val cal = Calendar.getInstance()
        // Day 15 of March 2026 -> after first week (day > 7)
        cal.set(2026, Calendar.MARCH, 15, 10, 0, 0)
        val today = cal.timeInMillis

        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        }
        val habit = createHabit(id = "meditate", name = "Meditation", startDate = startCal.timeInMillis)

        val janCompletions = (1..10).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }
        val febCompletions = (1..20).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.FEBRUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }

        val hwc = HabitWithCompletions(habit, janCompletions + febCompletions)
        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_MONTHLY_TREND }
        val match = category.condition(context)
        // Must be null because day of month is 15 (> 7)
        assertEquals(null, match)
    }

    @Test
    fun testHabitQuittingMilestone() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JUNE, 10, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // Inverse habit (quitting smoking) started 7 days ago, with 0 slips (completions list is empty)
        val habit = createHabit(
            id = "quit-smoke",
            name = "Smoking",
            isInverse = true,
            startDate = today - (7L * 86400000L)
        )
        val hwc = HabitWithCompletions(habit, emptyList())

        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_HABIT_QUIT_MILESTONE }
        val match = category.condition(context)
        assertNotNull(match)
        assertEquals("quit-smoke", match?.habitId)

        val vars = category.extractVariables(context, match?.habitId)
        assertEquals("Smoking", vars["habitName"])
        // Clean days should be at least 7
        val streak = vars["streak"]?.toIntOrNull() ?: 0
        assertTrue("Streak was $streak", streak >= 7)

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )
        // Should produce a supportive message without unreplaced tokens
        assertFalse(resolution.text.contains("{habitName}"))
        assertFalse(resolution.text.contains("{streak}"))
        assertTrue(resolution.text.contains("Smoking"))
    }

    @Test
    fun testDailyStability_categoryDoesNotChangeOnCompletion() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JULY, 20, 9, 0, 0) // Morning 9 AM
        val today = normalizeToStartOfDay(cal.timeInMillis)
        val dateKey = WelcomeCardEngine.getFormattedDateKey(today)

        val habit = createHabit(id = "habit-water", name = "Drink Water", startDate = today - (10L * 86400000L))
        // 3 consecutive days completed yesterday and earlier (not today yet)
        val morningCompletions = listOf(
            createCompletion(habit.id, today - 86400000L),
            createCompletion(habit.id, today - 2 * 86400000L),
            createCompletion(habit.id, today - 3 * 86400000L)
        )
        val morningHwc = HabitWithCompletions(habit, morningCompletions)

        val morningContext = WelcomeCardContext(
            habits = listOf(morningHwc),
            todayMillis = today
        )

        // First resolution of the day: picks category for today
        val morningResolution = WelcomeCardEngine.resolveMessage(
            context = morningContext,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertTrue(morningResolution.isNewSelection)
        val chosenCategory = morningResolution.categoryId
        val chosenHabitId = morningResolution.habitId
        val chosenTemplateIndex = morningResolution.templateIndex
        val morningStreak = morningResolution.variables["streak"]?.toIntOrNull() ?: 0

        // User now completes the habit in the afternoon at 2 PM!
        val afternoonCompletions = morningCompletions + createCompletion(habit.id, today)
        val afternoonHwc = HabitWithCompletions(habit, afternoonCompletions)

        val afternoonContext = WelcomeCardContext(
            habits = listOf(afternoonHwc),
            todayMillis = today
        )

        // Re-resolving with saved state for the same day
        val afternoonResolution = WelcomeCardEngine.resolveMessage(
            context = afternoonContext,
            savedDateKey = dateKey,
            savedCategoryId = chosenCategory,
            savedHabitId = chosenHabitId,
            savedTemplateIndex = chosenTemplateIndex
        )

        // The category and habit must NEVER change!
        assertFalse(afternoonResolution.isNewSelection)
        assertEquals(chosenCategory, afternoonResolution.categoryId)
        assertEquals(chosenHabitId, afternoonResolution.habitId)
        assertEquals(chosenTemplateIndex, afternoonResolution.templateIndex)

        // But the streak value MUST update to reflect the completion!
        val afternoonStreak = afternoonResolution.variables["streak"]?.toIntOrNull() ?: 0
        assertEquals(morningStreak + 1, afternoonStreak)
        assertTrue(afternoonResolution.text.contains(afternoonStreak.toString()))
    }

    @Test
    fun testDailyStability_recoversIfHabitDeleted() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 5, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)
        val dateKey = WelcomeCardEngine.getFormattedDateKey(today)

        val remainingHabit = createHabit(id = "habit-read", name = "Reading", startDate = today - (10L * 86400000L))
        val hwc = HabitWithCompletions(remainingHabit, emptyList())

        val context = WelcomeCardContext(
            habits = listOf(hwc),
            todayMillis = today
        )

        // Stored state points to "deleted-habit-id"
        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = dateKey,
            savedCategoryId = CATEGORY_CURRENT_STREAK,
            savedHabitId = "deleted-habit-id",
            savedTemplateIndex = 0
        )

        // Should gracefully recover and make a new selection without throwing
        assertTrue(resolution.isNewSelection)
        assertNotNull(resolution.text)
        assertFalse(resolution.text.contains("deleted-habit-id"))
    }

    @Test
    fun testDeveloperMode_ignoresDailyLockAndDisplaysHighestPriority() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 10, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)
        val dateKey = WelcomeCardEngine.getFormattedDateKey(today)

        // Habit 1: Active 3-day streak (Current Streak, priority 40)
        val habit1 = createHabit(id = "habit-1", name = "Walk", startDate = today - (10L * 86400000L))
        val habit1Completions = listOf(
            createCompletion(habit1.id, today - 86400000L),
            createCompletion(habit1.id, today - 2 * 86400000L),
            createCompletion(habit1.id, today - 3 * 86400000L)
        )
        val hwc1 = HabitWithCompletions(habit1, habit1Completions)

        // Habit 2: Long streak reaching 50-day round milestone today (Longest streak record, priority 100)
        val habit2 = createHabit(id = "habit-2", name = "Gym", startDate = today - (60L * 86400000L))
        val habit2Completions = (0 until 50).map { i ->
            createCompletion(habit2.id, today - (i * 86400000L))
        }
        val hwc2 = HabitWithCompletions(habit2, habit2Completions)

        val context = WelcomeCardContext(
            habits = listOf(hwc1, hwc2),
            todayMillis = today
        )

        // Scenario 1: Production mode (ignoreDailyLock = false).
        // If Morning had locked Habit 1 under CATEGORY_CURRENT_STREAK (priority 40),
        // it must stay on CATEGORY_CURRENT_STREAK despite Habit 2 qualifying for priority 100.
        val prodResolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = dateKey,
            savedCategoryId = CATEGORY_CURRENT_STREAK,
            savedHabitId = habit1.id,
            savedTemplateIndex = 0,
            ignoreDailyLock = false
        )
        assertEquals(CATEGORY_CURRENT_STREAK, prodResolution.categoryId)
        assertEquals(habit1.id, prodResolution.habitId)

        // Scenario 2: Developer mode (ignoreDailyLock = true).
        // Daily lock is ignored, and it MUST pick the category with the highest priority (CATEGORY_LONGEST_STREAK_RECORD, priority 100)
        val devResolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = dateKey,
            savedCategoryId = CATEGORY_CURRENT_STREAK,
            savedHabitId = habit1.id,
            savedTemplateIndex = 0,
            ignoreDailyLock = true
        )
        assertEquals(CATEGORY_LONGEST_STREAK_RECORD, devResolution.categoryId)
        assertEquals(habit2.id, devResolution.habitId)
        assertTrue(devResolution.text.contains("Gym"))
        assertTrue(devResolution.text.contains("50"))
        assertFalse(devResolution.isNewSelection)
    }

    @Test
    fun testMultipleCompletionsHabit() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 1, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // Habit requires 3 completions per day to count
        val habit = createHabit(
            id = "habit-pushups",
            name = "Pushups",
            completionsPerInterval = 3,
            completionsPerDay = 3,
            startDate = today - (5L * 86400000L)
        )

        // 2 days completed before today (yesterday and day before)
        val pastCompletions = listOf(
            createCompletion(habit.id, today - 86400000L, amount = 3),
            createCompletion(habit.id, today - 2 * 86400000L, amount = 3)
        )

        // Today only 1 completion so far (target is 3, not yet completed today)
        val partialTodayCompletions = pastCompletions + createCompletion(habit.id, today, amount = 1)
        val hwcPartial = HabitWithCompletions(habit, partialTodayCompletions)

        val contextPartial = WelcomeCardContext(habits = listOf(hwcPartial), todayMillis = today)
        val resolutionPartial = WelcomeCardEngine.resolveMessage(
            context = contextPartial,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        val partialStreak = resolutionPartial.variables["streak"]?.toIntOrNull() ?: 0
        assertEquals(2, partialStreak) // Streak is 2 days (from past 2 days)

        // User finishes all 3 completions for today!
        val fullTodayCompletions = pastCompletions + createCompletion(habit.id, today, amount = 3)
        val hwcFull = HabitWithCompletions(habit, fullTodayCompletions)

        val contextFull = WelcomeCardContext(habits = listOf(hwcFull), todayMillis = today)
        val resolutionFull = WelcomeCardEngine.resolveMessage(
            context = contextFull,
            savedDateKey = resolutionPartial.dateKey,
            savedCategoryId = resolutionPartial.categoryId,
            savedHabitId = resolutionPartial.habitId,
            savedTemplateIndex = resolutionPartial.templateIndex
        )

        val fullStreak = resolutionFull.variables["streak"]?.toIntOrNull() ?: 0
        assertEquals(3, fullStreak) // Today completed, streak is now 3 days!
    }

    @Test
    fun testLongTimeTrackingMilestone() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // 100 days milestone!
        val habit = createHabit(
            id = "habit-floss",
            name = "Flossing",
            startDate = today - (99L * 86400000L) // creation ~100 days
        )
        // With some completions logged
        val completions = listOf(createCompletion(habit.id, today - 86400000L))
        val hwc = HabitWithCompletions(habit, completions)

        val context = WelcomeCardContext(habits = listOf(hwc), todayMillis = today)
        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_LONG_TIME_TRACKING }
        val match = category.condition(context)
        assertNotNull(match)

        val vars = category.extractVariables(context, match?.habitId)
        assertEquals("Flossing", vars["habitName"])
        assertNotNull(vars["days"])
    }

    @Test
    fun testCustomCategoryExtension() {
        // Test that the engine easily supports adding custom categories
        val customCategory = WelcomeCategory(
            id = "custom_super_streak",
            priority = 200, // Higher than any default category
            templates = listOf("Super milestone: {habitName} has reached {streak} days!"),
            condition = { context ->
                context.habits.firstOrNull()?.let {
                    WelcomeCategoryMatch("custom_super_streak", it.habit.id)
                }
            },
            extractVariables = { context, habitId ->
                val hwc = context.habits.find { it.habit.id == habitId }
                mapOf("habitName" to (hwc?.habit?.name ?: ""), "streak" to "999")
            }
        )

        WelcomeCardEngine.categories = listOf(customCategory) + WelcomeCardEngine.DEFAULT_WELCOME_CATEGORIES

        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(name = "Coding")
        val context = WelcomeCardContext(habits = listOf(HabitWithCompletions(habit, emptyList())), todayMillis = today)

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertEquals("custom_super_streak", resolution.categoryId)
        assertEquals("Super milestone: Coding has reached 999 days!", resolution.text)
    }
}
