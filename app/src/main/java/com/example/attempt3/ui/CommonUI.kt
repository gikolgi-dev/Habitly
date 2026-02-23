package com.example.attempt3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore

@Composable
fun AppBackButton(
    onBack: () -> Unit,
    settingsDataStore: SettingsDataStore,
    isRoot: Boolean = false,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0f)
    
    Surface(
        onClick = onBack,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
        ),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = if (isRoot) "Close" else "Back",
                modifier = Modifier.size(24.dp),
                tint = tint
            )
        }
    }
}
