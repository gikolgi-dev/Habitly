/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.habitly.habitly.ui.components.DayOfWeekSelector
import kotlinx.coroutines.launch
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.Database.isDayCompleted
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.ui.HabitItemCard
import com.habitly.habitly.ui.colors.habitColors
import com.habitly.habitly.ui.defaultHabitIconKey
import java.util.Calendar

@Composable
fun HeatmapSubScreen(
    settingsDataStore: SettingsDataStore,
    db: HabitDatabase? = null,
    onNavigateToHeatmapNotificationDot: () -> Unit,
    onNavigateToHabitColor: () -> Unit,
    modifier: Modifier = Modifier,
    showMonthLabels: Boolean = DefaultSettings.MONTH_LABELS,
    showYearDivider: Boolean = DefaultSettings.YEAR_DIVIDER,
    showYearLabels: Boolean = DefaultSettings.YEAR_LABELS,
    heatmapNotificationDot: Boolean = DefaultSettings.HEATMAP_NOTIFICATION_DOT,
    heatmapNotificationDotRange: String = DefaultSettings.HEATMAP_NOTIFICATION_DOT_RANGE,
    heatmapScrolling: Boolean = DefaultSettings.HEATMAP_SCROLLING,
    heatmapVisibleDays: Set<String> = emptySet(),
    dayOfWeekLabelsOnRight: Boolean = false,
    borderContrast: Float = DefaultSettings.BORDERS,
    vibrationsEnabled: Boolean = DefaultSettings.VIBRATIONS,
    firstDayOfWeekCalendar: Int = Calendar.MONDAY,
    heatmapWeeks: Int = DefaultSettings.HEATMAP_WEEKS,
    heatmapInfinite: Boolean = DefaultSettings.HEATMAP_INFINITE,
    useHabitColorForCard: Boolean = DefaultSettings.USE_HABIT_COLOR_FOR_CARD,
    habitColorTargets: Set<String> = DefaultSettings.HABIT_COLOR_TARGETS.split(',').filter { it.isNotEmpty() }.toSet(),
    reduceMovement: Boolean = DefaultSettings.REDUCE_MOVEMENT,
    reduceMovementTargets: Set<String> = DefaultSettings.REDUCE_MOVEMENT_TARGETS.split(',').filter { it.isNotEmpty() }.toSet(),
    autoScrollText: Boolean = DefaultSettings.AUTO_SCROLL_TEXT,
    autoScrollTextElements: Set<String> = DefaultSettings.AUTO_SCROLL_TEXT_ELEMENTS.split(',').filter { it.isNotEmpty() }.toSet(),
    autoScrollTextScreens: Set<String> = DefaultSettings.AUTO_SCROLL_TEXT_SCREENS.split(',').filter { it.isNotEmpty() }.toSet(),
    theme: String = DefaultSettings.THEME
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val useHabitColorForItemCards = useHabitColorForCard && "Habit Cards" in habitColorTargets
    val disableAnimations = reduceMovement && "Rotation" in reduceMovementTargets

    // Imagined date: Wednesday, February 11, 2026.
    // This places the 2025/2026 year boundary 6 weeks back, clearly visible in the initial preview,
    // and places "today" on a weekday (Wednesday) so all notification indicator ranges (future, today_and_future, this_week)
    // are distinct and fully demonstrable.
    val imaginedCurrentDateMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 11) // Wednesday
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val imaginedHabit = remember(imaginedCurrentDateMillis) {
        val startMillis = imaginedCurrentDateMillis - (210L * 24 * 3600 * 1000)
        Habit(
            id = "imagined_preview_habit",
            name = "Morning Meditation",
            description = "15 minutes of mindfulness",
            color = habitColors.first().toArgb(),
            icon = defaultHabitIconKey,
            orderIndex = 0,
            createdAt = startMillis.toString(),
            isInverse = false,
            startDate = startMillis.toString(),
            archived = false,
            emoji = null,
            completionsPerInterval = 1,
            intervalUnit = "day",
            notificationsEnabled = true,
            notificationTime = "08:00",
            notificationDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
            completionsPerDay = 1,
            streakCountingDisabled = false
        )
    }

    val imaginedCompletions = remember(imaginedHabit, imaginedCurrentDateMillis) {
        val list = mutableListOf<Completion>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = imaginedCurrentDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.add(Calendar.DAY_OF_YEAR, -210)
        val random = java.util.Random(1337)

        for (i in 0..210) {
            val dayMillis = cal.timeInMillis
            val isToday = (i == 210)
            val isCompleted = if (isToday) true else random.nextFloat() < 0.72f
            if (isCompleted) {
                list.add(
                    Completion(
                        id = "preview_comp_$i",
                        habitId = imaginedHabit.id,
                        date = dayMillis,
                        timezoneOffsetInMinutes = 0,
                        amountOfCompletions = 1
                    )
                )
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val isCompleted = remember(imaginedHabit, imaginedCompletions, imaginedCurrentDateMillis) {
        isDayCompleted(imaginedHabit, imaginedCompletions, imaginedCurrentDateMillis, imaginedCurrentDateMillis)
    }

    val (scrollState, scrollEnabled) = rememberSettingsScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val dividerAlpha by animateFloatAsState(targetValue = if (isScrolled) 1f else 0f, label = "dividerAlpha")

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            HabitItemCard(
                habit = imaginedHabit,
                isCompleted = isCompleted,
                completions = imaginedCompletions,
                showCheckbox = true,
                showMonthLabels = showMonthLabels,
                visibleDayLabels = heatmapVisibleDays,
                dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                showYearDivider = showYearDivider,
                showYearLabels = showYearLabels,
                heatmapNotificationDot = heatmapNotificationDot,
                heatmapNotificationDotRange = heatmapNotificationDotRange,
                showScrollBlur = false,
                borderContrast = borderContrast,
                heatmapScrollEnabled = heatmapScrolling,
                heatmapWeeks = heatmapWeeks,
                heatmapInfinite = heatmapInfinite,
                useHabitColor = useHabitColorForItemCards,
                disableAnimations = disableAnimations,
                isPreview = true,
                onComplete = { },
                onClick = { },
                sharedTransitionScope = null,
                visible = true,
                detailBgColor = Color(imaginedHabit.color).copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
                currentDateMillis = imaginedCurrentDateMillis,
                animateTileChanges = true,
                firstDayOfWeek = firstDayOfWeekCalendar,
                autoScrollText = autoScrollText,
                autoScrollTextElements = autoScrollTextElements,
                autoScrollTextScreens = autoScrollTextScreens,
                theme = theme
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .alpha(dividerAlpha),
            color = Color.Gray.copy(alpha = 0.35f)
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(scrollState, enabled = scrollEnabled)
        ) {
            SettingsGroup(settingsDataStore = settingsDataStore) {
                SettingsSwitchNavigationItem(
                    text = "Habit color accents",
                    description = "Add color accents to habit related elements",
                    checked = useHabitColorForCard,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Top,
                    onCheckedChange = {
                        scope.launch { settingsDataStore.setUseHabitColorForCard(it) }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        }
                    },
                    onClick = onNavigateToHabitColor
                )
                SettingsSwitchItem(
                    text = "Month labels",
                    description = "Show the names of months above the heatmap",
                    checked = showMonthLabels,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle
                ) {
                    scope.launch { settingsDataStore.setMonthLabels(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }

                SettingsSwitchItem(
                    text = "Year divider",
                    description = "Add a visual gap between different years",
                    checked = showYearDivider,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle
                ) {
                    scope.launch { settingsDataStore.setYearDivider(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }

                SettingsSwitchItem(
                    text = "Year labels",
                    description = "Display the year next to the heatmap",
                    checked = showYearLabels,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle
                ) {
                    scope.launch { settingsDataStore.setYearLabels(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }

                SettingsSwitchNavigationItem(
                    text = "Notification indicator",
                    description = "Show a dot on days with scheduled notifications",
                    checked = heatmapNotificationDot,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle,
                    onCheckedChange = {
                        scope.launch { settingsDataStore.setHeatmapNotificationDot(it) }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        }
                    },
                    onClick = onNavigateToHeatmapNotificationDot
                )

                SettingsSwitchItem(
                    text = "Heatmap scrolling",
                    description = "Allow scrolling the heatmap on the main screen",
                    checked = heatmapScrolling,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle
                ) {
                    scope.launch { settingsDataStore.setHeatmapScrolling(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }

                SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Bottom) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 16.dp
                        )
                    ) {
                        Text(
                            text = "Visible day labels",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toggle which days are shown on the heatmap's side labels.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        DayOfWeekSelector(
                            selectedDays = heatmapVisibleDays,
                            onDaySelected = { day ->
                                val newDays = if (heatmapVisibleDays.contains(day)) heatmapVisibleDays - day else heatmapVisibleDays + day
                                scope.launch { settingsDataStore.setHeatmapVisibleDays(newDays) }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            },
                            firstDayOfWeek = firstDayOfWeekCalendar,
                            borderAlpha = borderContrast
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}
