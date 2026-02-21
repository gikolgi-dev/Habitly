package com.example.attempt3.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.Database.Completion
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.colors.predefinedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class ExportedHabit(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: Int,
    val archived: Boolean,
    val orderIndex: Int,
    val createdAt: String,
    val isInverse: Boolean,
    val emoji: String?,
    val completionsPerInterval: Int,
    val intervalUnit: String,
    val notificationsEnabled: Boolean,
    val notificationTime: String?,
    val notificationDays: String?,
    val completions: List<ExportedCompletion>
)

@Serializable
data class ExportedCompletion(
    val id: String,
    val habitId: String,
    val date: Long,
    val timezoneOffsetInMinutes: Int,
    val amountOfCompletions: Int
)

@Serializable
data class ExportData(
    val habits: List<ExportedHabit>
)

// HabitKit Data Classes
@Serializable
data class HabitKitExport(
    val habits: List<HabitKitHabit> = emptyList(),
    val completions: List<HabitKitCompletion> = emptyList(),
    // Intervals are ignored as requested by the user
    val reminders: List<HabitKitReminder> = emptyList()
)

@Serializable
data class HabitKitHabit(
    val id: String,
    val name: String,
    val description: String? = "",
    val icon: String,
    val color: String,
    val archived: Boolean,
    val orderIndex: Int,
    val createdAt: String,
    val isInverse: Boolean,
    val emoji: String?
)

@Serializable
data class HabitKitCompletion(
    val id: String,
    val date: String,
    val habitId: String,
    val timezoneOffsetInMinutes: Int,
    val amountOfCompletions: Int,
    val note: String? = null
)

@Serializable
data class HabitKitReminder(
    val id: String,
    val habitId: String,
    val weekdayIndices: List<Int>,
    val hour: Int,
    val minute: Int
)

enum class ImportType {
    APP_BACKUP,
    HABIT_KIT
}

fun getFileName(uri: Uri, context: Context): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = cursor.getString(displayNameIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            if (cut != null) {
                result = result.substring(cut + 1)
            }
        }
    }
    return result
}


