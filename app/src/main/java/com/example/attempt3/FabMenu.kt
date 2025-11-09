package com.example.attempt3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun FabMenu(
    isArchivedScreen: Boolean,
    onAddNewHabit: () -> Unit,
    onShowArchived: () -> Unit,
    onShowHome: () -> Unit,
    onShowSettings: () -> Unit,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val transition = updateTransition(targetState = isExpanded, label = "FabMenuTransition")

    val rotation by transition.animateFloat(
        label = "Rotation",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { isExpandedState ->
        if (isExpandedState) 180f else 0f
    }
    val cornerRadius by transition.animateDp(
        label = "CornerRadius",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { isExpandedState ->
        if (isExpandedState) 28.dp else 16.dp
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (isArchivedScreen) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) +
                            expandHorizontally(
                                animationSpec = tween(300, delayMillis = 150),
                                expandFrom = Alignment.End
                            ),
                    exit = fadeOut(animationSpec = tween(300)) +
                           shrinkHorizontally(
                               animationSpec = tween(300),
                               shrinkTowards = Alignment.End
                           )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onShowHome()
                            onToggle(false)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        text = { Text("Home") },
                        shape = CircleShape,
                        modifier = Modifier.shadow(4.dp, CircleShape),
                        expanded = true,
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) +
                            expandHorizontally(
                                animationSpec = tween(300, delayMillis = 150),
                                expandFrom = Alignment.End
                            ),
                    exit = fadeOut(animationSpec = tween(300)) +
                           shrinkHorizontally(
                               animationSpec = tween(300),
                               shrinkTowards = Alignment.End
                           )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onShowSettings()
                            onToggle(false)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        text = { Text("Settings") },
                        shape = CircleShape,
                        modifier = Modifier.shadow(4.dp, CircleShape),
                        expanded = true,
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                            expandHorizontally(
                                animationSpec = tween(300, delayMillis = 100),
                                expandFrom = Alignment.End
                            ),
                    exit = fadeOut(animationSpec = tween(300, delayMillis = 50)) +
                           shrinkHorizontally(
                               animationSpec = tween(300, delayMillis = 50),
                               shrinkTowards = Alignment.End
                           )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onShowArchived()
                            onToggle(false)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.Archive, contentDescription = "Archived") },
                        text = { Text("Archived") },
                        shape = CircleShape,
                        modifier = Modifier.shadow(4.dp, CircleShape),
                        expanded = true,
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 50)) +
                            expandHorizontally(
                                animationSpec = tween(300, delayMillis = 50),
                                expandFrom = Alignment.End
                            ),
                    exit = fadeOut(animationSpec = tween(300, delayMillis = 100)) +
                           shrinkHorizontally(
                               animationSpec = tween(300, delayMillis = 100),
                               shrinkTowards = Alignment.End
                           )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onAddNewHabit()
                            onToggle(false)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.Add, contentDescription = "New Habit") },
                        text = { Text("New Habit") },
                        shape = CircleShape,
                        modifier = Modifier.shadow(4.dp, CircleShape),
                        expanded = true,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onToggle(!isExpanded) },
            shape = RoundedCornerShape(cornerRadius),
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(cornerRadius))
        ) {
            Crossfade(
                targetState = isExpanded,
                animationSpec = tween(500),
                label = "FabIconCrossfade"
            ) { expanded ->
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Toggle FAB Menu",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}