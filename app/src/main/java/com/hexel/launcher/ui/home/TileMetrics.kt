package com.hexel.launcher.ui.home

/**
 * Tile geometry in dp, measured from the tile centre. A pointy-top hex with radius 44dp is
 * 76dp wide and 88dp tall, i.e. about five columns on a typical phone.
 */
object TileMetrics {
    const val RadiusDp = 44f
    const val IconSizeDp = 38f
    /** Vertical offset of the icon centre (negative = up) to leave room for the label. */
    const val IconCenterYDp = -7f
    const val LabelTopDp = 14f
    const val LabelMaxWidthDp = 58f
    const val BorderWidthDp = 1f
}