@SuppressLint("DefaultLocale")
@Composable
fun ImportExportScreen(db: HabitDatabase) {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val bordersAlpha by settingsDataStore.borders.collectAsState(initial = 0.25f)

    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val jsonParser = remember { Json { ignoreUnknownKeys = true } }
    
    val habitColorMap = remember {
        predefinedColors.flatMap { namedColor ->
            namedColor.names.map { name -> name.lowercase() to namedColor.color.toArgb() }
        }.toMap()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    try {
                        val habitsWithCompletions = withContext(Dispatchers.IO) {
                            db.habitDao().getAllHabitsWithCompletionsSnapshot()
                        }

                        val exportedData = ExportData(
                            habits = habitsWithCompletions.map { habitWithCompletions ->
                                ExportedHabit(
                                    id = habitWithCompletions.habit.id,
                                    name = habitWithCompletions.habit.name,
                                    description = habitWithCompletions.habit.description,
                                    icon = habitWithCompletions.habit.icon,
                                    color = habitWithCompletions.habit.color,
                                    archived = habitWithCompletions.habit.archived,
                                    orderIndex = habitWithCompletions.habit.orderIndex,
                                    createdAt = habitWithCompletions.habit.createdAt,
                                    isInverse = habitWithCompletions.habit.isInverse,
                                    emoji = habitWithCompletions.habit.emoji,
                                    completionsPerInterval = habitWithCompletions.habit.completionsPerInterval,
                                    intervalUnit = habitWithCompletions.habit.intervalUnit,
                                    notificationsEnabled = habitWithCompletions.habit.notificationsEnabled,
                                    notificationTime = habitWithCompletions.habit.notificationTime,
                                    notificationDays = habitWithCompletions.habit.notificationDays,
                                    completions = habitWithCompletions.completions.map { completion ->
                                        ExportedCompletion(
                                            id = completion.id,
                                            habitId = habitWithCompletions.habit.id,
                                            date = completion.date,
                                            timezoneOffsetInMinutes = completion.timezoneOffsetInMinutes,
                                            amountOfCompletions = completion.amountOfCompletions
                                        )
                                    }
                                )
                            }
                        )

                        val jsonString = jsonParser.encodeToString(exportedData)

                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                OutputStreamWriter(outputStream).use { writer ->
                                    writer.write(jsonString)
                                }
                            }
                        }
                        Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedFileUri = uri
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(color = MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = bordersAlpha),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Export Icon", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Export", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Export habits and completion for safe keeping, migration and sharing", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else { "" }
                    exportLauncher.launch("habits_backup_$currentDate.json")
                }) {
                    Text("Export Data")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(color = MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = bordersAlpha),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Import Icon", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Import", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Import habits from here or other apps", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { importLauncher.launch("application/json") }) {
                        Text("Select File to Import")
                    }
                }
            } else {
                var importType by remember { mutableStateOf<ImportType?>(null) }
                var mergeData by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val fileName = selectedFileUri?.let { getFileName(it, context) } ?: "Unknown File"
                    Text("File: $fileName", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { importLauncher.launch("application/json") }) {
                        Text("Change File")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Source", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = importType == ImportType.APP_BACKUP,
                            onClick = { importType = ImportType.APP_BACKUP }
                        )
                        Text("This App")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = importType == ImportType.HABIT_KIT,
                            onClick = { importType = ImportType.HABIT_KIT }
                        )
                        Text("HabitKit")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text("Merge with existing data")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = mergeData,
                            onCheckedChange = { mergeData = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Row {
                        Button(onClick = { selectedFileUri = null }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = importType != null,
                            onClick = {
                                scope.launch {
                                    try {
                                        val jsonString = withContext(Dispatchers.IO) {
                                            context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                                    reader.readText()
                                                }
                                            }
                                        }

                                        if (jsonString != null) {
                                            val habitsToInsert = mutableListOf<Habit>()
                                            val completionsToInsert = mutableListOf<Completion>()

                                            if (importType == ImportType.HABIT_KIT) {
                                                val habitKitData = jsonParser.decodeFromString<HabitKitExport>(jsonString)

                                                habitKitData.habits.forEach { kitHabit ->
                                                    val reminder = habitKitData.reminders.find { it.habitId == kitHabit.id }

                                                    val notificationsEnabled = reminder != null
                                                    val notificationTime = if (reminder != null) String.format("%02d:%02d", reminder.hour, reminder.minute) else null

                                                    val dayMap = mapOf(
                                                        1 to "MON", 2 to "TUE", 3 to "WED", 4 to "THU", 5 to "FRI", 6 to "SAT", 7 to "SUN"
                                                    )
                                                    val notificationDays = reminder?.weekdayIndices?.mapNotNull { dayMap[it] }?.joinToString(",")

                                                    val habit = Habit(
                                                        id = kitHabit.id,
                                                        name = kitHabit.name,
                                                        description = kitHabit.description
                                                            ?: "",
                                                        icon = "default_icon", // Default icon as requested
                                                        color = habitColorMap[kitHabit.color.lowercase()]
                                                            ?: Color.GRAY,
                                                        archived = kitHabit.archived,
                                                        orderIndex = kitHabit.orderIndex,
                                                        createdAt = kitHabit.createdAt,
                                                        isInverse = kitHabit.isInverse,
                                                        emoji = kitHabit.emoji,
                                                        completionsPerInterval = 1, // Default value
                                                        intervalUnit = "day",      // Default value
                                                        notificationsEnabled = notificationsEnabled,
                                                        notificationTime = notificationTime,
                                                        notificationDays = notificationDays
                                                    )
                                                    habitsToInsert.add(habit)
                                                }

                                                habitKitData.completions.forEach { kitCompletion ->
                                                    val dateMillis = try {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            try {
                                                                    Instant.parse(kitCompletion.date).toEpochMilli()
                                                            } catch (_: Exception) {
                                                                try {
                                                                    val localDateTime = LocalDateTime.parse(kitCompletion.date)
                                                                    val offset = ZoneOffset.ofTotalSeconds(kitCompletion.timezoneOffsetInMinutes * 60)
                                                                    localDateTime.toInstant(offset).toEpochMilli()
                                                                } catch (_: Exception) {
                                                                    val localDate = LocalDate.parse(kitCompletion.date)
                                                                    val offset = ZoneOffset.ofTotalSeconds(kitCompletion.timezoneOffsetInMinutes * 60)
                                                                    localDate.atStartOfDay().toInstant(offset).toEpochMilli()
                                                                }
                                                            }
                                                        } else {
                                                            0L // Fallback for older SDKs
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        0L
                                                    }

                                                    if (dateMillis != 0L && kitCompletion.amountOfCompletions > 0) {
                                                        completionsToInsert.add(
                                                            Completion(
                                                                id = UUID.randomUUID()
                                                                    .toString(),
                                                                habitId = kitCompletion.habitId,
                                                                date = dateMillis,
                                                                timezoneOffsetInMinutes = kitCompletion.timezoneOffsetInMinutes,
                                                                amountOfCompletions = kitCompletion.amountOfCompletions
                                                            )
                                                        )
                                                    }
                                                }

                                            } else {
                                                // Default APP_BACKUP logic
                                                val exportedData = jsonParser.decodeFromString<ExportData>(jsonString)
                                                habitsToInsert.addAll(exportedData.habits.map { exportedHabit ->
                                                    Habit(
                                                        id = exportedHabit.id,
                                                        name = exportedHabit.name,
                                                        description = exportedHabit.description,
                                                        icon = exportedHabit.icon,
                                                        color = exportedHabit.color,
                                                        archived = exportedHabit.archived,
                                                        orderIndex = exportedHabit.orderIndex,
                                                        createdAt = exportedHabit.createdAt,
                                                        isInverse = exportedHabit.isInverse,
                                                        emoji = exportedHabit.emoji,
                                                        completionsPerInterval = exportedHabit.completionsPerInterval,
                                                        intervalUnit = exportedHabit.intervalUnit,
                                                        notificationsEnabled = exportedHabit.notificationsEnabled,
                                                        notificationTime = exportedHabit.notificationTime,
                                                        notificationDays = exportedHabit.notificationDays
                                                    )
                                                })
                                                completionsToInsert.addAll(exportedData.habits.flatMap { exportedHabit ->
                                                    exportedHabit.completions.map { exportedCompletion ->
                                                        Completion(
                                                            id = exportedCompletion.id,
                                                            habitId = exportedCompletion.habitId,
                                                            date = exportedCompletion.date,
                                                            timezoneOffsetInMinutes = exportedCompletion.timezoneOffsetInMinutes,
                                                            amountOfCompletions = exportedCompletion.amountOfCompletions
                                                        )
                                                    }
                                                })
                                            }

                                            withContext(Dispatchers.IO) {
                                                if (!mergeData) {
                                                    db.habitDao().clearAllTables()
                                                }
                                                db.habitDao().insertHabits(habitsToInsert)
                                                db.habitDao().insertCompletions(completionsToInsert)
                                            }
                                            Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
                                            selectedFileUri = null
                                        } else {
                                            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        ) {
                            Text("Confirm Import")
                        }
                    }
                }
            }
        }
    }
}
