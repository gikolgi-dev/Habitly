/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.Database.HabitViewModel
import com.habitly.habitly.data.Database.HabitViewModelFactory
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.ui.colors.Attempt3Theme
import com.habitly.habitly.ui.screen.ExpressiveMainScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isReady = false

        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        createNotificationChannel()

        val db = HabitDatabase.getDatabase(applicationContext)
        val habitDao = db.habitDao()
        val settingsDataStore = SettingsDataStore(applicationContext)
        val viewModel: HabitViewModel by viewModels {
            HabitViewModelFactory(habitDao)
        }

        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            // Wait for settings to load so colors are correct from the very first frame
            val initialTheme = settingsDataStore.theme.first()
            val initialMaterial = settingsDataStore.useMaterialTheming.first()

            setContent {
                Attempt3Theme(
                    settingsDataStore = settingsDataStore,
                    initialTheme = initialTheme,
                    initialUseMaterialTheming = initialMaterial
                ) {
                    ExpressiveMainScreen(viewModel, habitDao, db, settingsDataStore)
                }
            }
            
            isReady = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Habit Reminders"
            val descriptionText = "Notifications to remind you of your habits"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("habit_reminders", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
