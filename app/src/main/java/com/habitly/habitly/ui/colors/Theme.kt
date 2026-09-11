/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.habitly.habitly.ui.colors

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.delay

@Composable
fun Attempt3Theme(
    settingsDataStore: SettingsDataStore,
    initialTheme: String = "system",
    initialUseMaterialTheming: Boolean = true,
    content: @Composable () -> Unit
) {
    val theme by settingsDataStore.theme.collectAsState(initial = initialTheme)
    val useMaterialTheming by settingsDataStore.useMaterialTheming.collectAsState(initial = initialUseMaterialTheming)

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
                // Primary Roles
                primary = Color(0xFFBB86FC),
                onPrimary = Color(0xFF1C0B3C),
                primaryContainer = Color(0xFF4700AB),
                onPrimaryContainer = Color(0xFFEADDFF),
                inversePrimary = Color(0xFF6200EE),

                // Secondary Roles
                secondary = Color(0xFF80D5C9),
                onSecondary = Color(0xFF003732),
                secondaryContainer = Color(0xFF264E48),
                onSecondaryContainer = Color(0xFFA6F2E5),

                // Tertiary Roles
                tertiary = Color(0xFFEFB8C8),
                onTertiary = Color(0xFF492532),
                tertiaryContainer = Color(0xFF633B48),
                onTertiaryContainer = Color(0xFFFFD8E4),

                // Error Roles
                error = Color(0xFFCF6679),
                onError = Color(0xFF1E0004),
                errorContainer = Color(0xFF8C1D18),
                onErrorContainer = Color(0xFFF9DEDC),

                // Background & Surface
                background = Color(0xFF121212),
                onBackground = Color(0xFFE6E1E5),
                surface = Color(0xFF121212),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF49454F),
                onSurfaceVariant = Color(0xFFCAC4D0),
                surfaceTint = Color(0xFFBB86FC),
                inverseSurface = Color(0xFFE6E1E5),
                inverseOnSurface = Color(0xFF313033),

                // Outlines & Scrim
                outline = Color(0xFF938F99),
                outlineVariant = Color(0xFF49454F),
                scrim = Color(0xFF000000)
            )
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            lightColorScheme(
                // Primary Roles
                primary = Color(0xFF6200EE),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                inversePrimary = Color(0xFFD0BCFF),

                // Secondary Roles
                secondary = Color(0xFF3B6661),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFCEE8E2),
                onSecondaryContainer = Color(0xFF05201D),

                // Tertiary Roles (Warm coral accent)
                tertiary = Color(0xFF7D5260),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFD8E4),
                onTertiaryContainer = Color(0xFF31111D),

                // Error Roles
                error = Color(0xFFB00020),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFF9DEDC),
                onErrorContainer = Color(0xFF410E0B),

                // Background & Surface
                background = Color(0xFFFFFBFE),
                onBackground = Color(0xFF1C1B1F),
                surface = Color(0xFFFFFBFE),
                onSurface = Color(0xFF1C1B1F),
                surfaceVariant = Color(0xFFE7E0EC),
                onSurfaceVariant = Color(0xFF49454F),
                surfaceTint = Color(0xFF6200EE),
                inverseSurface = Color(0xFF313033),
                inverseOnSurface = Color(0xFFF4EFF4),

                // Outlines & Scrim
                outline = Color(0xFF79747E),
                outlineVariant = Color(0xFFCAC4D0),
                scrim = Color(0xFF000000)
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
                surfaceVariant = Color(0xFFdcdcdc),
                onSurface = Color(0xFF121216),
                onSurfaceVariant = Color(0xFF121216)
            )
        }
    } else {
        baseColorScheme
    }

    var enableAnimations by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Delay enabling animations until after DataStore has loaded initial values
        // and initial layout passes are done.
        delay(500)
        enableAnimations = true
    }

    val animationSpec = if (enableAnimations) tween<Color>(durationMillis = 400) else snap<Color>()

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
