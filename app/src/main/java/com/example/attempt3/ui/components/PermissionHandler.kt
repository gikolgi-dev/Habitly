/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.attempt3.MainActivity

/**
 * Interface to interact with notification permission logic.
 */
interface NotificationPermissionHandler {
    val hasPermission: Boolean
    fun requestPermission()
}

/**
 * A helper to handle notification permissions.
 * It will request permission if not granted, and show a settings dialog if permanently denied.
 */
@Composable
fun rememberNotificationPermissionHandler(
    onPermissionGranted: () -> Unit = {}
): NotificationPermissionHandler {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                onPermissionGranted()
            } else {
                // If denied, we check if we should show settings dialog next time
                // or maybe even now if they clicked "Don't ask again"
            }
        }
    )

    if (showSettingsDialog) {
        PermissionSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                showSettingsDialog = false
                openAppSettings(context)
            }
        )
    }

    return remember(hasPermission, showSettingsDialog) {
        object : NotificationPermissionHandler {
            override val hasPermission: Boolean get() = hasPermission
            
            override fun requestPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val activity = context as? MainActivity
                    val shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                    } ?: false

                    val currentStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    
                    if (currentStatus == PackageManager.PERMISSION_GRANTED) {
                        hasPermission = true
                        onPermissionGranted()
                    } else {
                        // If we've already shown rationale and it's still denied, 
                        // or if we shouldn't show rationale but it's denied (and not the first time),
                        // then it might be permanently denied.
                        
                        // Heuristic for "Permanently Denied": 
                        // If currentStatus is DENIED and shouldShowRationale is false, 
                        // it's either the first time OR it's permanently denied.
                        // Since this is triggered by a button press, if we want to be aggressive, 
                        // we can show our own dialog if the system one doesn't appear.
                        
                        if (!shouldShowRationale && hasPermission == false) {
                            // This state often means "Don't ask again" was selected previously.
                            // We'll show the settings dialog.
                            showSettingsDialog = true
                        } else {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    hasPermission = true
                    onPermissionGranted()
                }
            }
        }
    }
}

@Composable
fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text("Notification permission is disabled. Please enable it in system settings to receive reminders.") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onOpenSettings,
                    shape = CircleShape
                ) {
                    Text("Settings")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
