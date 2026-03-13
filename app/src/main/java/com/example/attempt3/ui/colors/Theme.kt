/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.colors

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.attempt3.data.settings.SettingsDataStore

@Composable
fun Attempt3Theme(
    settingsDataStore: SettingsDataStore,
    content: @Composable () -> Unit
) {
    val theme by settingsDataStore.theme.collectAsState(initial = "system")
    val useMaterialTheming by settingsDataStore.useMaterialTheming.collectAsState(initial = true)
    
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val context = LocalContext.current

    val baseColorScheme = if (isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            darkColorScheme(
                primary = Color(0xFFBB86FC),
                onPrimary = Color(0xFF1C0B3C),
                secondary = Color(0xFF03DAC6),
                onSecondary = Color(0xFF002724),
                error = Color(0xFFCF6679),
                onError = Color.Black
            )
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            lightColorScheme(
                primary = Color(0xFF6200EE),
                onPrimary = Color.White,
                secondary = Color(0xFF03DAC6),
                onSecondary = Color.Black,
                error = Color(0xFFB00020),
                onError = Color.White
            )
        }
    }

    val colorScheme = if (!useMaterialTheming) {
        if (isDark) {
            baseColorScheme.copy(
                background = Color(0xFF111111),
                onBackground = Color(0xFFE7E7EA),
                surface = Color(0xFF111111),
                surfaceVariant = Color(0xFF232323),
                onSurface = Color(0xFFE7E7EA),
                onSurfaceVariant = Color(0xFFE7E7EA)
            )
        } else {
            baseColorScheme.copy(
                background = Color(0xFFFFFFFF),
                onBackground = Color(0xFF121216),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF9F9F9),
                onSurface = Color(0xFF121216),
                onSurfaceVariant = Color(0xFF121216)
            )
        }
    } else {
        baseColorScheme
    }

    val animationSpec = tween<Color>(durationMillis = 400)
    val animatedColorScheme = colorScheme.copy(
        primary = animateColorAsState(colorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(colorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(colorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(colorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(colorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(colorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(colorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(colorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(colorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(colorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(colorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(colorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(colorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(colorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(colorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(colorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(colorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(colorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        error = animateColorAsState(colorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(colorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(colorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(colorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(colorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(colorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(colorScheme.scrim, animationSpec, label = "scrim").value
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
