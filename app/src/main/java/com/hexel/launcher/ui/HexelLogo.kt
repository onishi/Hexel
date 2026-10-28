package com.hexel.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexLayout
import com.hexel.launcher.ui.theme.HexelColors

/** The "Cluster" mark: one hexagon on top of two, touching edge to edge. */
@Composable
fun HexelLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // Cluster bounding box is 2*sqrt(3)*r wide and 3.5*r tall.
        val r = minOf(size.width / (2f * 1.7320508f), size.height / 3.5f)
        val layout = HexLayout(r)
        val tiles = listOf(
            HexCoord(0, 0) to HexelColors.Accent,
            HexCoord(1, 0) to Color(0xFF63B1EC),
            HexCoord(1, -1) to HexelColors.Primary,
        )
        // Centre the cluster: bottom-left hex centre is at (0,0); cluster centre is (w/2, -0.75r).
        val origin = Offset(size.width / 2f - layout.width / 2f, size.height / 2f + 0.75f * r)
        val corners = layout.corners()
        for ((hex, color) in tiles) {
            val c = layout.hexToWorld(hex)
            val path = Path().apply {
                corners.forEachIndexed { i, p ->
                    val x = origin.x + c.x + p.x
                    val y = origin.y + c.y + p.y
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }
    }
}
