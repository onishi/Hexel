package com.hexel.launcher.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hexel.core.hex.HexDirection
import com.hexel.launcher.HomeState
import com.hexel.launcher.R
import com.hexel.launcher.ui.theme.HexelColors

/** Lists installed apps to assign to one direction of [source]. */
@Composable
fun AppPicker(
    source: String,
    direction: HexDirection,
    state: HomeState,
    icons: Map<String, ImageBitmap>,
    onSelect: (packageName: String?) -> Unit,
    onClose: () -> Unit,
) {
    val current = state.assignments[source]?.get(direction)
    // Already sorted by label in the repository; the source app itself is not a useful target.
    val candidates = state.apps.values.filter { it.packageName != source }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TextButton(onClick = onClose, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("← " + stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.picker_title, stringResource(directionName(direction))),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                if (current != null) {
                    item(key = "__unassign") {
                        PickerRow(icon = null, label = stringResource(R.string.unassign), selected = false) {
                            onSelect(null)
                        }
                        HorizontalDivider(color = HexelColors.Border)
                    }
                }
                items(candidates, key = { it.packageName }) { app ->
                    PickerRow(icon = icons[app.packageName], label = app.label, selected = app.packageName == current) {
                        onSelect(app.packageName)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(icon: ImageBitmap?, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) HexelColors.HighlightTile else HexelColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Box(Modifier.size(36.dp).background(HexelColors.Border, CircleShape))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) HexelColors.Primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
