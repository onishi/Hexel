package com.hexel.launcher.ui.home

import android.graphics.Rect
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.hexel.core.gesture.GestureClassifier
import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexDirection
import com.hexel.core.hex.HexLayout
import com.hexel.core.hex.Vec2
import com.hexel.launcher.HomeState
import com.hexel.launcher.ui.theme.TileLabelStyle
import kotlin.math.roundToInt

/** What the direction highlight currently shows. */
data class SwipePreview(val source: HexCoord, val direction: HexDirection, val target: String?)

/**
 * The home screen: an endless pointy-top hex grid with apps placed on it.
 *
 * Responsibilities are split as follows:
 * - geometry: [HexLayout] (core)
 * - tap / flick / swipe decisions: [hexMapGestures] + [GestureClassifier] (core)
 * - drawing: [drawHexMap]
 */
@Composable
fun HexMap(
    state: HomeState,
    icons: Map<String, ImageBitmap>,
    camera: MapCamera,
    classifier: GestureClassifier,
    onLaunch: (packageName: String, sourceBounds: Rect) -> Unit,
    onLongPress: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val radiusPx = TileMetrics.RadiusDp * density.density
    val layout = remember(radiusPx) { HexLayout(radiusPx) }
    val hexPath = remember(layout) { hexPath(layout) }

    // Let the outermost apps reach the centre of the screen, but not further.
    val limit = Offset(layout.width * state.maxRing, layout.rowHeight * state.maxRing)
    SideEffect { camera.limit = limit }

    val insets = WindowInsets.safeDrawing
    val insetTop = insets.getTop(density)
    val insetBottom = insets.getBottom(density)

    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    val labelWidthPx = (TileMetrics.LabelMaxWidthDp * density.density).roundToInt()
    val labelTexts = state.apps.mapValues { it.value.label }
    val labels: Map<String, TextLayoutResult> = remember(labelTexts, labelWidthPx, density) {
        labelTexts.mapValues { (_, label) ->
            textMeasurer.measure(
                text = label,
                style = TileLabelStyle.copy(textAlign = TextAlign.Center),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(minWidth = labelWidthPx, maxWidth = labelWidthPx),
            )
        }
    }

    var preview by remember { mutableStateOf<SwipePreview?>(null) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val decay = remember(density) { SplineBasedFloatDecayAnimationSpec(density) }

    val currentState by rememberUpdatedState(state)
    val currentOnLaunch by rememberUpdatedState(onLaunch)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    val geometry = MapGeometry(layout, insetTop.toFloat(), insetBottom.toFloat())
    val currentGeometry by rememberUpdatedState(geometry)

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(classifier, camera) {
                hexMapGestures(
                    classifier = classifier,
                    camera = camera,
                    geometry = { currentGeometry },
                    state = { currentState },
                    callbacks = object : HexMapGestureCallbacks {
                        override fun onTap(packageName: String, bounds: Rect) = currentOnLaunch(packageName, bounds)

                        override fun onLongPress(packageName: String) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnLongPress(packageName)
                        }

                        override fun onPreview(value: SwipePreview?) {
                            if (value != null && value.direction != preview?.direction) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            preview = value
                        }

                        override fun onSwipe(packageName: String, bounds: Rect) = currentOnLaunch(packageName, bounds)

                        override fun onFling(velocityPx: Offset) = camera.fling(scope, velocityPx, decay)
                    },
                )
            },
    ) {
        drawHexMap(
            geometry = geometry,
            cameraOffset = camera.offset,
            hexPath = hexPath,
            state = state,
            icons = icons,
            labels = labels,
            preview = preview,
        )
    }
}

/** Converts between screen pixels and hex tiles for the current viewport. */
class MapGeometry(val layout: HexLayout, private val insetTop: Float, private val insetBottom: Float) {

    /** Screen position of world (0,0) before applying the camera: centre of the safe area. */
    fun origin(width: Float, height: Float) = Offset(width / 2f, insetTop + (height - insetTop - insetBottom) / 2f)

    fun hexAt(screen: Offset, origin: Offset, camera: Offset): HexCoord {
        val w = screen - origin - camera
        return layout.worldToHex(Vec2(w.x, w.y))
    }

    fun centerOf(hex: HexCoord, origin: Offset, camera: Offset): Offset {
        val w = layout.hexToWorld(hex)
        return Offset(origin.x + camera.x + w.x, origin.y + camera.y + w.y)
    }

    fun boundsOf(hex: HexCoord, origin: Offset, camera: Offset): Rect {
        val c = centerOf(hex, origin, camera)
        val hw = layout.width / 2f
        val r = layout.radius
        return Rect((c.x - hw).roundToInt(), (c.y - r).roundToInt(), (c.x + hw).roundToInt(), (c.y + r).roundToInt())
    }
}

private fun hexPath(layout: HexLayout): Path = Path().apply {
    layout.corners().forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}
