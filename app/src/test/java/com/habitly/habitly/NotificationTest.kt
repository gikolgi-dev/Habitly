/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly

import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.notifications.getDayBounds
import com.habitly.habitly.notifications.getNextAlarmTime
import com.habitly.habitly.notifications.shouldShowHabitNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NotificationTest {

    @Test
    fun defaultSettings_exactAlarmsIsFalse() {
        assertEquals(false, DefaultSettings.EXACT_ALARMS)
    }

    @Test
    fun getNextAlarmTime_emptyDays_returnsNull() {
        val result = getNextAlarmTime(9, 0, emptySet())
        assertNull(result)
    }

    @Test
    fun getNextAlarmTime_futureToday_returnsTimestamp() {
        val now = Calendar.getInstance()
        now.add(Calendar.HOUR_OF_DAY, 2)
        val dayMap = mapOf(
            Calendar.SUNDAY to "SUN",
            Calendar.MONDAY to "MON",
            Calendar.TUESDAY to "TUE",
            Calendar.WEDNESDAY to "WED",
            Calendar.THURSDAY to "THU",
            Calendar.FRIDAY to "FRI",
            Calendar.SATURDAY to "SAT"
        )
        val todayStr = dayMap[now.get(Calendar.DAY_OF_WEEK)]!!

        val result = getNextAlarmTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), setOf(todayStr))
        assertNotNull(result)
        assertTrue(result!! > System.currentTimeMillis())
    }

    @Test
    fun getNextAlarmTime_pastToday_returnsFutureDay() {
        val now = Calendar.getInstance()
        val dayMap = mapOf(
            Calendar.SUNDAY to "SUN",
            Calendar.MONDAY to "MON",
            Calendar.TUESDAY to "TUE",
            Calendar.WEDNESDAY to "WED",
            Calendar.THURSDAY to "THU",
            Calendar.FRIDAY to "FRI",
            Calendar.SATURDAY to "SAT"
        )
        val todayStr = dayMap[now.get(Calendar.DAY_OF_WEEK)]!!

        // Schedule for 00:00 today which is already in the past
        val result = getNextAlarmTime(0, 0, setOf(todayStr))
        assertNotNull(result)
        assertTrue(result!! > System.currentTimeMillis())
    }

    @Test
    fun shouldShowHabitNotification_completedAndSkipEnabled_doesNotShow() {
        // When habit is completed (1 or more completions) and skip is enabled, notification must not show
        assertFalse(shouldShowHabitNotification(skipCompleted = true, completionsCount = 1))
        assertFalse(shouldShowHabitNotification(skipCompleted = true, completionsCount = 3))
    }

    @Test
    fun shouldShowHabitNotification_notCompletedAndSkipEnabled_showsNotification() {
        // When habit is not completed (0 completions) and skip is enabled, notification must show
        assertTrue(shouldShowHabitNotification(skipCompleted = true, completionsCount = 0))
    }

    @Test
    fun shouldShowHabitNotification_completedAndSkipDisabled_showsNotification() {
        // When skip is disabled, notification must always show even if completed
        assertTrue(shouldShowHabitNotification(skipCompleted = false, completionsCount = 1))
        assertTrue(shouldShowHabitNotification(skipCompleted = false, completionsCount = 4))
    }

    @Test
    fun shouldShowHabitNotification_notCompletedAndSkipDisabled_showsNotification() {
        // Baseline: not completed and skip disabled -> shows notification
        assertTrue(shouldShowHabitNotification(skipCompleted = false, completionsCount = 0))
    }

    @Test
    fun getDayBounds_encapsulatesDayCompletely() {
        val calendar = Calendar.getInstance()
        val (startOfDay, endOfDay) = getDayBounds(calendar)

        val calStart = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endOfDay }
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))

        // Midday timestamp must fall within the bounds
        val calNoon = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(calNoon.timeInMillis in startOfDay..endOfDay)

        // Yesterday and tomorrow must not fall within bounds
        val calYesterday = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        assertFalse(calYesterday.timeInMillis in startOfDay..endOfDay)

        val calTomorrow = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertFalse(calTomorrow.timeInMillis in startOfDay..endOfDay)
    }
}
