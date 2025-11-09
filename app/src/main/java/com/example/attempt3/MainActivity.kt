package com.example.attempt3

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.room.Room

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(applicationContext, HabitDatabase::class.java, "habits.db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val viewModel by viewModels<HabitViewModel> { HabitViewModelFactory(db.habitDao()) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            // App theme and content
            ExpressiveDarkApp(viewModel = viewModel, habitDao = db.habitDao(), db = db)
        }
    }
}

/**
 * App-level theme and root composable.
 * Uses a custom dark color scheme for a bold expressive dark look.
 */
@Composable
fun ExpressiveDarkApp(
    viewModel: HabitViewModel,
    habitDao: HabitDao,
    db: HabitDatabase,
    content: @Composable (() -> Unit)? = null
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val baseColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Custom dark color scheme — pick expressive, slightly saturated colors for accent.
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

    val colorScheme = if (darkTheme) {
        baseColorScheme.copy(
            background = Color(0xFF111111),
            surface = Color(0xFF1C1B1B)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (content != null) content() else ExpressiveMainScreen(viewModel = viewModel, habitDao = habitDao, db = db)
        }
    }
}