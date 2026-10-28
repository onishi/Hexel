package com.hexel.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** "Paper" palette. */
object HexelColors {
    val Primary = Color(0xFF4A90E2)
    val Accent = Color(0xFF7ED3F4)
    val Surface = Color(0xFFF8FAFC)
    val Border = Color(0xFFE4E9F0)
    val Text = Color(0xFF1F2937)
    val Tile = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF6B7280)

    /** Pale blue used for the swipe direction highlight. */
    val Highlight = Color(0x2E4A90E2)
    val HighlightTile = Color(0xFFEAF4FD)
}

/**
 * The system sans-serif is used on purpose: on Android it is Roboto for Latin text (an Inter-like
 * neo-grotesque) and Noto Sans CJK for Japanese, so no font files need to be bundled.
 */
private val HexelFont = FontFamily.SansSerif

private val HexelTypography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontFamily = HexelFont, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = HexelFont, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = HexelFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = HexelFont),
        bodySmall = base.bodySmall.copy(fontFamily = HexelFont),
        labelLarge = base.labelLarge.copy(fontFamily = HexelFont),
        labelSmall = base.labelSmall.copy(fontFamily = HexelFont),
    )
}

/** Style of the app name under each tile icon. */
val TileLabelStyle = TextStyle(
    fontFamily = HexelFont,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    color = HexelColors.Text,
)

@Composable
fun HexelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HexelColors.Primary,
            onPrimary = Color.White,
            secondary = HexelColors.Accent,
            background = HexelColors.Surface,
            onBackground = HexelColors.Text,
            surface = HexelColors.Surface,
            onSurface = HexelColors.Text,
            surfaceVariant = HexelColors.Tile,
            onSurfaceVariant = HexelColors.TextMuted,
            outline = HexelColors.Border,
            outlineVariant = HexelColors.Border,
        ),
        typography = HexelTypography,
        content = content,
    )
}
