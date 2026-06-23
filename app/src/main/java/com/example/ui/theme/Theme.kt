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
    primary = MidnightPrimary,
    secondary = MidnightSecondary,
    tertiary = MidnightTertiary,
    background = BaseMidnightOffset,
    surface = BaseMidnightOffset,
    onPrimary = Color(0xFF161B2B),
    onSecondary = Color(0xFF272F4A),
    onTertiary = Color(0xFF1E284C),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = OutlineM3,
    error = ExpenseCoral,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = Color(0xFF005AC1),
    secondary = Color(0xFF74777F),
    tertiary = Color(0xFFD1E1FF),
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF001D36),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFE0E2EC),
    error = Color(0xFFBA1A1A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F3F6),
    surfaceContainer = Color(0xFFEEEEF2),
    surfaceContainerHigh = Color(0xFFE8E8EC),
    surfaceContainerHighest = Color(0xFFE2E2E6)
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
                listOf(Color.White.copy(alpha = 0.15f), MidnightSecondary.copy(alpha = 0.30f))
            ),
            neonGlowColor = MidnightTertiary,
            secondaryNeonGlow = MidnightPrimary,
            frostedBlurRadius = 16.dp,
            subtleShadowColor = Color.Black.copy(alpha = 0.4f)
        )
    } else {
        AureumStyleSpecs(
            isDark = false,
            glassBackground = Color(0xE6FFFFFF),
            glassBorderColor = Color(0xFFE0E2EC).copy(alpha = 0.6f),
            glassBorderBrush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.8f), Color(0xFFE0E2EC).copy(alpha = 0.5f))
            ),
            neonGlowColor = Color(0xFF005AC1),
            secondaryNeonGlow = Color(0xFFD1E1FF),
            frostedBlurRadius = 12.dp,
            subtleShadowColor = Color(0xFFE0E2EC).copy(alpha = 0.25f)
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
