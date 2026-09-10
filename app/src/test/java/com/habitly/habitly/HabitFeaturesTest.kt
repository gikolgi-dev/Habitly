/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly

import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.Database.getEffectiveCompletionsForDay
import com.habitly.habitly.data.Database.isDayCompleted
import com.habitly.habitly.data.Database.normalizeToStartOfDay
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.data.Database.normalizeToEndOfDay
import com.habitly.habitly.data.calculateStatistics
import com.habitly.habitly.data.getCompletedDays
import com.habitly.habitly.data.getTotalEffectiveCompletions
import org.junit.Assert.assertEquals
import com.habitly.habitly.ui.screen.settings.ExportData
import com.habitly.habitly.ui.screen.settings.HabitKitExport
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class HabitFeaturesTest {

    private fun createHabit(
        isInverse: Boolean = false,
        completionsPerInterval: Int = 1,
        startDate: Long = System.currentTimeMillis() - (5L * 24 * 3600 * 1000), // 5 days ago
        completionsPerDay: Int = completionsPerInterval,
        intervalUnit: String = "day"
    ): Habit {
        return Habit(
            id = "test-habit-1",
            name = "Test Habit",
            description = "Testing habit",
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

    @Test
    fun testBuildHabit_singleCompletion() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = false, completionsPerInterval = 1)
        val completions = emptyList<Completion>()

        // Default state: 0 completions, not completed
        assertEquals(0, getEffectiveCompletionsForDay(habit, completions, today))
        assertFalse(isDayCompleted(habit, completions, today))

        // When completed: 1 completion
        val completedList = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 1
            )
        )
        assertEquals(1, getEffectiveCompletionsForDay(habit, completedList, today))
        assertTrue(isDayCompleted(habit, completedList, today))
    }

    @Test
    fun testBuildHabit_multipleCompletions() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = false, completionsPerInterval = 3)
        val completions = emptyList<Completion>()

        // Default: 0/3
        assertEquals(0, getEffectiveCompletionsForDay(habit, completions, today))
        assertFalse(isDayCompleted(habit, completions, today))

        // 2 completions: partial progress
        val partialList = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 2
            )
        )
        assertEquals(2, getEffectiveCompletionsForDay(habit, partialList, today))
        assertFalse(isDayCompleted(habit, partialList, today))

        // 3 completions: fully completed
        val fullList = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 3
            )
        )
        assertEquals(3, getEffectiveCompletionsForDay(habit, fullList, today))
        assertTrue(isDayCompleted(habit, fullList, today))
    }

    @Test
    fun testQuitHabit_completedByDefault_andSpaceSaving() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = true, completionsPerInterval = 1)

        // 0 entries in DB -> COMPLETED by default! (Space saving)
        val emptyCompletions = emptyList<Completion>()
        assertEquals(1, getEffectiveCompletionsForDay(habit, emptyCompletions, today))
        assertTrue(isDayCompleted(habit, emptyCompletions, today))

        // 1 entry in DB (a slip/relapse) -> UNCOMPLETED
        val slipList = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 1
            )
        )
        assertEquals(0, getEffectiveCompletionsForDay(habit, slipList, today))
        assertFalse(isDayCompleted(habit, slipList, today))
    }

    @Test
    fun testQuitHabit_multipleCompletions_interwoven() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = true, completionsPerInterval = 3)

        // Default: 3/3 completed with 0 rows in DB
        val emptyCompletions = emptyList<Completion>()
        assertEquals(3, getEffectiveCompletionsForDay(habit, emptyCompletions, today))
        assertTrue(isDayCompleted(habit, emptyCompletions, today))

        // 1 slip: 2/3 remaining
        val oneSlip = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 1
            )
        )
        assertEquals(2, getEffectiveCompletionsForDay(habit, oneSlip, today))
        assertFalse(isDayCompleted(habit, oneSlip, today))

        // 3 slips: 0/3 remaining (completely relapsed)
        val threeSlips = listOf(
            Completion(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 3
            )
        )
        assertEquals(0, getEffectiveCompletionsForDay(habit, threeSlips, today))
        assertFalse(isDayCompleted(habit, threeSlips, today))
    }

    @Test
    fun testBackdatedStartDate_quitHabitAutoCompletes() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val tenDaysAgo = today - (10L * 24 * 3600 * 1000)
        val habit = createHabit(isInverse = true, completionsPerInterval = 1, startDate = tenDaysAgo)
        val emptyCompletions = emptyList<Completion>()

        // Date before start date is NOT completed (habit had not started yet)
        val twelveDaysAgo = today - (12L * 24 * 3600 * 1000)
        assertEquals(0, getEffectiveCompletionsForDay(habit, emptyCompletions, twelveDaysAgo))
        assertFalse(isDayCompleted(habit, emptyCompletions, twelveDaysAgo))

        // All 10 days from start date to today are completed by default
        for (i in 0..10) {
            val date = tenDaysAgo + (i * 24 * 3600 * 1000L)
            assertEquals(1, getEffectiveCompletionsForDay(habit, emptyCompletions, date))
            assertTrue(isDayCompleted(habit, emptyCompletions, date))
        }

        // Total completions across these 11 days (tenDaysAgo up to today) is 11
        val total = getTotalEffectiveCompletions(habit, emptyCompletions, today)
        assertEquals(11, total)
    }

    @Test
    fun testConversionBuildToQuit_preservesVisualAppearanceAndInvertsStorage() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000) // Day 0, Day 1, Day 2 (today)
        val buildHabit = createHabit(isInverse = false, completionsPerInterval = 1, startDate = threeDaysAgo)

        // Build habit has Day 0 completed, Day 1 empty, Day 2 (today) completed
        val day0 = threeDaysAgo
        val day1 = threeDaysAgo + (24 * 3600 * 1000L)
        val day2 = today

        val buildCompletions = listOf(
            Completion("c0", buildHabit.id, day0 + 5000L, 0, 1),
            Completion("c2", buildHabit.id, day2 + 5000L, 0, 1)
        )

        // Visual state in Build habit: Day 0 completed, Day 1 not completed, Day 2 completed
        assertEquals(1, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day0))
        assertEquals(0, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day1))
        assertEquals(1, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day2))

        // Convert to Quit habit:
        // Empty dates (Day 1) should be added as slips in DB!
        // Completed dates (Day 0 and Day 2) should NOT be in DB!
        val quitHabit = buildHabit.copy(isInverse = true)
        val quitCompletions = listOf(
            Completion("slip-1", quitHabit.id, day1 + 5000L, 0, 1)
        )

        // Visual state in Quit habit MUST be identical!
        assertEquals(1, getEffectiveCompletionsForDay(quitHabit, quitCompletions, day0))
        assertEquals(0, getEffectiveCompletionsForDay(quitHabit, quitCompletions, day1))
        assertEquals(1, getEffectiveCompletionsForDay(quitHabit, quitCompletions, day2))
    }

    @Test
    fun testStreakAndStatistics_quitHabit() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val fiveDaysAgo = today - (4L * 24 * 3600 * 1000) // 5 consecutive days: 4, 3, 2, 1, 0 (today)
        val quitHabit = createHabit(isInverse = true, completionsPerInterval = 1, startDate = fiveDaysAgo)

        // User relapsed 2 days ago (day index 2)
        val twoDaysAgo = today - (2L * 24 * 3600 * 1000)
        val completions = listOf(
            Completion("slip-1", quitHabit.id, twoDaysAgo + 5000L, 0, 1)
        )

        val completedDays = getCompletedDays(quitHabit, completions, today)
        // Completed days: 4 days ago, 3 days ago, 1 day ago, today (4 days total)
        assertEquals(4, completedDays.size)
        assertTrue(completedDays.contains(today))
        assertFalse(completedDays.contains(twoDaysAgo))

        // Current streak: today and 1 day ago = 2
        val hwc = HabitWithCompletions(quitHabit, completions)
        val stats = calculateStatistics(hwc)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
        assertEquals(4, stats.totalCompletions)
        assertEquals(4, stats.totalCompletions)
    }

    @Test
    fun testStreak_multiCompleteHabit_countsDays() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000)
        val habit = createHabit(isInverse = false, completionsPerInterval = 3, startDate = threeDaysAgo)

        // Fully complete 2 days: yesterday and today (each with 3 completions)
        val completions = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 3),
            Completion("c-2", habit.id, today + 1000L, 0, 3)
        )

        val hwc = HabitWithCompletions(habit, completions)
        val stats = calculateStatistics(hwc)
        // 2 consecutive completed days -> streak is 2 days
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun testDailyStreak_todayNotSuccessfulDoesNotBreakStreak() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000)
        // Multiple completions: 5 per day, streak target: 3
        val habit = createHabit(isInverse = false, completionsPerInterval = 3, completionsPerDay = 5, startDate = threeDaysAgo)

        // Completed yesterday with 4 (>= 3), but today has 0
        val completionsYesterdayOnly = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4)
        )
        val statsYesterday = calculateStatistics(HabitWithCompletions(habit, completionsYesterdayOnly))
        // Streak does NOT fail today -> streak is 1
        assertEquals(1, statsYesterday.currentStreak)

        // If today also reaches 3 (>= 3):
        val completionsWithToday = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4),
            Completion("c-2", habit.id, today + 1000L, 0, 3)
        )
        val statsToday = calculateStatistics(HabitWithCompletions(habit, completionsWithToday))
        // Today adds one -> streak is 2
        assertEquals(2, statsToday.currentStreak)
    }


    @Test
    fun testIndependentCompletionsPerDay_andWeeklyStreakTarget() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val cal = Calendar.getInstance().apply { timeInMillis = today }
        val firstDay = cal.firstDayOfWeek
        while (cal.get(Calendar.DAY_OF_WEEK) != firstDay) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val startOfWeek = normalizeToStartOfDay(cal.timeInMillis)

        // Habit: 2 completions per day, 10 completions per week
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 10,
            intervalUnit = "week",
            completionsPerDay = 2,
            startDate = startOfWeek
        )

        // Verify daily target is 2 and weekly target is 10
        assertEquals(2, habit.getDailyTarget())
        assertEquals(10, habit.completionsPerInterval)
        assertEquals("week", habit.intervalUnit)

        // 4 days with 2 completions each (8 total) -> weekly target not met yet (8 < 10)
        val fourDaysCompletions = (0..3).map { dayOffset ->
            Completion(
                id = "c-$dayOffset",
                habitId = habit.id,
                date = startOfWeek + (dayOffset * 24 * 3600 * 1000L) + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 2
            )
        }
        assertEquals(8, getTotalEffectiveCompletions(habit, fourDaysCompletions, today))

        // 5 days with 2 completions each (10 total) -> weekly target met (10 >= 10)!
        val fiveDaysCompletions = (0..4).map { dayOffset ->
            Completion(
                id = "c-$dayOffset",
                habitId = habit.id,
                date = startOfWeek + (dayOffset * 24 * 3600 * 1000L) + 1000L,
                timezoneOffsetInMinutes = 0,
                amountOfCompletions = 2
            )
        }
        val hwc5 = HabitWithCompletions(habit, fiveDaysCompletions)
        val stats5 = calculateStatistics(hwc5)
        assertEquals(10, stats5.currentStreak)
        assertEquals(10, stats5.longestStreak)
        assertEquals(10, stats5.totalCompletions)
    }
    @Test
    fun testExportData_includesVersionInfo() {
        val exportData = ExportData(habits = emptyList())
        assertEquals(2, exportData.version)
        assertEquals(2, exportData.formatVersion)

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(ExportData.serializer(), exportData)
        assertTrue(encoded.contains("\"version\":2") || encoded.contains("\"version\": 2"))
        assertTrue(encoded.contains("\"formatVersion\":2") || encoded.contains("\"formatVersion\": 2"))
    }

    @Test
    fun testHabitKitFormatVersion2_importMapping() {
        val sampleJson = """{"formatVersion":2,"habits":[{"id":"583371e5-1dbb-4490-bfc7-fa2ca3218499","name":"Hehs","description":null,"icon":"activity","color":"red","archived":false,"orderIndex":0,"createdAt":"2026-08-27T19:42:49.376491Z","isInverse":true,"inverseStartDate":"2026-08-18","inverseSpanBounds":[],"emoji":null},{"id":"4bd94bc5-3512-41b0-a7f1-51d0179f6628","name":"Multi","description":null,"icon":"activity","color":"red","archived":false,"orderIndex":0,"createdAt":"2026-08-27T19:43:31.694871Z","isInverse":false,"inverseStartDate":null,"inverseSpanBounds":[],"emoji":null}],"completions":[{"id":"9d4dce62-fb2a-4a4b-a08d-21840ed58d1b","date":"2026-08-26T22:00:00.000Z","habitId":"583371e5-1dbb-4490-bfc7-fa2ca3218499","timezoneOffsetInMinutes":120,"amountOfCompletions":0,"note":null},{"id":"98efa988-469d-4c26-916f-7f62d8c18897","date":"2026-08-19T22:00:01.000Z","habitId":"583371e5-1dbb-4490-bfc7-fa2ca3218499","timezoneOffsetInMinutes":120,"amountOfCompletions":1,"note":null},{"id":"01f9dc61-4584-4775-ad55-b90e6f2c3e1d","date":"2026-08-24T22:00:01.000Z","habitId":"583371e5-1dbb-4490-bfc7-fa2ca3218499","timezoneOffsetInMinutes":120,"amountOfCompletions":1,"note":null},{"id":"56144dab-0673-4924-bcdb-00fa5e0280d8","date":"2026-08-26T22:00:00.000Z","habitId":"4bd94bc5-3512-41b0-a7f1-51d0179f6628","timezoneOffsetInMinutes":120,"amountOfCompletions":2,"note":null}],"intervals":[{"id":"74bd989f-136c-46be-84c1-41f25c36cf2d","habitId":"583371e5-1dbb-4490-bfc7-fa2ca3218499","startDate":"2026-08-19T22:00:00.000Z","endDate":null,"type":"none","requiredNumberOfCompletions":null,"requiredNumberOfCompletionsPerDay":1,"unitType":"incremental","streakType":"day","allowExceedingGoal":false},{"id":"b74b911b-481a-4afa-8427-21c071cf7290","habitId":"4bd94bc5-3512-41b0-a7f1-51d0179f6628","startDate":"2026-08-26T22:00:00.000Z","endDate":null,"type":"none","requiredNumberOfCompletions":null,"requiredNumberOfCompletionsPerDay":3,"unitType":"incremental","streakType":"day","allowExceedingGoal":false}],"reminders":[],"categories":[]}"""

        val jsonParser = Json { ignoreUnknownKeys = true }
        val habitKitData = jsonParser.decodeFromString<HabitKitExport>(sampleJson)

        assertEquals(2, habitKitData.formatVersion)
        assertEquals(2, habitKitData.habits.size)
        assertEquals(2, habitKitData.intervals.size)
        assertEquals(4, habitKitData.completions.size)

        // Habit "Hehs" (inverse habit)
        val hehs = habitKitData.habits.find { it.name == "Hehs" }!!
        assertTrue(hehs.isInverse)
        assertEquals("2026-08-18", hehs.inverseStartDate)

        val hehsInterval = habitKitData.intervals.find { it.habitId == hehs.id }!!
        assertEquals(1, hehsInterval.requiredNumberOfCompletionsPerDay)
        assertEquals("day", hehsInterval.streakType)

        val expectedHehsStartMillis = LocalDate.parse("2026-08-18").atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli().toString()
        val mappedHehsStartDate = if (hehs.isInverse && !hehs.inverseStartDate.isNullOrBlank()) {
            LocalDate.parse(hehs.inverseStartDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli().toString()
        } else null
        assertEquals(expectedHehsStartMillis, mappedHehsStartDate)

        // Habit "Multi" (multi-completion habit: 3 per day)
        val multi = habitKitData.habits.find { it.name == "Multi" }!!
        assertFalse(multi.isInverse)

        val multiInterval = habitKitData.intervals.find { it.habitId == multi.id }!!
        assertEquals(3, multiInterval.requiredNumberOfCompletionsPerDay)
        assertEquals("day", multiInterval.streakType)

        // Check non-zero completions filtering
        val nonZeroCompletions = habitKitData.completions.filter { it.amountOfCompletions > 0 }
        assertEquals(3, nonZeroCompletions.size) // 1 for Aug 19, 1 for Aug 24, 2 for Aug 26
    }

    @Test
    fun testDailyTargetCapTo14() {
        val habitUnder14 = createHabit(completionsPerDay = 5)
        assertEquals(5, habitUnder14.getDailyTarget())

        val habitAt14 = createHabit(completionsPerDay = 14)
        assertEquals(14, habitAt14.getDailyTarget())

        val habitOver14 = createHabit(completionsPerDay = 25)
        assertEquals(14, habitOver14.getDailyTarget())

        val habitIntervalOver14 = createHabit(completionsPerInterval = 20, intervalUnit = "day")
        assertEquals(14, habitIntervalOver14.getDailyTarget())

        val habitZero = createHabit(completionsPerDay = 0)
        assertEquals(1, habitZero.getDailyTarget())
    }

    @Test
    fun testQuitHabit_uncompleteInReverse() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = true, completionsPerDay = 4)

        // Default: 0 slips in DB -> 4 completions (fully completed)
        val defaultCompletions = emptyList<Completion>()
        assertEquals(4, getEffectiveCompletionsForDay(habit, defaultCompletions, today))
        assertTrue(isDayCompleted(habit, defaultCompletions, today))

        // 1 slip -> 3 completions
        val oneSlip = listOf(Completion("s-1", habit.id, today + 1000L, 0, 1))
        assertEquals(3, getEffectiveCompletionsForDay(habit, oneSlip, today))
        assertFalse(isDayCompleted(habit, oneSlip, today))

        // 2 slips -> 2 completions
        val twoSlips = listOf(Completion("s-2", habit.id, today + 1000L, 0, 2))
        assertEquals(2, getEffectiveCompletionsForDay(habit, twoSlips, today))

        // 3 slips -> 1 completion
        val threeSlips = listOf(Completion("s-3", habit.id, today + 1000L, 0, 3))
        assertEquals(1, getEffectiveCompletionsForDay(habit, threeSlips, today))

        // 4 slips -> 0 completions
        val fourSlips = listOf(Completion("s-4", habit.id, today + 1000L, 0, 4))
        assertEquals(0, getEffectiveCompletionsForDay(habit, fourSlips, today))
    }
}
