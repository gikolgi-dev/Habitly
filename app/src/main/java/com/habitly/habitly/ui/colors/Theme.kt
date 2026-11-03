/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.colors

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
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
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.data.settings.LocalAppSettings
import com.habitly.habitly.data.settings.LocalSettingsDataStore

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Attempt3Theme(
    settingsDataStore: SettingsDataStore = LocalSettingsDataStore.current,
    initialTheme: String = "system",
    initialUseMaterialTheming: Boolean = true,
    content: @Composable () -> Unit
) {
    val settings = LocalAppSettings.current
    val theme = settings.theme
    val useMaterialTheming = settings.useMaterialTheming
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
                scrim = Color(0xFF000000),
                surfaceContainerLowest = Color(0xFF0C0C0C),
                surfaceContainerLow = Color(0xFF171717),
                surfaceContainer = Color(0xFF1E1E1E),
                surfaceContainerHigh = Color(0xFF282828),
                surfaceContainerHighest = Color(0xFF333333)
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
                scrim = Color(0xFF000000),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF7F2FA),
                surfaceContainer = Color(0xFFF3EDF7),
                surfaceContainerHigh = Color(0xFFECE6F0),
                surfaceContainerHighest = Color(0xFFE6E0E9)
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
                onSurfaceVariant = Color(0xFFE7E7EA),
                surfaceContainerLowest = Color(0xFF0A0A0A),
                surfaceContainerLow = Color(0xFF171717),
                surfaceContainer = Color(0xFF1E1E1E),
                surfaceContainerHigh = Color(0xFF232323),
                surfaceContainerHighest = Color(0xFF2E2E2E)
            )
        } else {
            baseColorScheme.copy(
                background = Color(0xFFEEEEEE),
                onBackground = Color(0xFF181815),
                surface = Color(0xFFEEEEEE),
                surfaceVariant = Color(0xFFDCDCDC),
                onSurface = Color(0xFF181815),
                onSurfaceVariant = Color(0xFF181815),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFE8E8E8),
                surfaceContainer = Color(0xFFE2E2E2),
                surfaceContainerHigh = Color(0xFFDCDCDC),
                surfaceContainerHighest = Color(0xFFD4D4D4)
            )
        }
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
