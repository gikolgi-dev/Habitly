/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.Database.HabitViewModel
import com.example.attempt3.data.Database.HabitViewModelFactory
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.colors.Attempt3Theme
import com.example.attempt3.ui.screen.ExpressiveMainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        createNotificationChannel()

        val db = HabitDatabase.Companion.getDatabase(applicationContext)
        val habitDao = db.habitDao()
        val settingsDataStore = SettingsDataStore(applicationContext)
        val viewModel: HabitViewModel by viewModels {
            HabitViewModelFactory(habitDao)
        }

        setContent {
            Attempt3Theme(settingsDataStore = settingsDataStore) {
                ExpressiveMainScreen(viewModel, habitDao, db, settingsDataStore)
            }
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