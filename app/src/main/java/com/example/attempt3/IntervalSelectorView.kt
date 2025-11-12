package com.example.attempt3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalPicker(
    completionsPerInterval: String,
    onCompletionsPerIntervalChange: (String) -> Unit,
    intervalValue: String,
    onIntervalValueChange: (String) -> Unit,
    intervalUnit: String,
    onIntervalUnitChange: (String) -> Unit,
    validationResult: IntervalValidationResult
) {
    val timeUnits = listOf("day", "week", "month")
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = completionsPerInterval,
            onValueChange = onCompletionsPerIntervalChange,
            label = { Text("Completions") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            isError = validationResult.completionsError != null
        )
        Text("/")
        OutlinedTextField(
            value = intervalValue,
            onValueChange = onIntervalValueChange,
            label = { Text("Interval") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            isError = validationResult.intervalValueError != null
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
        ) {
            OutlinedTextField(
                value = intervalUnit,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor().align(Alignment.CenterVertically)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                timeUnits.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit) },
                        onClick = {
                            onIntervalUnitChange(unit)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    if (validationResult.completionsError != null) {
        Text(validationResult.completionsError, color = MaterialTheme.colorScheme.error)
    }
    if (validationResult.intervalValueError != null) {
        Text(validationResult.intervalValueError, color = MaterialTheme.colorScheme.error)
    }
}

data class IntervalValidationResult(
    val completionsError: String? = null,
    val intervalValueError: String? = null
)
