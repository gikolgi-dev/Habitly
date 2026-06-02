/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.colors.predefinedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
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
    val statsLayout: String? = null,
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
    val appOrigin: String = "habitly",
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
fun ImportExportScreen(db: HabitDatabase, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val bordersAlphaState = settingsDataStore.borders.collectAsState(initial = null)
    val bordersAlpha = bordersAlphaState.value ?: return

    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var importType by remember { mutableStateOf<ImportType?>(null) }
    var mergeData by remember { mutableStateOf(false) }
    var isToggling by remember { mutableStateOf(false) }

    val jsonParser = remember { Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    } }

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
                                    statsLayout = habitWithCompletions.habit.statsLayout,
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
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedFileUri = uri
            if (uri != null) {
                scope.launch {
                    try {
                        val headerText = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                                val buffer = CharArray(4000)
                                val readCount = reader.read(buffer)
                                if (readCount != -1) {
                                    String(buffer, 0, readCount)
                                } else {
                                    ""
                                }
                            }
                        }
                        if (headerText != null) {
                            if (headerText.contains("\"appOrigin\":\"habitly\"") ||
                                headerText.contains("\"appOrigin\": \"habitly\"") ||
                                headerText.contains("\"completionsPerInterval\"")) {
                                importType = ImportType.APP_BACKUP
                            } else {
                                importType = null // let the user choose if it can't determine or is from somewhere else
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportFraction by animateFloatAsState(
        targetValue = if (selectedFileUri == null) 0.5f else 0.0001f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutQuart),
        label = "exportFraction"
    )
    val exportAlpha by animateFloatAsState(
        targetValue = if (selectedFileUri == null) 1f else 0f,
        label = "exportAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (exportFraction > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(exportFraction.coerceAtLeast(0.0001f))
                    .graphicsLayer { alpha = exportAlpha }
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = bordersAlpha),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        } else {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        }
                        exportLauncher.launch("habits_backup_$currentDate.json")
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Export Icon",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export habits and completion for safe keeping, migration and sharing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight((1f - exportFraction).coerceAtLeast(0.0001f))
                .clip(RoundedCornerShape(8.dp))
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = bordersAlpha),
                    RoundedCornerShape(8.dp)
                )
                .then(
                    if (selectedFileUri == null) {
                        Modifier.clickable { importLauncher.launch("application/json") }
                    } else Modifier
                )
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                val headerBias by animateFloatAsState(
                    targetValue = if (selectedFileUri == null) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "headerBias"
                )

                if (headerBias > 0.001f) {
                    Spacer(modifier = Modifier.weight(headerBias))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Import Icon",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import habits from here or other apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = selectedFileUri != null,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        val fileName =
                            selectedFileUri?.let { getFileName(it, context) } ?: "Unknown File"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(
                                        alpha = bordersAlpha.coerceAtLeast(
                                            0.1f
                                        )
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { importLauncher.launch("application/json") }
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Select Source",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val options = listOf("Habitly", "HabitKit")
                        val types = listOf(ImportType.APP_BACKUP, ImportType.HABIT_KIT)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    ),
                                    onClick = { importType = types[index] },
                                    selected = importType == types[index],
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val targetCornerRadius = when {
                            isPressed || isToggling -> 14.dp
                            mergeData -> 22.dp
                            else -> 28.dp
                        }

                        val cornerRadius by animateDpAsState(
                            targetValue = targetCornerRadius,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "cornerRadius"
                        )

                        val containerColor by animateColorAsState(
                            targetValue = if (mergeData) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "containerColor"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (mergeData) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.error,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "contentColor"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp)))
                                .background(containerColor)
                                .border(
                                    1.dp,
                                    contentColor.copy(
                                        alpha = (bordersAlpha * 2f).coerceAtMost(1f)
                                            .coerceAtLeast(0.2f)
                                    ),
                                    RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        scope.launch {
                                            isToggling = true
                                            mergeData = !mergeData
                                            delay(100)
                                            isToggling = false
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (mergeData) Icons.AutoMirrored.Filled.CallMerge else Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (mergeData) "Merge Data" else "Overwrite Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        /*Spacer(modifier = Modifier.height(24.dp))*/

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedFileUri = null
                                    importType = null
                                    mergeData = false
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = bordersAlpha.coerceAtLeast(0.15f)
                                    )
                                )
                            ) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(12.dp))
                            val confirmAlpha by animateFloatAsState(
                                targetValue = if (importType != null) 1f else 0.5f,
                                label = "confirmAlpha"
                            )
                            Button(
                                enabled = importType != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = confirmAlpha },
                                onClick = {
                                    scope.launch {
                                        try {
                                            val jsonString = withContext(Dispatchers.IO) {
                                                context.contentResolver.openInputStream(
                                                    selectedFileUri!!
                                                )?.use { inputStream ->
                                                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                                        reader.readText()
                                                    }
                                                }
                                            }

                                            if (jsonString != null) {
                                                val habitsToInsert = mutableListOf<Habit>()
                                                val completionsToInsert =
                                                    mutableListOf<Completion>()

                                                if (importType == ImportType.HABIT_KIT) {
                                                    val habitKitData =
                                                        jsonParser.decodeFromString<HabitKitExport>(
                                                            jsonString
                                                        )

                                                    habitKitData.habits.forEach { kitHabit ->
                                                        val reminder =
                                                            habitKitData.reminders.find { it.habitId == kitHabit.id }

                                                        val notificationsEnabled = reminder != null
                                                        val notificationTime =
                                                            if (reminder != null) String.format(
                                                                "%02d:%02d",
                                                                reminder.hour,
                                                                reminder.minute
                                                            ) else null

                                                        val dayMap = mapOf(
                                                            1 to "MON",
                                                            2 to "TUE",
                                                            3 to "WED",
                                                            4 to "THU",
                                                            5 to "FRI",
                                                            6 to "SAT",
                                                            7 to "SUN"
                                                        )
                                                        val notificationDays =
                                                            reminder?.weekdayIndices?.mapNotNull { dayMap[it] }
                                                                ?.joinToString(",")

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
                                                                    Instant.parse(kitCompletion.date)
                                                                        .toEpochMilli()
                                                                } catch (_: Exception) {
                                                                    try {
                                                                        val localDateTime =
                                                                            LocalDateTime.parse(
                                                                                kitCompletion.date
                                                                            )
                                                                        val offset =
                                                                            ZoneOffset.ofTotalSeconds(
                                                                                kitCompletion.timezoneOffsetInMinutes * 60
                                                                            )
                                                                        localDateTime.toInstant(
                                                                            offset
                                                                        ).toEpochMilli()
                                                                    } catch (_: Exception) {
                                                                        val localDate =
                                                                            LocalDate.parse(
                                                                                kitCompletion.date
                                                                            )
                                                                        val offset =
                                                                            ZoneOffset.ofTotalSeconds(
                                                                                kitCompletion.timezoneOffsetInMinutes * 60
                                                                            )
                                                                        localDate.atStartOfDay()
                                                                            .toInstant(offset)
                                                                            .toEpochMilli()
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
                                                    val exportedData =
                                                        jsonParser.decodeFromString<ExportData>(
                                                            jsonString
                                                        )
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
                                                            notificationDays = exportedHabit.notificationDays,
                                                            statsLayout = exportedHabit.statsLayout
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
                                                        NotificationScheduler(context).cancelAllNotifications()
                                                        db.habitDao().clearAllTables()
                                                    }
                                                    db.habitDao().insertHabits(habitsToInsert)
                                                    db.habitDao()
                                                        .insertCompletions(completionsToInsert)
                                                    NotificationScheduler(context).rescheduleAll()
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "Import successful",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedFileUri = null
                                                importType = null
                                                mergeData = false
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to read file",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                context,
                                                "Import failed: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Confirm Import")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
