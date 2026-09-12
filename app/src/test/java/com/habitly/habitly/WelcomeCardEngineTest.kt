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
<<<<<<< Updated upstream
=======
import com.habitly.habitly.data.welcome.WelcomeCardEngine.CATEGORY_30_DAY_IMPROVEMENT
>>>>>>> Stashed changes
import com.habitly.habitly.data.welcome.WelcomeCategory
import com.habitly.habitly.data.welcome.WelcomeCategoryMatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
<<<<<<< Updated upstream
=======
import org.junit.Assert.assertNull
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
        intervalUnit: String = "day"
=======
        intervalUnit: String = "day",
        ignoreInWelcomeCard: Boolean = false
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
            completionsPerDay = completionsPerDay
=======
            completionsPerDay = completionsPerDay,
            ignoreInWelcomeCard = ignoreInWelcomeCard
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
=======

    @Test
    fun testBuildHabit_evaluationIgnoresLackOfCompletionsToday_andLiveUpdatesWhenCompleted() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 10, 8, 0, 0) // 8 AM morning
        val today = normalizeToStartOfDay(cal.timeInMillis)

        val habit = createHabit(id = "habit-yoga", name = "Yoga", startDate = today - (20L * 86400000L))
        // An earlier 6-day streak in the past so longestStreak is 6, not 4
        val pastLongerStreak = (7..12).map { i -> createCompletion(habit.id, today - (i * 86400000L)) }
        // Current streak of 4 consecutive days up to yesterday; today has 0 completions
        val currentStreakUpToYesterday = listOf(
            createCompletion(habit.id, today - 86400000L),
            createCompletion(habit.id, today - 2 * 86400000L),
            createCompletion(habit.id, today - 3 * 86400000L),
            createCompletion(habit.id, today - 4 * 86400000L)
        )
        val hwcMorning = HabitWithCompletions(habit, pastLongerStreak + currentStreakUpToYesterday)
        val morningContext = WelcomeCardContext(habits = listOf(hwcMorning), todayMillis = today)

        // Morning evaluation: should NOT see "lack of completions today" and fail to current_streak
        val morningResolution = WelcomeCardEngine.resolveMessage(
            context = morningContext,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertEquals(CATEGORY_CURRENT_STREAK, morningResolution.categoryId)
        assertEquals(habit.id, morningResolution.habitId)
        assertEquals("4", morningResolution.variables["streak"])

        // User now completes the habit today
        val afternoonCompletions = pastLongerStreak + currentStreakUpToYesterday + createCompletion(habit.id, today)
        val hwcAfternoon = HabitWithCompletions(habit, afternoonCompletions)
        val afternoonContext = WelcomeCardContext(habits = listOf(hwcAfternoon), todayMillis = today)

        val afternoonResolution = WelcomeCardEngine.resolveMessage(
            context = afternoonContext,
            savedDateKey = morningResolution.dateKey,
            savedCategoryId = morningResolution.categoryId,
            savedHabitId = morningResolution.habitId,
            savedTemplateIndex = morningResolution.templateIndex
        )

        assertEquals(CATEGORY_CURRENT_STREAK, afternoonResolution.categoryId)
        assertEquals("5", afternoonResolution.variables["streak"])
        assertTrue(afternoonResolution.text.contains("5"))
    }

    @Test
    fun testHighCompletionRate_notPenalizedByTodayZeroCompletions() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 20, 9, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // 30 days completed up to yesterday
        val habit = createHabit(id = "habit-water", name = "Hydration", startDate = today - (35L * 86400000L))
        val completions = (1..30).map { i ->
            createCompletion(habit.id, today - (i * 86400000L))
        }
        val hwc = HabitWithCompletions(habit, completions)
        val context = WelcomeCardContext(habits = listOf(hwc), todayMillis = today)

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_HIGH_COMPLETION_RATE }
        val match = category.condition(context)
        assertNotNull("Should match high completion rate despite 0 completions today", match)

        val vars = category.extractVariables(context, match?.habitId)
        assertEquals("100", vars["rate"])
    }

    @Test
    fun testQuitHabit_cleanStreakThroughYesterday() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 15, 10, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // Quit smoking 5 days ago (5 full days clean up to yesterday)
        val habit = createHabit(
            id = "quit-smoke",
            name = "Smoking",
            isInverse = true,
            startDate = today - (5L * 86400000L)
        )
        val hwc = HabitWithCompletions(habit, emptyList())
        val context = WelcomeCardContext(habits = listOf(hwc), todayMillis = today)

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_HABIT_QUIT_MILESTONE }
        val match = category.condition(context)
        assertNotNull(match)

        val vars = category.extractVariables(context, match?.habitId)
        assertEquals("5", vars["streak"])
    }

    @Test
    fun testQuitHabit_slipTodayBreaksMilestoneAndStreakDropsToZero() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 15, 14, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // Quit smoking started 7 days ago, but user slipped today
        val habit = createHabit(
            id = "quit-smoke",
            name = "Smoking",
            isInverse = true,
            startDate = today - (7L * 86400000L)
        )
        val slipToday = createCompletion(habit.id, today, amount = 1)
        val hwc = HabitWithCompletions(habit, listOf(slipToday))
        val context = WelcomeCardContext(habits = listOf(hwc), todayMillis = today)

        // Condition should NOT match because of the slip today
        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_HABIT_QUIT_MILESTONE }
        val match = category.condition(context)
        assertEquals(null, match)

        // If locked from earlier, variables should report 0
        val vars = category.extractVariables(context, habit.id)
        assertEquals("0", vars["streak"])
    }

    @Test
    fun testMilestoneEquation_dynamicScaling() {
        // Equation tests:
        // Under 30: multiples of 7
        assertTrue(WelcomeCardEngine.isRoundStreak(7))
        assertTrue(WelcomeCardEngine.isRoundStreak(14))
        assertTrue(WelcomeCardEngine.isRoundStreak(21))
        assertTrue(WelcomeCardEngine.isRoundStreak(28))
        assertFalse(WelcomeCardEngine.isRoundStreak(1))
        assertFalse(WelcomeCardEngine.isRoundStreak(5))
        assertFalse(WelcomeCardEngine.isRoundStreak(10)) // 10 is < 30, not a multiple of 7
        assertFalse(WelcomeCardEngine.isRoundStreak(25))

        // 30 to 99: multiples of 10
        assertTrue(WelcomeCardEngine.isRoundStreak(30))
        assertTrue(WelcomeCardEngine.isRoundStreak(40))
        assertTrue(WelcomeCardEngine.isRoundStreak(50))
        assertTrue(WelcomeCardEngine.isRoundStreak(90))
        assertFalse(WelcomeCardEngine.isRoundStreak(35))
        assertFalse(WelcomeCardEngine.isRoundStreak(42))
        assertFalse(WelcomeCardEngine.isRoundStreak(99))

        // 100 to 499: multiples of 50
        assertTrue(WelcomeCardEngine.isRoundStreak(100))
        assertTrue(WelcomeCardEngine.isRoundStreak(150))
        assertTrue(WelcomeCardEngine.isRoundStreak(200))
        assertTrue(WelcomeCardEngine.isRoundStreak(450))
        assertFalse(WelcomeCardEngine.isRoundStreak(110))
        assertFalse(WelcomeCardEngine.isRoundStreak(125))

        // 500 and beyond: multiples of 100
        assertTrue(WelcomeCardEngine.isRoundStreak(500))
        assertTrue(WelcomeCardEngine.isRoundStreak(600))
        assertTrue(WelcomeCardEngine.isRoundStreak(1000))
        assertTrue(WelcomeCardEngine.isRoundStreak(1200))
        assertFalse(WelcomeCardEngine.isRoundStreak(550))
        assertFalse(WelcomeCardEngine.isRoundStreak(1050))

        // Yearly milestones: multiples of 365
        assertTrue(WelcomeCardEngine.isRoundStreak(365))
        assertTrue(WelcomeCardEngine.isRoundStreak(730))
        assertTrue(WelcomeCardEngine.isRoundStreak(1095))

        // Applies identically to tracking days
        assertTrue(WelcomeCardEngine.isRoundTrackingDays(365L))
        assertTrue(WelcomeCardEngine.isRoundTrackingDays(730L))
        assertTrue(WelcomeCardEngine.isRoundTrackingDays(50L))
        assertTrue(WelcomeCardEngine.isRoundTrackingDays(100L))
        assertFalse(WelcomeCardEngine.isRoundTrackingDays(1050L))
    }

    @Test
    fun testYearMilestonePhrasing_forStreaksAndTracking() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // 1. Longest streak reaching 365 days (1 year)
        val habit1 = createHabit(id = "streak-1yr", name = "Guitar", startDate = today - (400L * 86400000L))
        val habit1Completions = (1..365).map { i ->
            val c = Calendar.getInstance().apply {
                timeInMillis = today
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            createCompletion(habit1.id, c.timeInMillis)
        }
        val hwc1 = HabitWithCompletions(habit1, habit1Completions)
        val context1 = WelcomeCardContext(habits = listOf(hwc1), todayMillis = today)

        val resStreak1Yr = WelcomeCardEngine.resolveMessage(
            context = context1,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )
        assertEquals(CATEGORY_LONGEST_STREAK_RECORD, resStreak1Yr.categoryId)
        // Must say "1-year" or "1 year" instead of "365 days" or "365-day"
        assertFalse(resStreak1Yr.text.contains("365 days"))
        assertFalse(resStreak1Yr.text.contains("365-day"))
        assertTrue("Text was: ${resStreak1Yr.text}", resStreak1Yr.text.contains("1-year") || resStreak1Yr.text.contains("1 year"))

        // 2. Long-time tracking reaching 365 days (1 year)
        val habitTracking = createHabit(id = "track-1yr", name = "Reading", startDate = today - (364L * 86400000L))
        // 1 completion so total completions > 0, but streak is only 1 so longest streak record doesn't trigger
        val trackingCompletions = listOf(createCompletion(habitTracking.id, today - 86400000L))
        val hwcTracking = HabitWithCompletions(habitTracking, trackingCompletions)
        val contextTracking = WelcomeCardContext(habits = listOf(hwcTracking), todayMillis = today)

        val resTracking1Yr = WelcomeCardEngine.resolveMessage(
            context = contextTracking,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )
        assertEquals(CATEGORY_LONG_TIME_TRACKING, resTracking1Yr.categoryId)
        assertFalse(resTracking1Yr.text.contains("365 days"))
        assertTrue("Text was: ${resTracking1Yr.text}", resTracking1Yr.text.contains("1 year"))

        // 3. Long-time tracking reaching 730 days (2 years)
        val habitTracking2Yr = createHabit(id = "track-2yr", name = "Running", startDate = today - (729L * 86400000L))
        val hwcTracking2Yr = HabitWithCompletions(habitTracking2Yr, trackingCompletions)
        val contextTracking2Yr = WelcomeCardContext(habits = listOf(hwcTracking2Yr), todayMillis = today)

        val resTracking2Yr = WelcomeCardEngine.resolveMessage(
            context = contextTracking2Yr,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )
        assertEquals(CATEGORY_LONG_TIME_TRACKING, resTracking2Yr.categoryId)
        assertFalse(resTracking2Yr.text.contains("730 days"))
        assertTrue("Text was: ${resTracking2Yr.text}", resTracking2Yr.text.contains("2 years"))
    }

    @Test
    fun test30DayImprovementVsPreviousMonth_showsBothValues() {
        val cal = Calendar.getInstance()
        // Middle of March 2026 (day > 7, so monthly_trend doesn't trigger)
        cal.set(2026, Calendar.MARCH, 15, 12, 0, 0)
        val today = normalizeToStartOfDay(cal.timeInMillis)

        // Habit started Jan 1, 2026 (tracked > 30 days)
        val startCal = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 1, 0, 0, 0) }
        val habit = createHabit(id = "code-habit", name = "Coding", startDate = startCal.timeInMillis)

        // Prior 25-day streak in January so longestStreak is 25 (preventing 14-day streak from triggering longest_streak_record)
        val janCompletions = (1..25).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }
        // February 2026 (28 days): 14 completions = 50% completion rate
        val febCompletions = (1..14).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.FEBRUARY, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }
        // In the rolling last 30 days (Feb 13 to Mar 14):
        // 26 completions out of 30 days (~87% completion rate)
        val marchCompletions = (1..14).map { day ->
            val c = Calendar.getInstance().apply { set(2026, Calendar.MARCH, day, 12, 0, 0) }
            createCompletion(habit.id, c.timeInMillis)
        }
        val hwc = HabitWithCompletions(habit, janCompletions + febCompletions + marchCompletions)
        val context = WelcomeCardContext(habits = listOf(hwc), todayMillis = today)

        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_30_DAY_IMPROVEMENT }
        val match = category.condition(context)
        assertNotNull("Should match 30-day improvement", match)
        assertEquals("code-habit", match?.habitId)

        val vars = category.extractVariables(context, match?.habitId)
        val currentRate = vars["currentRate"]?.toIntOrNull() ?: 0
        val previousRate = vars["previousRate"]?.toIntOrNull() ?: 0

        assertTrue("Current 30-day rate ($currentRate) should be greater than previous month rate ($previousRate)", currentRate > previousRate)
        assertEquals(50, previousRate) // Feb was 50%

        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null
        )

        assertEquals(CATEGORY_30_DAY_IMPROVEMENT, resolution.categoryId)
        // Must show BOTH values to compare in the message!
        assertTrue("Text must contain current rate ($currentRate%): ${resolution.text}", resolution.text.contains("$currentRate%"))
        assertTrue("Text must contain previous rate ($previousRate%): ${resolution.text}", resolution.text.contains("$previousRate%"))
    }

    @Test
    fun testHighCompletionRateTemplates_doNotMentionMonthFor30Days() {
        val category = WelcomeCardEngine.categories.first { it.id == CATEGORY_HIGH_COMPLETION_RATE }
        for (template in category.templates) {
            // Must not claim "this past month" or "over the last month" when reporting 30-day rate
            assertFalse("Template contains inaccurate 'past month': $template", template.contains("past month", ignoreCase = true))
            assertFalse("Template contains inaccurate 'over the last month': $template", template.contains("over the last month", ignoreCase = true))
        }
    }

    @Test
    fun testHabitIgnoredInWelcomeCard_isExcludedFromStreakRecordAndCurrentStreak() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val cal = Calendar.getInstance().apply { timeInMillis = today }

        // Create a habit with an active streak but marked ignoreInWelcomeCard = true
        val ignoredHabit = createHabit(
            id = "ignored-habit",
            name = "Ignored Meditation",
            startDate = today - (10L * 24 * 3600 * 1000),
            ignoreInWelcomeCard = true
        )
        val completionsIgnored = (1..7).map { dayOffset ->
            createCompletion(ignoredHabit.id, today - (dayOffset * 24 * 3600 * 1000L))
        }

        val context1 = WelcomeCardContext(
            habits = listOf(HabitWithCompletions(ignoredHabit, completionsIgnored)),
            todayMillis = today
        )

        // Since the only habit has ignoreInWelcomeCard = true, it must not be picked
        val match1 = WelcomeCardEngine.selectCategoryForDay(context1)
        assertEquals(CATEGORY_FALLBACK, match1.categoryId)

        // Now add a second habit with ignoreInWelcomeCard = false
        val activeHabit = createHabit(
            id = "active-habit",
            name = "Reading",
            startDate = today - (5L * 24 * 3600 * 1000),
            ignoreInWelcomeCard = false
        )
        val completionsActive = (1..3).map { dayOffset ->
            createCompletion(activeHabit.id, today - (dayOffset * 24 * 3600 * 1000L))
        }

        val context2 = WelcomeCardContext(
            habits = listOf(
                HabitWithCompletions(ignoredHabit, completionsIgnored),
                HabitWithCompletions(activeHabit, completionsActive)
            ),
            todayMillis = today
        )

        val match2 = WelcomeCardEngine.selectCategoryForDay(context2)
        assertEquals(activeHabit.id, match2.habitId)
    }

    @Test
    fun testHabitIgnoredInWelcomeCard_isExcludedFromQuitMilestoneAndLongTimeTracking() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())

        // Quit habit with 30 days clean, but ignored
        val ignoredQuit = createHabit(
            id = "ignored-quit",
            name = "Smoking",
            isInverse = true,
            startDate = today - (30L * 24 * 3600 * 1000),
            ignoreInWelcomeCard = true
        )
        val context = WelcomeCardContext(
            habits = listOf(HabitWithCompletions(ignoredQuit, emptyList())),
            todayMillis = today
        )

        val match = WelcomeCardEngine.selectCategoryForDay(context)
        assertEquals(CATEGORY_FALLBACK, match.categoryId)
    }

    @Test
    fun testSavedHabitId_reselectedIfHabitIgnoredDuringDay() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val dateKey = WelcomeCardEngine.getFormattedDateKey(today)

        val habitNowIgnored = createHabit(
            id = "habit-1",
            name = "Exercise",
            startDate = today - (10L * 24 * 3600 * 1000),
            ignoreInWelcomeCard = true
        )

        val context = WelcomeCardContext(
            habits = listOf(HabitWithCompletions(habitNowIgnored, emptyList())),
            todayMillis = today
        )

        // If savedHabitId was previously saved for today, but the habit is now ignored,
        // it must NOT keep the ignored habit!
        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = dateKey,
            savedCategoryId = CATEGORY_CURRENT_STREAK,
            savedHabitId = "habit-1",
            savedTemplateIndex = 0,
            ignoreDailyLock = false
        )

        assertEquals(CATEGORY_FALLBACK, resolution.categoryId)
        assertFalse(resolution.habitId == "habit-1")
    }

    @Test
    fun testResolveMessage_withIgnoreWelcomeEngine_returnsSimpleDescriptions() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())

        // Create a habit with an impressive 100-day streak that would normally trigger streak record
        val habit = createHabit(
            id = "habit-streak-100",
            name = "Coding",
            startDate = today - (120L * 24 * 3600 * 1000)
        )
        val completions = (1..100).map { dayOffset ->
            createCompletion(habit.id, today - (dayOffset * 24 * 3600 * 1000L))
        }

        val context = WelcomeCardContext(
            habits = listOf(HabitWithCompletions(habit, completions)),
            todayMillis = today
        )

        // When ignoreWelcomeEngine is true, it must ignore all engine categories and return a simple description
        val resolution = WelcomeCardEngine.resolveMessage(
            context = context,
            savedDateKey = null,
            savedCategoryId = null,
            savedHabitId = null,
            savedTemplateIndex = null,
            ignoreWelcomeEngine = true
        )

        assertEquals(CATEGORY_FALLBACK, resolution.categoryId)
        assertNull(resolution.habitId)
        assertFalse(resolution.isNewSelection)
        assertTrue(
            "Text '${resolution.text}' must be in SIMPLE_WELCOME_DESCRIPTIONS",
            WelcomeCardEngine.SIMPLE_WELCOME_DESCRIPTIONS.contains(resolution.text)
        )
    }
>>>>>>> Stashed changes
}
