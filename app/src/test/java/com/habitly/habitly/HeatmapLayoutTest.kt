/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.habitly.habitly.ui.calculateHeatmapLayout
import com.habitly.habitly.ui.resolveHeatmapMonthLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor

class HeatmapLayoutTest {

    private val commonPhoneWidthsDp = listOf(
        320f,   // Compact / small screen
        360f,   // Standard compact (e.g., Galaxy S series default, standard 1080p @ 3.0x)
        384f,   // Nexus 4 / legacy standard
        392.7f, // Pixel 4/5 (1080p @ 2.75x)
        411.4f, // Pixel 7/8/9 (1080p @ 2.625x)
        412f,   // Pixel 6/7/8 Pro / large phone
        480f,   // Small tablet / foldable unfolded
        600f,   // 7-inch tablet
        800f    // 10-inch tablet
    )

    private val commonDensities = listOf(
        1.5f,   // hdpi
        2.0f,   // xhdpi
        2.625f, // Pixel 7/8/9
        2.75f,  // Pixel 4/5
        3.0f,   // xxhdpi
        3.5f,   // Pixel Pro 1440p
        4.0f    // xxxhdpi
    )

    @Test
    fun testHeatmapLayoutNeverOverflowsAvailableWidth() {
        val cellSize = 10.dp
        val minSpacing = 4.dp

        for (widthDp in commonPhoneWidthsDp) {
            for (densityVal in commonDensities) {
                val density = Density(density = densityVal, fontScale = 1.0f)
                val cellSizePx = with(density) { cellSize.roundToPx() }
                val minSpacingPx = with(density) { minSpacing.roundToPx() }

                // Test both MainScreen padding (screenWidth - 44dp) and Settings Preview padding (screenWidth - 52dp)
                val availableWidthsDp = listOf(widthDp - 44f, widthDp - 52f, widthDp)

                for (availDp in availableWidthsDp) {
                    val totalAvailableWidthPx = with(density) { availDp.dp.roundToPx() }
                    if (totalAvailableWidthPx < cellSizePx) continue

                    for (showDayLabels in listOf(false, true)) {
                        val layout = calculateHeatmapLayout(
                            totalAvailableWidthPx = totalAvailableWidthPx,
                            density = density,
                            cellSize = cellSize,
                            minHorizontalSpacing = minSpacing,
                            showDayLabels = showDayLabels
                        )

                        val spacingPx = with(density) { layout.horizontalSpacing.roundToPx() }
                        assertTrue(
                            "Spacing should be at least minSpacing ($minSpacingPx px), got $spacingPx px for W=${availDp}dp, density=$densityVal",
                            spacingPx >= minSpacingPx
                        )

                        val dayLabelsWidthPx = if (showDayLabels) {
                            with(density) { (cellSize + layout.horizontalSpacing).roundToPx() }
                        } else 0

                        val itemsWidthPx = layout.numWeeksOnScreen * cellSizePx + (layout.numWeeksOnScreen - 1) * spacingPx
                        val totalOccupiedPx = itemsWidthPx + dayLabelsWidthPx

                        assertTrue(
                            "Heatmap overflowed available width! totalOccupiedPx=$totalOccupiedPx > availableWidthPx=$totalAvailableWidthPx " +
                                    "(overflow=${totalOccupiedPx - totalAvailableWidthPx}px) for screenWidth=${widthDp}dp, density=$densityVal, " +
                                    "availDp=${availDp}dp, showDayLabels=$showDayLabels, slots=${layout.totalSlots}, numWeeks=${layout.numWeeksOnScreen}",
                            totalOccupiedPx <= totalAvailableWidthPx
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testMainScreenVsSettingsPreviewNoCutoff() {
        // Specifically check the device configurations where MainScreen previously had mid-box cutoff
        // (Pixel 7/8/9 at 411.4dp @ 2.625f and Pixel 4/5 at 392.7dp @ 2.75f)
        val cellSize = 10.dp
        val minSpacing = 4.dp

        val problemConfigs = listOf(
            Pair(411.4f, 2.625f),
            Pair(412f, 2.625f),
            Pair(392.7f, 2.75f),
            Pair(360f, 3.0f)
        )

        for ((widthDp, densityVal) in problemConfigs) {
            val density = Density(density = densityVal, fontScale = 1.0f)
            val cellSizePx = with(density) { cellSize.roundToPx() }

            // Main screen has 12dp card padding each side + 10dp column padding each side = 44dp
            val mainAvailableWidthPx = with(density) { (widthDp - 44f).dp.roundToPx() }
            // Settings preview has 16dp card padding each side + 10dp column padding each side = 52dp
            val settingsAvailableWidthPx = with(density) { (widthDp - 52f).dp.roundToPx() }

            val mainLayout = calculateHeatmapLayout(
                totalAvailableWidthPx = mainAvailableWidthPx,
                density = density,
                cellSize = cellSize,
                minHorizontalSpacing = minSpacing,
                showDayLabels = false
            )
            val mainSpacingPx = with(density) { mainLayout.horizontalSpacing.roundToPx() }
            val mainTotalWidthPx = mainLayout.numWeeksOnScreen * cellSizePx + (mainLayout.numWeeksOnScreen - 1) * mainSpacingPx

            val settingsLayout = calculateHeatmapLayout(
                totalAvailableWidthPx = settingsAvailableWidthPx,
                density = density,
                cellSize = cellSize,
                minHorizontalSpacing = minSpacing,
                showDayLabels = false
            )
            val settingsSpacingPx = with(density) { settingsLayout.horizontalSpacing.roundToPx() }
            val settingsTotalWidthPx = settingsLayout.numWeeksOnScreen * cellSizePx + (settingsLayout.numWeeksOnScreen - 1) * settingsSpacingPx

            assertTrue(
                "Main screen must not overflow: mainTotalWidthPx=$mainTotalWidthPx, mainAvailableWidthPx=$mainAvailableWidthPx",
                mainTotalWidthPx <= mainAvailableWidthPx
            )
            assertTrue(
                "Settings preview must not overflow: settingsTotalWidthPx=$settingsTotalWidthPx, settingsAvailableWidthPx=$settingsAvailableWidthPx",
                settingsTotalWidthPx <= settingsAvailableWidthPx
            )
        }
    }

    @Test
    fun testColumnAmountsMatchesMaximumFit() {
        val density = Density(density = 2.625f, fontScale = 1.0f)
        val cellSize = 10.dp
        val minSpacing = 4.dp
        val cellSizePx = with(density) { cellSize.roundToPx() }
        val minSpacingPx = with(density) { minSpacing.roundToPx() }

        for (availableWidthPx in listOf(500, 750, 964, 1080, 1440)) {
            val layout = calculateHeatmapLayout(
                totalAvailableWidthPx = availableWidthPx,
                density = density,
                cellSize = cellSize,
                minHorizontalSpacing = minSpacing,
                showDayLabels = false
            )

            val expectedSlots = floor((availableWidthPx + minSpacingPx).toFloat() / (cellSizePx + minSpacingPx)).toInt()
            assertEquals("Total slots must match expected maximum fit", expectedSlots, layout.totalSlots)
            assertEquals("When day labels hidden, numWeeksOnScreen must equal totalSlots", expectedSlots, layout.numWeeksOnScreen)

            // Verify that totalSlots + 1 would NOT fit even with minimum spacing
            val overflowWithOneMore = (expectedSlots + 1) * cellSizePx + expectedSlots * minSpacingPx
            assertTrue(
                "Adding one more slot must exceed available width ($overflowWithOneMore > $availableWidthPx)",
                overflowWithOneMore > availableWidthPx
            )
        }
    }

    @Test
    fun testHorizontalSpacingIsProperlyDistributed() {
        val density = Density(density = 3.0f, fontScale = 1.0f)
        val cellSize = 10.dp
        val minSpacing = 4.dp
        val cellSizePx = with(density) { cellSize.roundToPx() }

        val availableWidthPx = 948 // 360dp - 44dp = 316dp @ 3.0f = 948px
        val layout = calculateHeatmapLayout(
            totalAvailableWidthPx = availableWidthPx,
            density = density,
            cellSize = cellSize,
            minHorizontalSpacing = minSpacing,
            showDayLabels = false
        )

        val totalSlots = layout.totalSlots
        val remainingSpacePx = availableWidthPx - (totalSlots * cellSizePx)
        val expectedSpacingPx = remainingSpacePx / (totalSlots - 1)
        val actualSpacingPx = with(density) { layout.horizontalSpacing.roundToPx() }

        assertEquals(
            "Spacing in pixels must equal integer division of remaining space by gaps count",
            expectedSpacingPx,
            actualSpacingPx
        )
    }

    @Test
    fun testDayLabelsWidthAndAlignment() {
        val density = Density(density = 2.625f, fontScale = 1.0f)
        val cellSize = 10.dp
        val minSpacing = 4.dp

        val availableWidthPx = 964
        val layoutWithoutDays = calculateHeatmapLayout(
            totalAvailableWidthPx = availableWidthPx,
            density = density,
            cellSize = cellSize,
            minHorizontalSpacing = minSpacing,
            showDayLabels = false
        )

        val layoutWithDays = calculateHeatmapLayout(
            totalAvailableWidthPx = availableWidthPx,
            density = density,
            cellSize = cellSize,
            minHorizontalSpacing = minSpacing,
            showDayLabels = true
        )

        assertEquals("Total slots must be identical whether day labels are shown or not", layoutWithoutDays.totalSlots, layoutWithDays.totalSlots)
        assertEquals("Horizontal spacing must be identical whether day labels are shown or not", layoutWithoutDays.horizontalSpacing, layoutWithDays.horizontalSpacing)
        assertEquals("numWeeksOnScreen must be reduced by 1 when day labels are shown", layoutWithoutDays.numWeeksOnScreen - 1, layoutWithDays.numWeeksOnScreen)
    }

    @Test
    fun testSingleSlotFallback() {
        val density = Density(density = 2.0f, fontScale = 1.0f)
        val cellSize = 10.dp
        val minSpacing = 4.dp
        val cellSizePx = with(density) { cellSize.roundToPx() }

        // Width only enough for 1 cell
        val availableWidthPx = cellSizePx + 2
        val layout = calculateHeatmapLayout(
            totalAvailableWidthPx = availableWidthPx,
            density = density,
            cellSize = cellSize,
            minHorizontalSpacing = minSpacing,
            showDayLabels = false
        )

        assertEquals("Must coerce to at least 1 slot", 1, layout.totalSlots)
        assertEquals("numWeeksOnScreen must be 1", 1, layout.numWeeksOnScreen)
        assertEquals("Must fall back to minHorizontalSpacing when totalSlots is 1", minSpacing, layout.horizontalSpacing)
    }

    @Test
    fun testMonthLabelShiftedFromEdges() {
        val firstDayOfWeek = Calendar.MONDAY
        val monthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)

        // Week 0 starts on Monday, June 1, 2026 (contains 1st of June -> right edge column 0)
        val cal = Calendar.getInstance().apply {
            this.firstDayOfWeek = firstDayOfWeek
            set(2026, Calendar.JUNE, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentFirstDayOfWeekMillis = cal.timeInMillis

        fun weekStart(index: Int): Long {
            return Calendar.getInstance().apply {
                this.firstDayOfWeek = firstDayOfWeek
                timeInMillis = currentFirstDayOfWeekMillis
                add(Calendar.WEEK_OF_YEAR, -index)
            }.timeInMillis
        }

        // Suppose totalWeeks = 6, so weekIndex = 5 is 5 weeks earlier: Monday, April 27, 2026 (contains May 1 -> left edge column 5)
        val totalWeeks = 6

        // Right edge (weekIndex 0) must be null, shifted to weekIndex 1 ("Jun")
        assertNull(
            resolveHeatmapMonthLabel(0, totalWeeks, weekStart(0), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
        assertEquals(
            "Jun",
            resolveHeatmapMonthLabel(1, totalWeeks, weekStart(1), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )

        // Left edge (weekIndex 5) must be null, shifted to weekIndex 4 ("May")
        assertNull(
            resolveHeatmapMonthLabel(5, totalWeeks, weekStart(5), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
        assertEquals(
            "May",
            resolveHeatmapMonthLabel(4, totalWeeks, weekStart(4), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )

        // When totalWeeks is larger (e.g. infinite / larger week limit = 10, where weekIndex 9 is Monday, March 30, 2026 -> contains April 1),
        // weekIndex 5 is now an interior column and keeps "May", while the new left edge (weekIndex 9) shifts "Apr" to weekIndex 8.
        val expandedTotalWeeks = 10
        assertEquals(
            "May",
            resolveHeatmapMonthLabel(5, expandedTotalWeeks, weekStart(5), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
        assertNull(
            resolveHeatmapMonthLabel(4, expandedTotalWeeks, weekStart(4), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
        assertNull(
            resolveHeatmapMonthLabel(9, expandedTotalWeeks, weekStart(9), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
        assertEquals(
            "Apr",
            resolveHeatmapMonthLabel(8, expandedTotalWeeks, weekStart(8), currentFirstDayOfWeekMillis, firstDayOfWeek, monthFormat)
        )
    }
}
