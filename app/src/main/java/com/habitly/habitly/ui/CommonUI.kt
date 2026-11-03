/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.habitly.habitly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AppBackButton(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    borderContrast: Float,
    isRoot: Boolean = false,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    OutlinedIconButton(
        onClick = onBack,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = backgroundColor,
            contentColor = tint
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
        ),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(IconButtonDefaults.smallContainerSize())
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isRoot) "Close" else "Back",
            modifier = Modifier.size(IconButtonDefaults.smallIconSize)
        )
    }
}