package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

enum class DayLabelDisplayOptions {
    Off, Some, All
}

@Composable
fun DayLabelSelector(
    selectedOption: DayLabelDisplayOptions,
    onOptionSelected: (DayLabelDisplayOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = DayLabelDisplayOptions.entries.map { it.name }

    TabRow(
        selectedTabIndex = selectedOption.ordinal,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedOption.ordinal])
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        },
        divider = {}
    ) {
        options.forEachIndexed { index, text ->
            val selected = selectedOption.ordinal == index
            Tab(
                selected = selected,
                onClick = { onOptionSelected(DayLabelDisplayOptions.entries[index]) },
                text = {
                    Text(
                        text = text,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}