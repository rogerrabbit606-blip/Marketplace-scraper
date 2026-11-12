package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FlipScoutColorScheme = darkColorScheme(
    primary = EmeraldFree,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GoldCash,
    onSecondary = Color(0xFF271300),
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = SkyComps,
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = SkyContainer,
    onTertiaryContainer = Color(0xFFBAE6FD),
    error = CoralRed,
    onError = Color(0xFFFFF1F2),
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFFFECDD3),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = SlateElevated,
    outline = SlateBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val FlipScoutShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FlipScoutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlipScoutColorScheme,
        typography = AppTypography,
        shapes = FlipScoutShapes,
        content = content
    )
}
