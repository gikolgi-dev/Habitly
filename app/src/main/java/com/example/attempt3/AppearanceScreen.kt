package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(modifier: Modifier = Modifier, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = "system")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = 0.25f),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = modifier) {
                    val themes = listOf("Light", "Dark", "System")
                    themes.forEach { theme ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (theme.lowercase() == currentTheme),
                                    onClick = {
                                        scope.launch {
                                            settingsDataStore.setTheme(theme.lowercase())
                                        }
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (theme.lowercase() == currentTheme),
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setTheme(theme.lowercase())
                                    }
                                }
                            )
                            Text(
                                text = theme,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}