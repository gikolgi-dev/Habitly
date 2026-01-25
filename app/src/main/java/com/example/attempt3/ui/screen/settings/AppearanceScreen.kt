@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.DayLabelDisplayOptions
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(modifier: Modifier = Modifier, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = "system")
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = true)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = true)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = true)
    val showDayLabels by settingsDataStore.dayOfWeekLabelsVisible.collectAsState(initial = true)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val showAllDayOfWeekLabels by settingsDataStore.showAllDayOfWeekLabels.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0f)
    val appearanceTint by settingsDataStore.appearanceTint.collectAsState(initial = 0.1f)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val showTintDialogPref by settingsDataStore.showTintDialog.collectAsState(initial = true)
    
    var showTintDialogState by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var pendingTintValue by remember { mutableFloatStateOf(0f) }

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val tintInteractionSource = remember { MutableInteractionSource() }
    val isTintDragged by tintInteractionSource.collectIsDraggedAsState()

    if (showTintDialogState) {
        BasicAlertDialog(
            onDismissRequest = { }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Background Tint",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "This setting looks best under specific combinations of Material theme and habit colors. You can adjust it to your preference, even if it might look unconventional.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineBreak = LineBreak.Paragraph,
                            hyphens = Hyphens.Auto
                        ),
                        textAlign = TextAlign.Justify
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = dontShowAgain,
                                onClick = { dontShowAgain = !dontShowAgain }
                            )
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            modifier = Modifier.offset(x = (-12).dp)
                        )
                        Text(
                            text = "Don't show again",
                            modifier = Modifier.offset(x = (-12).dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsDataStore.setAppearanceTint(pendingTintValue)
                                    if (dontShowAgain) {
                                        settingsDataStore.setShowTintDialog(false)
                                    }
                                }
                            },
                            shape = CircleShape
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .combinedClickable(
                        onLongClick = {
                            scope.launch {
                                // Secretly "unselect" Don't show again
                                settingsDataStore.setShowTintDialog(true)
                            }
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            }
                        },
                        onClick = {}
                    )
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = modifier) {
                    val themes = listOf("Light", "Dark", "System")
                    themes.forEach { theme ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (theme.lowercase() == currentTheme),
                                    onClick = {
                                        scope.launch {
                                            settingsDataStore.setTheme(theme.lowercase())
                                        }
                                        if (vibrationsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (theme.lowercase() == currentTheme),
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setTheme(theme.lowercase())
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    }
                                }
                            )
                            Text(
                                text = theme,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Set background tint",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = appearanceTint,
                            onValueChange = { newValue ->
                                // Show dialog if it's currently 0 and we are moving away from 0,
                                // AND we haven't acknowledged it yet.
                                val isFirstTinting = (appearanceTint == 0f && newValue > 0f)
                                if (isFirstTinting && showTintDialogPref) {
                                    pendingTintValue = newValue
                                } else {
                                    scope.launch {
                                        settingsDataStore.setAppearanceTint(newValue)
                                    }
                                }
                            },
                            valueRange = 0f..1f,
                            steps = 19,
                            interactionSource = tintInteractionSource,
                            colors = SliderDefaults.colors(
                                activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            thumb = {
                                Layout(
                                    content = {
                                        if (isTintDragged) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary,

                                                ) {
                                                Text(
                                                    text = "%.2f".format(appearanceTint),
                                                    modifier = Modifier.padding(4.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        SliderDefaults.Thumb(
                                            interactionSource = tintInteractionSource,
                                            colors = SliderDefaults.colors(),
                                            enabled = true
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val thumbPlaceable = measurables.last().measure(constraints)
                                    val indicatorPlaceable = if (isTintDragged) {
                                        measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                                    } else {
                                        null
                                    }

                                    layout(thumbPlaceable.width, thumbPlaceable.height) {
                                        thumbPlaceable.placeRelative(0, 0)
                                        indicatorPlaceable?.let {
                                            val indicatorY = (thumbPlaceable.height - it.height) / 2
                                            val indicatorX = if (appearanceTint > 0.5f) {
                                                -it.width - 8.dp.roundToPx()
                                            } else {
                                                thumbPlaceable.width + 8.dp.roundToPx()
                                            }
                                            it.placeRelative(indicatorX, indicatorY)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Heatmap",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = showMonthLabels,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setMonthLabels(!showMonthLabels)
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(if (!showMonthLabels) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showMonthLabels,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setMonthLabels(it)
                                }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                        Text(
                            text = "Toggle month labels",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = showYearDivider,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setYearDivider(!showYearDivider)
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(if (!showYearDivider) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showYearDivider,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setYearDivider(it)
                                }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                        Text(
                            text = "Toggle year divider",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = showYearLabels,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setYearLabels(!showYearLabels)
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(if (!showYearLabels) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showYearLabels,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setYearLabels(it)
                                }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                        Text(
                            text = "Toggle year labels",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = showScrollBlur,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setshowScrollBlur(!showScrollBlur)
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(if (!showScrollBlur) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showScrollBlur,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setshowScrollBlur(it)
                                }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                        Text(
                            text = "Toggle scroll blur",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    val dayLabelDisplay = when {
                        !showDayLabels -> DayLabelDisplayOptions.Off
                        !showAllDayOfWeekLabels -> DayLabelDisplayOptions.Some
                        else -> DayLabelDisplayOptions.All
                    }
                    val options = DayLabelDisplayOptions.entries.map { it.name }

                    Column(modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .fillMaxWidth()){
                        Text(
                            text = "Day label display:",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Left
                        )
                        TabRow(
                            selectedTabIndex = dayLabelDisplay.ordinal,
                            modifier = modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            indicator = { tabPositions ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[dayLabelDisplay.ordinal])
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .zIndex(-1f)
                                )
                            },
                            divider = {}
                        ) {
                            options.forEachIndexed { index, text ->
                                val selected = dayLabelDisplay.ordinal == index
                                Tab(
                                    selected = selected,
                                    onClick = {
                                        scope.launch {
                                            when (DayLabelDisplayOptions.entries[index]) {
                                                DayLabelDisplayOptions.Off -> {
                                                    settingsDataStore.setDayOfWeekLabelsVisible(
                                                        false
                                                    )
                                                }

                                                DayLabelDisplayOptions.Some -> {
                                                    settingsDataStore.setDayOfWeekLabelsVisible(true)
                                                    settingsDataStore.setShowAllDayOfWeekLabels(
                                                        false
                                                    )
                                                }

                                                DayLabelDisplayOptions.All -> {
                                                    settingsDataStore.setDayOfWeekLabelsVisible(true)
                                                    settingsDataStore.setShowAllDayOfWeekLabels(true)
                                                }
                                            }
                                        }
                                    },
                                    text = { Text(text = text) },
                                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ){
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Set border contrast",
                        style = MaterialTheme.typography.bodyLarge

                    )
                    Slider(
                        value = borderContrast,
                        onValueChange = {
                            scope.launch {
                                settingsDataStore.setBorders(it)
                            }
                        },
                        valueRange = 0f..1f,
                        steps = 19,
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(
                            activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                            inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        thumb = {
                            Layout(
                                content = {
                                    if (isDragged) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary,

                                            ) {
                                            Text(
                                                text = "%.2f".format(borderContrast),
                                                modifier = Modifier.padding(4.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    SliderDefaults.Thumb(
                                        interactionSource = interactionSource,
                                        colors = SliderDefaults.colors(),
                                        enabled = true
                                    )
                                }
                            ) { measurables, constraints ->
                                val thumbPlaceable = measurables.last().measure(constraints)
                                val indicatorPlaceable = if (isDragged) {
                                    measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                                    } else {
                                        null
                                    }

                                layout(thumbPlaceable.width, thumbPlaceable.height) {
                                    thumbPlaceable.placeRelative(0, 0)
                                    indicatorPlaceable?.let {
                                        val indicatorY = (thumbPlaceable.height - it.height) / 2
                                        val indicatorX = if (borderContrast > 0.5f) {
                                            -it.width - 8.dp.roundToPx()
                                        } else {
                                            thumbPlaceable.width + 8.dp.roundToPx()
                                        }
                                        it.placeRelative(indicatorX, indicatorY)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
