package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PremiumDarkColorScheme = darkColorScheme(
    primary = NeonLime,
    secondary = TextSecondary,
    tertiary = NeonLimeLight,
    background = CanvasBgDark,
    surface = SurfaceContainer,
    onPrimary = CardBlack,
    onSecondary = Color.White,
    onTertiary = CardBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Color(0x26FFFFFF),
    error = ExpenseCoral,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = CardBlack,
    secondary = TextMuted,
    tertiary = NeonLime,
    background = CanvasBgLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = CardBlack,
    onBackground = TextCharcoal,
    onSurface = TextCharcoal,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFFEFF1F5),
    error = ExpenseCoral,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF3F4F6),
    surfaceContainerHighest = Color(0xFFE5E7EB)
)

@Composable
fun AureumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme

    val specs = if (darkTheme) {
        AureumStyleSpecs(
            isDark = true,
            glassBackground = GlassSurfaceFill,
            glassBorderColor = GlassBorderTop,
            glassBorderBrush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
            ),
            neonGlowColor = NeonLime,
            secondaryNeonGlow = NeonLimeDark,
            frostedBlurRadius = 16.dp,
            subtleShadowColor = Color.Black.copy(alpha = 0.4f)
        )
    } else {
        AureumStyleSpecs(
            isDark = false,
            glassBackground = Color.White,
            glassBorderColor = Color(0xFFEFF1F5),
            glassBorderBrush = Brush.verticalGradient(
                listOf(Color(0xFFEFF1F5), Color(0xFFE5E7EB))
            ),
            neonGlowColor = NeonLime,
            secondaryNeonGlow = NeonLimeLight,
            frostedBlurRadius = 8.dp,
            subtleShadowColor = Color.Black.copy(alpha = 0.04f)
        )
    }

    CompositionLocalProvider(LocalAureumStyleSpecs provides specs) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

