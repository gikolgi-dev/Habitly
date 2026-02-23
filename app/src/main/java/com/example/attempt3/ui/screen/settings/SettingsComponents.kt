@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore

enum class SettingsItemPosition {
    Top, Middle, Bottom, Alone
}

@Composable
fun getSettingsItemShape(position: SettingsItemPosition): Shape {
    val large = 24.dp
    val small = 6.dp
    return when (position) {
        SettingsItemPosition.Top -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        SettingsItemPosition.Middle -> RoundedCornerShape(small)
        SettingsItemPosition.Bottom -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        SettingsItemPosition.Alone -> RoundedCornerShape(large)
    }
}

@Composable
fun SettingsItemBox(
    settingsDataStore: SettingsDataStore,
    position: SettingsItemPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0f)
    val shape = getSettingsItemShape(position)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = borderContrast),
                shape
            )
    ) {
        content()
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    settingsDataStore: SettingsDataStore,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchItemContent(
    text: String,
    checked: Boolean,
    description: String? = null,
    showDivider: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = checked,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    text: String,
    checked: Boolean,
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier,
    description: String? = null,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    showDivider: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
        SettingsItemBox(settingsDataStore = settingsDataStore, position = position, modifier = modifier) {
            SettingsSwitchItemContent(
                text = text,
                checked = checked,
                description = description,
                showDivider = showDivider,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsSwitchNavigationItem(
    text: String,
    checked: Boolean,
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier,
    description: String? = null,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
        SettingsItemBox(settingsDataStore = settingsDataStore, position = position, modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 16.dp)
                        .width(0.75.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Box(
                    modifier = Modifier
                        .clickable { onCheckedChange(!checked) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCheckboxItemContent(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = checked,
                    enabled = enabled,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
                modifier = Modifier.padding(start = 6.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsCheckboxItem(
    text: String,
    checked: Boolean,
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    showDivider: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 46.dp) {
        SettingsItemBox(settingsDataStore = settingsDataStore, position = position, modifier = modifier) {
            SettingsCheckboxItemContent(
                text = text,
                checked = checked,
                enabled = enabled,
                showDivider = showDivider,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun RotatingCookie(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color,
    settingsDataStore: SettingsDataStore,
    size: Dp = 46.dp,
    iconSize: Dp = 30.dp,
    contentDescription: String? = null
) {
    val disableAnimations by settingsDataStore.disableAnimations.collectAsState(initial = false)
    
    val rotation = if (!disableAnimations) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val animatedRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        animatedRotation
    } else {
        0f
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .background(iconBackgroundColor, MaterialShapes.Cookie12Sided.toShape())
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GroupedSettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color,
    settingsDataStore: SettingsDataStore,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    SettingsItemBox(settingsDataStore = settingsDataStore, position = position) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RotatingCookie(
                    icon = icon,
                    iconBackgroundColor = iconBackgroundColor,
                    iconColor = iconColor,
                    settingsDataStore = settingsDataStore,
                    contentDescription = title
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}


@Composable
fun ModernSettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color,
    settingsDataStore: SettingsDataStore,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    onClick: () -> Unit
) {
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val shape = getSettingsItemShape(position)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
        ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RotatingCookie(
                icon = icon,
                iconBackgroundColor = iconBackgroundColor,
                iconColor = iconColor,
                settingsDataStore = settingsDataStore,
                contentDescription = title
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onSelectionChange(index) },
                selected = index == selectedIndex,
                label = { Text(label) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun MainSettingsToggle(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(64.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
