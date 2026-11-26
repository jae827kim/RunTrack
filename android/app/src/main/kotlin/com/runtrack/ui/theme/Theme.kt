package com.runtrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtrack.R

object RunTrackColors {
    val Accent = Color(0xFF197757)
    val Mint = Color(0xFF2ADAA5)
    val SoftAccent = Color(0xFFE7F3EC)
    val Background = Color(0xFFF6F8F7)
    val Surface = Color.White
    val Field = Color(0xFFF8FAF9)
    val Text = Color(0xFF192D24)
    val Muted = Color(0xFF68766E)
    val Outline = Color(0xFFE1E7E3)
    val Selection = Color(0xFFEBEEEC)
    val Warning = Color(0xFF946222)
    val WarningSurface = Color(0xFFFFF3DF)
}

private val Inter = FontFamily(Font(R.font.inter_regular), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_bold, FontWeight.Bold))
private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

private val AppTypography = Typography(
    displayLarge = type(40, 48, FontWeight.Bold), displayMedium = type(36, 44, FontWeight.Bold), displaySmall = type(32, 40, FontWeight.Bold),
    headlineLarge = type(30, 38, FontWeight.Bold), headlineMedium = type(26, 34, FontWeight.Bold), headlineSmall = type(24, 32, FontWeight.Bold),
    titleLarge = type(20, 28, FontWeight.Bold), titleMedium = type(16, 24, FontWeight.Medium), titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24), bodyMedium = type(14, 22), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.Medium), labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(11, 16, FontWeight.Medium)
)

@Composable
fun RunTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RunTrackColors.Accent, onPrimary = Color.White,
            primaryContainer = RunTrackColors.SoftAccent, onPrimaryContainer = RunTrackColors.Accent,
            secondary = RunTrackColors.Accent, secondaryContainer = RunTrackColors.Selection,
            background = RunTrackColors.Background, onBackground = RunTrackColors.Text,
            surface = RunTrackColors.Surface, onSurface = RunTrackColors.Text,
            surfaceVariant = RunTrackColors.Field, onSurfaceVariant = RunTrackColors.Muted,
            outline = RunTrackColors.Outline, outlineVariant = RunTrackColors.Outline,
            error = Color(0xFFBA433D), errorContainer = Color(0xFFFCEDEA)
        ),
        typography = AppTypography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content
    )
}
