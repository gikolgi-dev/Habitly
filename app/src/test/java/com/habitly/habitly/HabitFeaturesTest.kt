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
import com.habitly.habitly.data.Database.getEffectiveStartDateMillis
import com.habitly.habitly.data.calculateStatistics
import com.habitly.habitly.data.getCompletedDays
import com.habitly.habitly.data.getTotalEffectiveCompletions
import org.junit.Assert.assertEquals
import com.habitly.habitly.ui.screen.settings.ExportData
import com.habitly.habitly.ui.screen.settings.HabitKitExport
import kotlinx.serialization.json.Json
import com.habitly.habitly.data.settings.ExportedSettings
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.UUID
import com.habitly.habitly.ui.components.getDaysAndDayValues

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
    fun testConversionBuildToQuit_invertingCompletionsKeepsRawStorage() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000) // Day 0, Day 1, Day 2 (today)
        val buildHabit = createHabit(isInverse = false, completionsPerInterval = 1, startDate = threeDaysAgo)

        val day0 = threeDaysAgo
        val day1 = threeDaysAgo + (24 * 3600 * 1000L)
        val day2 = today

        // Day 0 completed, Day 1 empty, Day 2 completed
        val buildCompletions = listOf(
            Completion("c0", buildHabit.id, day0 + 5000L, 0, 1),
            Completion("c2", buildHabit.id, day2 + 5000L, 0, 1)
        )

        assertEquals(1, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day0))
        assertEquals(0, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day1))
        assertEquals(1, getEffectiveCompletionsForDay(buildHabit, buildCompletions, day2))

        // When the user chooses "Invert completions":
        // Backend does NOT rewrite completions. Existing raw completions remain as-is.
        // Under Quit habit semantics, having a DB row is a slip (0 completions),
        // and lack of DB row is a completion (1 completion).
        val quitHabitInverted = buildHabit.copy(isInverse = true)
        // The same completions list:
        assertEquals(0, getEffectiveCompletionsForDay(quitHabitInverted, buildCompletions, day0)) // was completed, now slip (inverted)
        assertEquals(1, getEffectiveCompletionsForDay(quitHabitInverted, buildCompletions, day1)) // was empty, now completed (inverted)
        assertEquals(0, getEffectiveCompletionsForDay(quitHabitInverted, buildCompletions, day2)) // was completed, now slip (inverted)
    }

    @Test
    fun testTargetConversion_absoluteVsPercentage() {
        // Case 1: Simple habit 1 or 0 converted to target 3
        val oldTarget1 = 1
        val newTarget3 = 3
        val completionAmount1 = 1

        // Absolute: 1 stays 1
        val absoluteVal = completionAmount1
        assertEquals(1, absoluteVal)

        // Percentage: 1/1 -> 100% of 3 = 3
        val percentageVal = Math.round(completionAmount1.toFloat() * newTarget3 / oldTarget1.toFloat()).coerceIn(0, newTarget3)
        assertEquals(3, percentageVal)

        // Case 2: Arbitrary target (e.g. 6) converted to target 3 (or vice versa)
        val oldTarget6 = 6
        val newTarget6to3 = 3
        val completionAmount4 = 4 // 4 out of 6 (66.7%)

        // Absolute: 4 stays 4 (or clamped to newTarget)
        val absoluteVal2 = completionAmount4
        assertEquals(4, absoluteVal2)

        // Percentage: 4 * 3 / 6 = 2 (66.7% of 3)
        val percentageVal2 = Math.round(completionAmount4.toFloat() * newTarget6to3 / oldTarget6.toFloat()).coerceIn(0, newTarget6to3)
        assertEquals(2, percentageVal2)
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
        val firstDay = Calendar.MONDAY
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
        val testNow = startOfWeek + (4 * 24 * 3600 * 1000L) + 5000L
        val stats5 = calculateStatistics(hwc5, firstDay, testNow)
        // 5 days completed -> streak is 5 days, not 10 completions
        assertEquals(5, stats5.currentStreak)
        assertEquals(5, stats5.longestStreak)
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
    fun testExportData_includesSettingsWhenExported() {
        val customSettings = ExportedSettings(
            theme = "dark",
            useMaterialTheming = false,
            borders = 0.5f,
            firstDayOfWeek = "sunday",
            heatmapWeeks = 30,
            vibrations = false,
            globalNotificationsEnabled = true,
            globalNotificationTime = "08:30",
            globalNotificationDays = "MON,WED,FRI",
            autoScrollText = true,
            autoScrollTextElements = "Title",
            autoScrollTextScreens = "Main Screen",
            ignoreWelcomeEngine = true
        )
        val exportData = ExportData(
            habits = emptyList(),
            settings = customSettings
        )

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(ExportData.serializer(), exportData)

        // Verify settings are included in the JSON export
        assertTrue(encoded.contains("\"settings\""))
        assertTrue(encoded.contains("\"theme\":\"dark\"") || encoded.contains("\"theme\": \"dark\""))
        assertTrue(encoded.contains("\"firstDayOfWeek\":\"sunday\"") || encoded.contains("\"firstDayOfWeek\": \"sunday\""))
        assertTrue(encoded.contains("\"globalNotificationTime\":\"08:30\"") || encoded.contains("\"globalNotificationTime\": \"08:30\""))
        assertTrue(encoded.contains("\"heatmapWeeks\":30") || encoded.contains("\"heatmapWeeks\": 30"))
        assertTrue(encoded.contains("\"ignoreWelcomeEngine\":true") || encoded.contains("\"ignoreWelcomeEngine\": true"))

        // Decode and verify all values deserialize correctly
        val decoded = json.decodeFromString(ExportData.serializer(), encoded)
        assertNotNull(decoded.settings)
        assertEquals("dark", decoded.settings?.theme)
        assertFalse(decoded.settings?.useMaterialTheming ?: true)
        assertEquals(0.5f, decoded.settings?.borders ?: 0f, 0.001f)
        assertEquals("sunday", decoded.settings?.firstDayOfWeek)
        assertEquals(30, decoded.settings?.heatmapWeeks)
        assertFalse(decoded.settings?.vibrations ?: true)
        assertTrue(decoded.settings?.globalNotificationsEnabled ?: false)
        assertEquals("08:30", decoded.settings?.globalNotificationTime)
        assertEquals("MON,WED,FRI", decoded.settings?.globalNotificationDays)
        assertTrue(decoded.settings?.autoScrollText ?: false)
        assertEquals("Title", decoded.settings?.autoScrollTextElements)
        assertEquals("Main Screen", decoded.settings?.autoScrollTextScreens)
        assertTrue(decoded.settings?.ignoreWelcomeEngine ?: false)
    }

    @Test
    fun testExportData_settingsBackwardsCompatibleWithoutIgnoreWelcomeEngine() {
        val jsonWithoutIgnoreEngine = """
            {
                "appOrigin": "habitly",
                "version": 2,
                "formatVersion": 2,
                "habits": [],
                "settings": {
                    "theme": "dark"
                }
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString(ExportData.serializer(), jsonWithoutIgnoreEngine)

        assertNotNull(decoded.settings)
        assertEquals("dark", decoded.settings?.theme)
        assertFalse(decoded.settings?.ignoreWelcomeEngine ?: true)
    }

    @Test
    fun testExportData_ignoreSettingsLogic() {
        val customSettings = ExportedSettings(
            theme = "dark",
            borders = 0.8f
        )
        val exportData = ExportData(
            habits = emptyList(),
            settings = customSettings
        )

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(ExportData.serializer(), exportData)
        val decoded = json.decodeFromString(ExportData.serializer(), encoded)

        // When ignoreSettings is true, settings should be discarded / ignored
        val ignoreSettings = true
        val effectiveSettings = if (ignoreSettings) null else decoded.settings
        assertNull(effectiveSettings)

        // When ignoreSettings is false, settings are preserved
        val ignoreSettingsFalse = false
        val importedSettings = if (!ignoreSettingsFalse) decoded.settings else null
        assertNotNull(importedSettings)
        assertEquals("dark", importedSettings?.theme)
    }

    @Test
    fun testExportData_backwardsCompatibleWithoutSettings() {
        // Older backup JSON that does not have "settings" key
        val jsonWithoutSettings = """
            {
                "appOrigin": "habitly",
                "version": 2,
                "formatVersion": 2,
                "habits": []
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString(ExportData.serializer(), jsonWithoutSettings)

        assertEquals("habitly", decoded.appOrigin)
        assertEquals(2, decoded.version)
        assertNull(decoded.settings)
    }

    @Test
    fun testExportData_ignoreInWelcomeCardField() {
        val habit1 = com.habitly.habitly.ui.screen.settings.ExportedHabit(
            id = "h-1",
            name = "Habit 1",
            description = "Desc",
            icon = "Book",
            color = 123,
            archived = false,
            orderIndex = 0,
            createdAt = "1000",
            isInverse = false,
            emoji = null,
            completionsPerInterval = 1,
            intervalUnit = "day",
            notificationsEnabled = false,
            notificationTime = null,
            notificationDays = null,
            ignoreInWelcomeCard = true,
            completions = emptyList()
        )
        val habit2 = com.habitly.habitly.ui.screen.settings.ExportedHabit(
            id = "h-2",
            name = "Habit 2",
            description = "Desc",
            icon = "Book",
            color = 123,
            archived = false,
            orderIndex = 1,
            createdAt = "1000",
            isInverse = false,
            emoji = null,
            completionsPerInterval = 1,
            intervalUnit = "day",
            notificationsEnabled = false,
            notificationTime = null,
            notificationDays = null,
            ignoreInWelcomeCard = false,
            completions = emptyList()
        )
        val exportData = ExportData(habits = listOf(habit1, habit2))
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(ExportData.serializer(), exportData)

        assertTrue(encoded.contains("\"ignoreInWelcomeCard\":true") || encoded.contains("\"ignoreInWelcomeCard\": true"))
        assertTrue(encoded.contains("\"ignoreInWelcomeCard\":false") || encoded.contains("\"ignoreInWelcomeCard\": false"))

        val decoded = json.decodeFromString(ExportData.serializer(), encoded)
        assertEquals(true, decoded.habits[0].ignoreInWelcomeCard)
        assertEquals(false, decoded.habits[1].ignoreInWelcomeCard)
    }

    @Test
    fun testExportData_backwardsCompatibleWithoutIgnoreInWelcomeCard() {
        val legacyJson = """
            {
                "appOrigin": "habitly",
                "version": 2,
                "formatVersion": 2,
                "habits": [
                    {
                        "id": "h-legacy",
                        "name": "Legacy Habit",
                        "description": "",
                        "icon": "Book",
                        "color": 0,
                        "archived": false,
                        "orderIndex": 0,
                        "createdAt": "1000",
                        "isInverse": false,
                        "emoji": null,
                        "completionsPerInterval": 1,
                        "intervalUnit": "day",
                        "notificationsEnabled": false,
                        "notificationTime": null,
                        "notificationDays": null,
                        "completions": []
                    }
                ]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString(ExportData.serializer(), legacyJson)
        assertEquals(1, decoded.habits.size)
        // Default must be false (default behaviour is to show)
        assertEquals(false, decoded.habits[0].ignoreInWelcomeCard)
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

    @Test
    fun testIsoCreatedAt_effectiveStartDate_andCompletionsNotWiped() {
        // Habit with ISO-8601 createdAt and null startDate (like in older backups / pre-migration app state)
        val isoCreatedAt = "2023-11-16T17:16:39.139951Z"
        val habit = Habit(
            id = "yk-habit-id",
            name = "YK",
            description = "",
            icon = "Spa",
            color = -1416351,
            archived = false,
            orderIndex = 0,
            createdAt = isoCreatedAt,
            isInverse = false,
            emoji = null,
            completionsPerInterval = 1,
            intervalUnit = "day",
            startDate = null
        )

        val expectedMillis = java.time.Instant.parse(isoCreatedAt).toEpochMilli()
        assertEquals(expectedMillis, habit.getEffectiveStartDateMillis())

        // A completion from late 2023 (before today, but after habit creation)
        val completionDate = expectedMillis + (10L * 24 * 3600 * 1000)
        val completions = listOf(
            Completion(
                id = "c-1",
                habitId = habit.id,
                date = completionDate,
                timezoneOffsetInMinutes = 60,
                amountOfCompletions = 1
            )
        )

        val effective = getEffectiveCompletionsForDay(habit, completions, completionDate)
        assertEquals(1, effective)
        assertTrue(isDayCompleted(habit, completions, completionDate))
    }

    @Test
    fun testFirstDayOfWeek_weeklyStreakChangesWithFirstDayOfWeekSetting() {
        // Setup a specific reference week starting Monday, Sep 7, 2026
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 0, 0, 0)
        }
        val mondayStart = cal.timeInMillis // Sep 7, 2026 (Mon)
        val sundayEndOfWeek = mondayStart + (6L * 24 * 3600 * 1000) // Sep 13, 2026 (Sun)
        val nextMonday = mondayStart + (7L * 24 * 3600 * 1000) // Sep 14, 2026 (Mon)
        val nextTuesday = mondayStart + (8L * 24 * 3600 * 1000) // Sep 15, 2026 (Tue)

        // Habit: 3 completions per week
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 3,
            intervalUnit = "week",
            completionsPerDay = 1,
            startDate = mondayStart
        )

        // Completions on: Sep 7 (Mon), Sep 8 (Tue), Sep 9 (Wed), Sep 13 (Sun), Sep 14 (Mon), Sep 15 (Tue)
        val completions = listOf(
            Completion("c-1", habit.id, mondayStart + 1000L, 0, 1),
            Completion("c-2", habit.id, mondayStart + (1L * 24 * 3600 * 1000) + 1000L, 0, 1),
            Completion("c-3", habit.id, mondayStart + (2L * 24 * 3600 * 1000) + 1000L, 0, 1),
            Completion("c-4", habit.id, sundayEndOfWeek + 1000L, 0, 1),
            Completion("c-5", habit.id, nextMonday + 1000L, 0, 1),
            Completion("c-6", habit.id, nextTuesday + 1000L, 0, 1)
        )

        val hwc = HabitWithCompletions(habit, completions)
        val evalTime = nextTuesday + 5000L

        // Under firstDayOfWeek = MONDAY:
        // Week 1 (Mon Sep 7 - Sun Sep 13) has 4 completions (Mon, Tue, Wed, Sun) >= 3 (Target met!).
        // Week 2 (Mon Sep 14 - Sun Sep 20) has 2 completions (Mon, Tue) < 3.
        val statsMonday = calculateStatistics(hwc, Calendar.MONDAY, evalTime)
        // Week 1 had 4 completed days (all 4 count, not capped at 3), Week 2 has 2 completed days (in-progress/optimistic)
        // Total current streak: 4 + 2 = 6 days
        assertEquals(6, statsMonday.currentStreak)

        // Under firstDayOfWeek = SUNDAY:
        // Week 1 (Sun Aug 30 - Sat Sep 5) has 0 completions.
        // Week 2 (Sun Sep 6 - Sat Sep 12) has 3 completions (Mon Sep 7, Tue Sep 8, Wed Sep 9) >= 3 (Target met!).
        // Week 3 (Sun Sep 13 - Sat Sep 19) has 3 completions (Sun Sep 13, Mon Sep 14, Tue Sep 15) >= 3 (Target met!).
        // Total current streak: 3 + 3 = 6
        val statsSunday = calculateStatistics(hwc, Calendar.SUNDAY, evalTime)
        assertEquals(6, statsSunday.currentStreak)
    }

    @Test
    fun testStreak_todayCompletedToMaxLevelDoesNotExceedMax() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000)
        // Multiple completions: 5 per day (max level), streak target: 3
        val habit = createHabit(isInverse = false, completionsPerInterval = 3, completionsPerDay = 5, startDate = threeDaysAgo)

        // Yesterday completed with 4 completions (>= 3). Today has 0.
        val completionsYesterday = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4)
        )
        val statsBeforeToday = calculateStatistics(HabitWithCompletions(habit, completionsYesterday), Calendar.MONDAY, today + 5000L)
        // Streak has not failed today, counts yesterday = 1 day
        assertEquals(1, statsBeforeToday.currentStreak)

        // Today reaches streak target (3 completions)
        val completionsTarget = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4),
            Completion("c-2", habit.id, today + 1000L, 0, 3)
        )
        val statsTarget = calculateStatistics(HabitWithCompletions(habit, completionsTarget), Calendar.MONDAY, today + 5000L)
        // Streak adds 1 day for today = 2 days
        assertEquals(2, statsTarget.currentStreak)
        assertEquals(2, statsTarget.longestStreak)
        assertEquals(0L, statsTarget.daysSinceLongestStreak) // Active today

        // Today completed to the MAX level (5 completions):
        val completionsMaxLevel = listOf(
            Completion("c-1", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4),
            Completion("c-2", habit.id, today + 1000L, 0, 5) // max level 5!
        )
        val statsMaxLevel = calculateStatistics(HabitWithCompletions(habit, completionsMaxLevel), Calendar.MONDAY, today + 5000L)
        // Reaching max level should NOT add max completions or inflate streak: streak is STILL 2 days!
        assertEquals(2, statsMaxLevel.currentStreak)
        assertEquals(2, statsMaxLevel.longestStreak)
        assertEquals(0L, statsMaxLevel.daysSinceLongestStreak) // Active today
    }

    @Test
    fun testFirstDayOfWeek_calendarOffsetCalculation() {
        // Test for a month where the 1st day is a Sunday (e.g. November 2026: Nov 1, 2026 is Sunday)
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 1)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY = 1
        assertEquals(Calendar.SUNDAY, dayOfWeek)

        // If firstDayOfWeek is MONDAY (2):
        // Sunday is 7th day of the week, so offset is 6
        val offsetMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        assertEquals(6, offsetMonday)

        // If firstDayOfWeek is SUNDAY (1):
        // Sunday is 1st day of the week, so offset is 0
        val offsetSunday = (dayOfWeek - Calendar.SUNDAY + 7) % 7
        assertEquals(0, offsetSunday)
    }

    @Test
    fun testFirstDayOfWeek_dayOfWeekSelector_mondayStart() {
        val (days, dayValues) = getDaysAndDayValues(Calendar.MONDAY)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), days)
        assertEquals(listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"), dayValues)
    }

    @Test
    fun testFirstDayOfWeek_dayOfWeekSelector_sundayStart() {
        val (days, dayValues) = getDaysAndDayValues(Calendar.SUNDAY)
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), days)
        assertEquals(listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"), dayValues)
    }

    @Test
    fun testFirstDayOfWeek_dayOfWeekSelector_saturdayStart() {
        val (days, dayValues) = getDaysAndDayValues(Calendar.SATURDAY)
        assertEquals(listOf("S", "S", "M", "T", "W", "T", "F"), days)
        assertEquals(listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI"), dayValues)
    }

    @Test
    fun testStreak_countsDaysNotCompletions_weeklyHabit() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Sep 7, 2026 is Monday
        }
        val mondayStart = normalizeToStartOfDay(cal.timeInMillis)
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 3,
            intervalUnit = "week",
            completionsPerDay = 1,
            startDate = mondayStart
        )

        // Case 1: 3 completions in 3 days (Mon, Wed, Fri) -> target satisfied, streak is 3 days
        val completions3Days = listOf(
            Completion("c-1", habit.id, mondayStart + 1000L, 0, 1),
            Completion("c-2", habit.id, mondayStart + (2L * 24 * 3600 * 1000) + 1000L, 0, 1),
            Completion("c-3", habit.id, mondayStart + (4L * 24 * 3600 * 1000) + 1000L, 0, 1)
        )
        val endOfWeekEval = mondayStart + (6L * 24 * 3600 * 1000) + 5000L // Sunday
        val stats3 = calculateStatistics(HabitWithCompletions(habit, completions3Days), Calendar.MONDAY, endOfWeekEval)
        assertEquals(3, stats3.currentStreak)
        assertEquals(3, stats3.longestStreak)

        // Case 2 (Principle 2): Target is 3, but completions on all 7 days -> streak is 7 days, NOT capped at 3!
        val completions7Days = (0..6).map { dayOffset ->
            Completion("c-$dayOffset", habit.id, mondayStart + (dayOffset * 24 * 3600 * 1000L) + 1000L, 0, 1)
        }
        val stats7 = calculateStatistics(HabitWithCompletions(habit, completions7Days), Calendar.MONDAY, endOfWeekEval)
        assertEquals(7, stats7.currentStreak)
        assertEquals(7, stats7.longestStreak)
    }

    @Test
    fun testStreak_optimisticAboutTheFuture_mondayMorningDoesNotFail() {
        // Principle 3: "it should be optimistic about the future ie it shouldnt fail if the amount
        // of completions was not possible to be achieved because you can do 1 completion per day need 3 per week and its only monday"
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Sep 7, 2026 is Monday
        }
        val week1Monday = normalizeToStartOfDay(cal.timeInMillis)
        val week2Monday = week1Monday + (7L * 24 * 3600 * 1000) // Sep 14, 2026 is Monday
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 3,
            intervalUnit = "week",
            completionsPerDay = 1,
            startDate = week1Monday
        )

        // Week 1 has completions on all 7 days (7 days streak from week 1)
        val week1Completions = (0..6).map { dayOffset ->
            Completion("w1-$dayOffset", habit.id, week1Monday + (dayOffset * 24 * 3600 * 1000L) + 1000L, 0, 1)
        }

        // On Week 2 Monday MORNING (0 completions so far this week):
        // Needs 3 per week, only Monday today, can do 1/day, impossible to have achieved 3 yet,
        // but 7 days remain so goal CAN be achieved. Streak MUST NOT fail!
        val mondayMorningEval = week2Monday + (8 * 3600 * 1000L) // 8:00 AM
        val statsMondayMorning = calculateStatistics(HabitWithCompletions(habit, week1Completions), Calendar.MONDAY, mondayMorningEval)
        assertEquals(7, statsMondayMorning.currentStreak)
        assertEquals(7, statsMondayMorning.longestStreak)

        // On Week 2 Monday EVENING: user completed Monday (1 completion)
        val completionsWithMon = week1Completions + listOf(
            Completion("w2-mon", habit.id, week2Monday + (18 * 3600 * 1000L), 0, 1)
        )
        val mondayEveningEval = week2Monday + (19 * 3600 * 1000L)
        val statsMondayEvening = calculateStatistics(HabitWithCompletions(habit, completionsWithMon), Calendar.MONDAY, mondayEveningEval)
        assertEquals(8, statsMondayEvening.currentStreak)
        assertEquals(8, statsMondayEvening.longestStreak)

        // On Week 2 Tuesday EVENING: user completed Tuesday (1 completion)
        val week2Tuesday = week2Monday + (24 * 3600 * 1000L)
        val completionsWithTue = completionsWithMon + listOf(
            Completion("w2-tue", habit.id, week2Tuesday + (18 * 3600 * 1000L), 0, 1)
        )
        val tuesdayEveningEval = week2Tuesday + (19 * 3600 * 1000L)
        val statsTuesdayEvening = calculateStatistics(HabitWithCompletions(habit, completionsWithTue), Calendar.MONDAY, tuesdayEveningEval)
        assertEquals(9, statsTuesdayEvening.currentStreak)
        assertEquals(9, statsTuesdayEvening.longestStreak)

        // On Week 2 Wednesday EVENING: user completed Wednesday (3rd completion -> weekly target met!)
        val week2Wednesday = week2Monday + (2L * 24 * 3600 * 1000L)
        val completionsWithWed = completionsWithTue + listOf(
            Completion("w2-wed", habit.id, week2Wednesday + (18 * 3600 * 1000L), 0, 1)
        )
        val wednesdayEveningEval = week2Wednesday + (19 * 3600 * 1000L)
        val statsWednesdayEvening = calculateStatistics(HabitWithCompletions(habit, completionsWithWed), Calendar.MONDAY, wednesdayEveningEval)
        assertEquals(10, statsWednesdayEvening.currentStreak)
        assertEquals(10, statsWednesdayEvening.longestStreak)
    }

    @Test
    fun testStreak_optimisticStreakBreaksWhenTargetBecomesImpossible() {
        // Target is 3 completions per week, 1 per day.
        // If user has only 1 completion by Sunday, max remaining is 1 (today).
        // 1 + 1 = 2 < 3, so target is impossible. Streak MUST break.
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Sep 7, 2026 is Monday
        }
        val week1Monday = normalizeToStartOfDay(cal.timeInMillis)
        val week2Monday = week1Monday + (7L * 24 * 3600 * 1000) // Sep 14, 2026 is Monday
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 3,
            intervalUnit = "week",
            completionsPerDay = 1,
            startDate = week1Monday
        )

        // Week 1 has 7 completed days
        val week1Completions = (0..6).map { dayOffset ->
            Completion("w1-$dayOffset", habit.id, week1Monday + (dayOffset * 24 * 3600 * 1000L) + 1000L, 0, 1)
        }

        // In Week 2, user only completed Monday:
        val week2Completions = week1Completions + listOf(
            Completion("w2-mon", habit.id, week2Monday + 1000L, 0, 1)
        )

        // On Saturday of Week 2 (Sep 19):
        // 1 completion so far (Mon). Remaining days: Saturday (today) and Sunday (2 days).
        // Max possible = 1 + (1 - 0) + 1 = 3 >= 3 -> Still possible! Streak is preserved (7 + 1 = 8)
        val week2Saturday = week2Monday + (5L * 24 * 3600 * 1000L)
        val statsSaturday = calculateStatistics(HabitWithCompletions(habit, week2Completions), Calendar.MONDAY, week2Saturday + 5000L)
        assertEquals(8, statsSaturday.currentStreak)
        assertEquals(8, statsSaturday.longestStreak)

        // On Sunday of Week 2 (Sep 20):
        // 1 completion so far (Mon). Remaining days: Sunday (today, 0 future days).
        // Max possible = 1 + (1 - 0) + 0 = 2 < 3 -> Impossible! Streak breaks!
        val week2Sunday = week2Monday + (6L * 24 * 3600 * 1000L)
        val statsSunday = calculateStatistics(HabitWithCompletions(habit, week2Completions), Calendar.MONDAY, week2Sunday + 5000L)
        assertEquals(0, statsSunday.currentStreak)
        // Longest streak was Week 1 (7 days). Week 2 failed, so its incomplete day does not count as a satisfied streak.
        assertEquals(7, statsSunday.longestStreak)
        // Days since longest streak: longest streak ended on Week 1 Sunday (7 days ago from Sunday Sep 20)
        assertEquals(7L, statsSunday.daysSinceLongestStreak)
    }

    @Test
    fun testStreak_multiCompletionHabit_countsDaysNotCompletions() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val threeDaysAgo = today - (2L * 24 * 3600 * 1000)
        // Daily habit with multiple completions: 5 max per day, streak target: 3
        val habit = createHabit(isInverse = false, completionsPerInterval = 3, completionsPerDay = 5, startDate = threeDaysAgo)

        // Day 1 (2 days ago): 5 completions
        // Day 2 (yesterday): 4 completions
        // Day 3 (today): 3 completions
        val completions = listOf(
            Completion("c-1", habit.id, threeDaysAgo + 1000L, 0, 5),
            Completion("c-2", habit.id, today - (24 * 3600 * 1000) + 1000L, 0, 4),
            Completion("c-3", habit.id, today + 1000L, 0, 3)
        )
        val stats = calculateStatistics(HabitWithCompletions(habit, completions), Calendar.MONDAY, today + 5000L)
        // Total completions = 5 + 4 + 3 = 12.
        // But streak MUST count days, not completions -> streak is 3 days!
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(12, stats.totalCompletions)
    }

    @Test
    fun testStreak_multiCompletionWeeklyHabit() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Sep 7, 2026 is Monday
        }
        val mondayStart = normalizeToStartOfDay(cal.timeInMillis)
        // Weekly habit: 2 completions max per day, 6 completions required per week
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 6,
            intervalUnit = "week",
            completionsPerDay = 2,
            startDate = mondayStart
        )

        // Case A: 3 days with 2 completions each (total 6 completions)
        // Satisfies weekly target (6 >= 6). Counts 3 days, not 6 completions!
        val completions3Days = listOf(
            Completion("c-1", habit.id, mondayStart + 1000L, 0, 2),
            Completion("c-2", habit.id, mondayStart + (2L * 24 * 3600 * 1000) + 1000L, 0, 2),
            Completion("c-3", habit.id, mondayStart + (4L * 24 * 3600 * 1000) + 1000L, 0, 2)
        )
        val endOfWeekEval = mondayStart + (6L * 24 * 3600 * 1000) + 5000L
        val stats3Days = calculateStatistics(HabitWithCompletions(habit, completions3Days), Calendar.MONDAY, endOfWeekEval)
        assertEquals(3, stats3Days.currentStreak)
        assertEquals(3, stats3Days.longestStreak)
        assertEquals(6, stats3Days.totalCompletions)

        // Case B: 7 days with 2 completions each (total 14 completions)
        // Satisfies weekly target. All 7 days count to streak!
        val completions7Days = (0..6).map { dayOffset ->
            Completion("c-$dayOffset", habit.id, mondayStart + (dayOffset * 24 * 3600 * 1000L) + 1000L, 0, 2)
        }
        val stats7Days = calculateStatistics(HabitWithCompletions(habit, completions7Days), Calendar.MONDAY, endOfWeekEval)
        assertEquals(7, stats7Days.currentStreak)
        assertEquals(7, stats7Days.longestStreak)
        assertEquals(14, stats7Days.totalCompletions)
    }

    @Test
    fun testStreak_monthlyHabit_countsDaysAndOptimistic() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 0, 0) // Jan 1, 2026
        }
        val jan1 = normalizeToStartOfDay(cal.timeInMillis)
        // Monthly habit: 10 completions per month, 1 per day
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 10,
            intervalUnit = "month",
            completionsPerDay = 1,
            startDate = jan1
        )

        // January: 15 completed days (Jan 1 to Jan 15)
        val janCompletions = (0..14).map { dayOffset ->
            Completion("jan-$dayOffset", habit.id, jan1 + (dayOffset * 24 * 3600 * 1000L) + 1000L, 0, 1)
        }
        val janEndEval = jan1 + (30L * 24 * 3600 * 1000L) + 5000L // Jan 31
        val janStats = calculateStatistics(HabitWithCompletions(habit, janCompletions), Calendar.MONDAY, janEndEval)
        // 15 days completed >= 10 target -> all 15 days count!
        assertEquals(15, janStats.currentStreak)
        assertEquals(15, janStats.longestStreak)

        // February 2: user completed Feb 1 and Feb 2 (2 completions)
        cal.set(2026, Calendar.FEBRUARY, 1)
        val feb1 = normalizeToStartOfDay(cal.timeInMillis)
        val febCompletions = janCompletions + listOf(
            Completion("feb-0", habit.id, feb1 + 1000L, 0, 1),
            Completion("feb-1", habit.id, feb1 + (24 * 3600 * 1000L) + 1000L, 0, 1)
        )
        val feb2Eval = feb1 + (24 * 3600 * 1000L) + 5000L
        val febStats = calculateStatistics(HabitWithCompletions(habit, febCompletions), Calendar.MONDAY, feb2Eval)
        // Optimistic! Streak continues: 15 (Jan) + 2 (Feb) = 17 days
        assertEquals(17, febStats.currentStreak)
        assertEquals(17, febStats.longestStreak)
    }

    @Test
    fun testLongestStreak_multipleEqualLongestStreaks_picksMostRecent() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val start = normalizeToStartOfDay(cal.timeInMillis)
        val habit = createHabit(isInverse = false, completionsPerInterval = 1, startDate = start)

        // Streak 1: Jan 1, 2, 3 (3 days)
        // Break: Jan 4, 5 (missed)
        // Streak 2: Jan 6, 7, 8 (3 days)
        // Eval time: Jan 10
        val completions = listOf(
            Completion("c-1", habit.id, start + 1000L, 0, 1),
            Completion("c-2", habit.id, start + (24 * 3600 * 1000L) + 1000L, 0, 1),
            Completion("c-3", habit.id, start + (2L * 24 * 3600 * 1000L) + 1000L, 0, 1),
            Completion("c-4", habit.id, start + (5L * 24 * 3600 * 1000L) + 1000L, 0, 1),
            Completion("c-5", habit.id, start + (6L * 24 * 3600 * 1000L) + 1000L, 0, 1),
            Completion("c-6", habit.id, start + (7L * 24 * 3600 * 1000L) + 1000L, 0, 1)
        )

        val evalTime = start + (9L * 24 * 3600 * 1000L) + 5000L // Jan 10
        val stats = calculateStatistics(HabitWithCompletions(habit, completions), Calendar.MONDAY, evalTime)

        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        // Streak 1 ended on Jan 3 (7 days before Jan 10)
        // Streak 2 ended on Jan 8 (2 days before Jan 10)
        // It MUST pick Streak 2 (most recent longest streak) -> 2 days ago!
        assertEquals(2L, stats.daysSinceLongestStreak)
    }

    @Test
    fun testLongestStreak_weeklyHabit_multipleEqualLongestStreaks_picksMostRecent() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Sep 7, 2026 is Monday
        }
        val week1Monday = normalizeToStartOfDay(cal.timeInMillis)
        val habit = createHabit(
            isInverse = false,
            completionsPerInterval = 2,
            intervalUnit = "week",
            completionsPerDay = 1,
            startDate = week1Monday
        )

        // Week 1 (Sep 7..Sep 13): Mon, Tue completed (2 days)
        // Week 2 (Sep 14..Sep 20): 0 completed (break)
        // Week 3 (Sep 21..Sep 27): Mon, Tue completed (2 days)
        // Week 4 (Sep 28..Oct 4): 0 completed. Eval on Sunday of Week 4 (Oct 4).
        val week3Monday = week1Monday + (14L * 24 * 3600 * 1000)
        val completions = listOf(
            Completion("w1-mon", habit.id, week1Monday + 1000L, 0, 1),
            Completion("w1-tue", habit.id, week1Monday + (24 * 3600 * 1000L) + 1000L, 0, 1),
            Completion("w3-mon", habit.id, week3Monday + 1000L, 0, 1),
            Completion("w3-tue", habit.id, week3Monday + (24 * 3600 * 1000L) + 1000L, 0, 1)
        )

        val week4Sunday = week1Monday + (27L * 24 * 3600 * 1000) + 5000L // Oct 4
        val stats = calculateStatistics(HabitWithCompletions(habit, completions), Calendar.MONDAY, week4Sunday)

        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
        // Longest streak should be Week 3, not Week 1!
        // Week 3 Tue was Sep 22 (12 days before Oct 4)
        // If it picked Week 1 Tue (Sep 8), it would be 26 days ago.
        assertTrue(stats.daysSinceLongestStreak <= 13L)
    }

    @Test
    fun testHabit_streakCountingDisabled_defaultIsFalse() {
        val habit = createHabit()
        assertFalse(habit.streakCountingDisabled)

        val disabledHabit = habit.copy(streakCountingDisabled = true)
        assertTrue(disabledHabit.streakCountingDisabled)
    }

    @Test
    fun testBuildHabit_continuousDecrement_bounds() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = false, completionsPerInterval = 5)
        val target = habit.getDailyTarget()

        // Start at 4 completions
        var currentEffective = 4
        var decrementCalls = 0

        fun canDecrement(effective: Int, isInverse: Boolean, target: Int): Boolean {
            return if (isInverse) effective < target else effective > 0
        }

        while (canDecrement(currentEffective, habit.isInverse, target)) {
            currentEffective = (currentEffective - 1).coerceAtLeast(0)
            decrementCalls++
        }

        assertEquals(4, decrementCalls)
        assertEquals(0, currentEffective)
        assertFalse(canDecrement(currentEffective, habit.isInverse, target))
    }

    @Test
    fun testQuitHabit_continuousDecrement_bounds() {
        val today = normalizeToStartOfDay(System.currentTimeMillis())
        val habit = createHabit(isInverse = true, completionsPerInterval = 4)
        val target = habit.getDailyTarget()

        // For inverse habit, 3 slips means effective = 4 - 3 = 1
        var currentEffective = 1
        var decrementCalls = 0

        fun canDecrement(effective: Int, isInverse: Boolean, target: Int): Boolean {
            return if (isInverse) effective < target else effective > 0
        }

        while (canDecrement(currentEffective, habit.isInverse, target)) {
            currentEffective = (currentEffective + 1).coerceAtMost(target)
            decrementCalls++
        }

        assertEquals(3, decrementCalls)
        assertEquals(target, currentEffective)
        assertFalse(canDecrement(currentEffective, habit.isInverse, target))
    }

    @Test
    fun testBuildHabit_decrementAtZero_noDecrements() {
        val habit = createHabit(isInverse = false, completionsPerInterval = 5)
        val target = habit.getDailyTarget()
        val currentEffective = 0

        fun canDecrement(effective: Int, isInverse: Boolean, target: Int): Boolean {
            return if (isInverse) effective < target else effective > 0
        }

        assertFalse(canDecrement(currentEffective, habit.isInverse, target))
    }

    @Test
    fun testQuitHabit_decrementAtTarget_noDecrements() {
        val habit = createHabit(isInverse = true, completionsPerInterval = 4)
        val target = habit.getDailyTarget()
        val currentEffective = target // Already at target (no slips to undo)

        fun canDecrement(effective: Int, isInverse: Boolean, target: Int): Boolean {
            return if (isInverse) effective < target else effective > 0
        }

        assertFalse(canDecrement(currentEffective, habit.isInverse, target))
    }
}

