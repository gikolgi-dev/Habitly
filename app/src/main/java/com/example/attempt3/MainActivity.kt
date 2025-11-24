package com.example.attempt3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.attempt3.ui.theme.Attempt3Theme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            // Handle permission grant result if needed
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(
            applicationContext,
            HabitDatabase::class.java, "habit-database"
        ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_8_9, MIGRATION_9_10).build()
        val habitDao = db.habitDao()
        val settingsDataStore = SettingsDataStore(applicationContext)
        val viewModel: HabitViewModel by viewModels {
            HabitViewModelFactory(habitDao)
        }

        setContent {
            Attempt3Theme {
                Surface {
                    ExpressiveMainScreen(viewModel, habitDao, db, settingsDataStore)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }
        }
    }
}