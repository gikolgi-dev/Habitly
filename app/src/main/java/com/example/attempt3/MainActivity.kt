package com.example.attempt3

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.room.Room

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(applicationContext, HabitDatabase::class.java, "habits.db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val viewModel by viewModels<HabitViewModel> { HabitViewModelFactory(db.habitDao()) }

    private val settingsDataStore by lazy {
        SettingsDataStore(applicationContext)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isSystemInDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            navigationBarStyle = if (isSystemInDarkTheme) {
                SystemBarStyle.dark(
                    Color.Transparent.toArgb()
                )
            } else {
                SystemBarStyle.light(
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb()
                )
            }
        )
        setContent {
            ExpressiveDarkApp(settingsDataStore = settingsDataStore) {
                ExpressiveMainScreen(viewModel = viewModel, habitDao = db.habitDao(), db = db, settingsDataStore = settingsDataStore)
            }
        }
    }
}

@Composable
fun ExpressiveDarkApp(
    settingsDataStore: SettingsDataStore,
    content: @Composable () -> Unit
) {
    val theme by settingsDataStore.theme.collectAsState(initial = "system")
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val context = LocalContext.current

    val colorScheme = if (isDark) {
        val baseColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            darkColorScheme(
                primary = Color(0xFFBB86FC),
                onPrimary = Color(0xFF1C0B3C),
                secondary = Color(0xFF03DAC6),
                onSecondary = Color(0xFF002724),
                background = Color(0xFF0B0B0E),
                onBackground = Color(0xFFE7E7EA),
                surface = Color(0xFF121216),
                onSurface = Color(0xFFE7E7EA),
                error = Color(0xFFCF6679),
                onError = Color.Black
            )
        }
        baseColorScheme.copy(
            background = Color(0xFF111111),
            surface = Color(0xFF1C1B1B),
            onSurface = Color(0xFFE7E7EA),
            onBackground = Color(0xFFE7E7EA)
        )
    } else {
        val baseColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            darkColorScheme( // Fallback for older APIs
                primary = Color(0xFF6200EE),
                onPrimary = Color.White,
                secondary = Color(0xFF03DAC6),
                onSecondary = Color.Black,
                background = Color.White,
                onBackground = Color.Black,
                surface = Color.White,
                onSurface = Color.Black,
                error = Color(0xFFB00020),
                onError = Color.White
            )
        }
        baseColorScheme.copy(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            onSurface = Color(0xFF121216),
            onBackground = Color(0xFF121216)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}