package com.hexel.launcher.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexDirection
import com.hexel.core.hex.HexLayout
import com.hexel.core.hex.Vec2
import com.hexel.launcher.HomeState
import com.hexel.launcher.R
import com.hexel.launcher.ui.HexelLogo
import com.hexel.launcher.ui.theme.HexelColors
import com.hexel.launcher.ui.theme.TileLabelStyle
import kotlin.math.roundToInt

/**
 * Long-press screen: the source app sits in the middle of a seven-hex "flower" and each of the six
 * surrounding hexes shows what a swipe in that direction launches. Tap a hex to change it.
 */
@Composable
fun AssignmentEditor(
    source: String,
    state: HomeState,
    icons: Map<String, ImageBitmap>,
    onPickDirection: (HexDirection) -> Unit,
    onClose: () -> Unit,
) {
    val sourceApp = state.apps[source]
    val assigned = state.assignments[source].orEmpty()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("← " + stringResource(R.string.back)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.editor_title), style = MaterialTheme.typography.titleLarge)
            Text(
                sourceApp?.label ?: source,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            DirectionFlower(
                sourceIcon = icons[source],
                sourceLabel = sourceApp?.label ?: source,
                targets = HexDirection.entries.associateWith { dir ->
                    assigned[dir]?.let { pkg -> FlowerTarget(state.apps[pkg]?.label, icons[pkg]) }
                },
                onPick = onPickDirection,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.editor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                HexelLogo(Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A direction's assigned app. [label] is null when the app is no longer installed. */
private data class FlowerTarget(val label: String?, val icon: ImageBitmap?)

@Composable
private fun DirectionFlower(
    sourceIcon: ImageBitmap?,
    sourceLabel: String,
    targets: Map<HexDirection, FlowerTarget?>,
    onPick: (HexDirection) -> Unit,
) {
    val density = LocalDensity.current
    val radiusDp = 46f
    val layout = remember(density) { HexLayout(radiusDp * density.density) }
    val path = remember(layout) {
        Path().apply {
            layout.corners().forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            close()
        }
    }
    val currentOnPick by rememberUpdatedState(onPick)
    val measurer = rememberTextMeasurer()
    val notInstalled = stringResource(R.string.not_installed)
    val labelWidth = (62 * density.density).roundToInt()
    fun measure(text: String): TextLayoutResult = measurer.measure(
        text = text,
        style = TileLabelStyle.copy(textAlign = TextAlign.Center),
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        maxLines = 1,
        constraints = Constraints(minWidth = labelWidth, maxWidth = labelWidth),
    )
    val directionNames = HexDirection.entries.associateWith { stringResource(directionName(it)) }

    // Flower: 3 hex widths wide, 5 radii tall.
    val widthDp = (3 * 1.7320508f * radiusDp + 4).dp
    val heightDp = (5 * radiusDp + 4).dp

    Box(Modifier.size(widthDp, heightDp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    detectTapGestures { pos ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val hex = layout.worldToHex(Vec2(pos.x - c.x, pos.y - c.y))
                        HexDirection.entries.firstOrNull { it.offset == hex }?.let { currentOnPick(it) }
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val border = Stroke(width = 1.dp.toPx())
            val iconSize = 36.dp.toPx()

            fun drawTile(hex: HexCoord, fill: Color, icon: ImageBitmap?, label: String) {
                val w = layout.hexToWorld(hex)
                val c = Offset(center.x + w.x, center.y + w.y)
                translate(c.x, c.y) {
                    drawPath(path, fill)
                    drawPath(path, HexelColors.Border, style = border)
                }
                val iconTop = c.y - 7.dp.toPx() - iconSize / 2f
                if (icon != null) {
                    drawImage(
                        image = icon,
                        dstOffset = IntOffset((c.x - iconSize / 2f).roundToInt(), iconTop.roundToInt()),
                        dstSize = IntSize(iconSize.roundToInt(), iconSize.roundToInt()),
                    )
                }
                val text = measure(label)
                val textTop = if (icon != null) c.y + 14.dp.toPx() else c.y - text.size.height / 2f
                drawText(text, topLeft = Offset(c.x - text.size.width / 2f, textTop))
            }

            drawTile(HexCoord.Origin, HexelColors.Tile, sourceIcon, sourceLabel)
            for (dir in HexDirection.entries) {
                val target = targets[dir]
                if (target == null) {
                    drawTile(dir.offset, HexelColors.Surface, null, directionNames.getValue(dir))
                } else {
                    drawTile(dir.offset, HexelColors.HighlightTile, target.icon, target.label ?: notInstalled)
                }
            }
        }
    }
}

fun directionName(direction: HexDirection): Int = when (direction) {
    HexDirection.EAST -> R.string.dir_east
    HexDirection.NORTH_EAST -> R.string.dir_north_east
    HexDirection.NORTH_WEST -> R.string.dir_north_west
    HexDirection.WEST -> R.string.dir_west
    HexDirection.SOUTH_WEST -> R.string.dir_south_west
    HexDirection.SOUTH_EAST -> R.string.dir_south_east
}
