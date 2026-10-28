package com.hexel.launcher.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.hexel.core.hex.HexCoord
import com.hexel.launcher.HomeState
import com.hexel.launcher.ui.theme.HexelColors
import kotlin.math.roundToInt

/**
 * Draws the visible part of the map. Only tiles intersecting the viewport are visited, so cost
 * depends on screen size, not on the number of installed apps.
 *
 * Seams: the whole background is painted in the surface colour first and every tile edge is then
 * stroked, so anti-aliasing between neighbouring fills can never show a gap.
 */
internal fun DrawScope.drawHexMap(
    geometry: MapGeometry,
    cameraOffset: Offset,
    hexPath: Path,
    state: HomeState,
    icons: Map<String, ImageBitmap>,
    labels: Map<String, TextLayoutResult>,
    preview: SwipePreview?,
) {
    val layout = geometry.layout
    val origin = geometry.origin(size.width, size.height)
    drawRect(HexelColors.Surface)

    val left = -origin.x - cameraOffset.x
    val top = -origin.y - cameraOffset.y
    val visible = layout.hexesInRect(left, top, left + size.width, top + size.height)
    val border = Stroke(width = TileMetrics.BorderWidthDp * density)

    // Pass 1: tile fills and borders.
    for (hex in visible) {
        val c = geometry.centerOf(hex, origin, cameraOffset)
        translate(c.x, c.y) {
            if (state.tiles[hex]?.let { it in state.apps } == true) drawPath(hexPath, HexelColors.Tile)
            drawPath(hexPath, HexelColors.Border, style = border)
        }
    }

    // Pass 2: icons and labels.
    for (hex in visible) {
        val pkg = state.tiles[hex] ?: continue
        if (pkg !in state.apps) continue
        drawAppContent(geometry.centerOf(hex, origin, cameraOffset), icons[pkg], labels[pkg])
    }

    // Pass 3: swipe direction highlight.
    if (preview != null) drawSwipePreview(geometry, origin, cameraOffset, hexPath, state, icons, labels, preview)
}

private fun DrawScope.drawAppContent(center: Offset, icon: ImageBitmap?, label: TextLayoutResult?) {
    val iconSize = TileMetrics.IconSizeDp * density
    val iconCenterY = center.y + TileMetrics.IconCenterYDp * density
    if (icon != null) {
        drawImage(
            image = icon,
            dstOffset = IntOffset((center.x - iconSize / 2f).roundToInt(), (iconCenterY - iconSize / 2f).roundToInt()),
            dstSize = IntSize(iconSize.roundToInt(), iconSize.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    } else {
        // Placeholder while icons are loading.
        drawCircle(HexelColors.Border, radius = iconSize * 0.42f, center = Offset(center.x, iconCenterY))
    }
    if (label != null) {
        drawText(
            textLayoutResult = label,
            topLeft = Offset(center.x - label.size.width / 2f, center.y + TileMetrics.LabelTopDp * density),
        )
    }
}

private fun DrawScope.drawSwipePreview(
    geometry: MapGeometry,
    origin: Offset,
    cameraOffset: Offset,
    hexPath: Path,
    state: HomeState,
    icons: Map<String, ImageBitmap>,
    labels: Map<String, TextLayoutResult>,
    preview: SwipePreview,
) {
    val layout = geometry.layout
    val source = geometry.centerOf(preview.source, origin, cameraOffset)
    val targetApp = preview.target?.takeIf { it in state.apps }

    // Wedge from the source centre to the edge facing the swipe direction.
    val (a, b) = layout.edgeCorners(preview.direction)
    val wedge = Path().apply {
        moveTo(source.x, source.y)
        lineTo(source.x + a.x, source.y + a.y)
        lineTo(source.x + b.x, source.y + b.y)
        close()
    }
    drawPath(wedge, if (targetApp != null) HexelColors.Highlight else HexelColors.Border.copy(alpha = 0.6f))

    // Show the app that will launch on top of the neighbouring tile in that direction.
    if (targetApp != null) {
        val target: HexCoord = preview.source.neighbor(preview.direction)
        val c = geometry.centerOf(target, origin, cameraOffset)
        translate(c.x, c.y) {
            drawPath(hexPath, HexelColors.HighlightTile)
            drawPath(hexPath, HexelColors.Primary.copy(alpha = 0.55f), style = Stroke(width = 1.5f * density))
        }
        drawAppContent(c, icons[targetApp], labels[targetApp])
    }
}
