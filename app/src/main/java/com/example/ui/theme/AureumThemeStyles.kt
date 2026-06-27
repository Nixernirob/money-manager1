package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember

@Immutable
data class AureumStyleSpecs(
    val isDark: Boolean,
    val glassBackground: Color,
    val glassBorderColor: Color,
    val glassBorderBrush: Brush,
    val neonGlowColor: Color,
    val secondaryNeonGlow: Color,
    val frostedBlurRadius: Dp,
    val subtleShadowColor: Color
)

val LocalAureumStyleSpecs = staticCompositionLocalOf<AureumStyleSpecs> {
    error("No AureumStyleSpecs provided")
}

object AureumThemeStyles {
    val specs: AureumStyleSpecs
        @Composable
        get() = LocalAureumStyleSpecs.current
}

// Global Core Palette constants to avoid reliance on composable context
val AureumMidnightPrimaryDark = Color(0xFFC1C5DC)
val AureumMidnightPrimaryLight = Color(0xFF005AC1)

val AureumMidnightSecondaryDark = Color(0xFFBDC5E7)
val AureumMidnightSecondaryLight = Color(0xFF74777F)

val AureumMidnightTertiaryDark = Color(0xFFBBC4F2)
val AureumMidnightTertiaryLight = Color(0xFF4C662B)

val AureumStyleSpecs.contentPrimary: Color
    get() = if (isDark) Color.White else Color(0xFF0F1524)

val AureumStyleSpecs.contentSecondary: Color
    get() = if (isDark) Color(0xFFC7C6CD) else Color(0xFF535864)

val AureumStyleSpecs.inactiveContainer: Color
    get() = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

val AureumStyleSpecs.inactiveContainerBorder: Color
    get() = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

val AureumStyleSpecs.headingColor: Color
    get() = if (isDark) AureumMidnightPrimaryDark else AureumMidnightPrimaryLight

val AureumStyleSpecs.activeContainer: Color
    get() = if (isDark) AureumMidnightPrimaryDark else AureumMidnightPrimaryLight

val AureumStyleSpecs.onActiveContainer: Color
    get() = if (isDark) Color.Black else Color.White


/**
 * Applies a premium frosted glassmorphic design to any Composable layout.
 * Combines highly tuned gradient translucent fills, high-contrast border highlights (chamfered lighting),
 * and dynamic animations.
 */
@Composable
fun Modifier.aureumGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    hasNeonGlow: Boolean = false,
    glowIntensity: Float = 0.25f
): Modifier {
    val specs = AureumThemeStyles.specs
    
    // Dynamic looping pulse for neon backdrop highlights
    val infiniteTransition = rememberInfiniteTransition(label = "NeonPulsing")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = glowIntensity * 0.7f,
        targetValue = glowIntensity * 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    return this
        .then(
            if (hasNeonGlow) {
                Modifier.drawBehind {
                    // Draw dynamic back neon glow spot using radial shader
                    val scale = size.minDimension * 0.75f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                specs.neonGlowColor.copy(alpha = pulseAlpha),
                                specs.secondaryNeonGlow.copy(alpha = pulseAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = scale
                        ),
                        radius = scale,
                        center = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                }
            } else Modifier
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    specs.glassBackground,
                    specs.glassBackground.copy(alpha = 0.5f)
                )
            )
        )
        .border(
            width = borderWidth,
            brush = specs.glassBorderBrush,
            shape = shape
        )
}

/**
 * Renders a premium neon-glow border shadow matching Aureum specification.
 */
@Composable
fun Modifier.aureumNeonGlow(
    color: Color = AureumThemeStyles.specs.neonGlowColor,
    radius: Dp = 8.dp,
    cornerRadius: Dp = 24.dp
): Modifier {
    return this.drawBehind {
        val radPx = radius.toPx()
        val cornerPx = cornerRadius.toPx()
        
        // Simulating layered soft glow shadow borders on custom shapes
        drawRoundRect(
            color = color.copy(alpha = 0.08f),
            size = size,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
        drawRoundRect(
            color = color.copy(alpha = 0.04f),
            size = size.copy(width = size.width + radPx, height = size.height + radPx),
            topLeft = Offset(-radPx / 2f, -radPx / 2f),
            cornerRadius = CornerRadius(cornerPx + radPx / 2f, cornerPx + radPx / 2f)
        )
    }
}

/**
 * Renders a highly polished, professional background using a professional mix of soft gradients and radiant neon-glow spots.
 * This guarantees the frosted-glass cards contrast impeccably and pop cleanly in both light and dark modes.
 */
@Composable
fun Modifier.aureumBackground(): Modifier {
    val specs = AureumThemeStyles.specs
    
    if (specs.isDark) {
        return this.drawBehind {
            // Elegant Cosmic dark obsidian base
            drawRect(color = Color(0xFF0F131E))
            
            // Ultra-subtle deep sapphire blue left accent spot
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF14244B), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.4f),
                    radius = size.width * 1.5f
                ),
                radius = size.width * 1.5f,
                center = Offset(size.width * 0.1f, size.height * 0.4f)
            )

            // Dynamic pulsing or glow spots for depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF381F3E), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.15f),
                    radius = size.width * 1.2f
                ),
                radius = size.width * 1.2f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )

            // Warm bottom copper/coal center ambient warmth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2218), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.9f),
                    radius = size.width * 1.3f
                ),
                radius = size.width * 1.3f,
                center = Offset(size.width * 0.5f, size.height * 0.9f)
            )
        }
    } else {
        return this.drawBehind {
            // Premium professional light theme steel background (replaces generic white / off-white)
            // Soft slate-silver-blue that allows white glass to shine
            drawRect(color = Color(0xFFE2E7F3))
            
            // Top Right: Radiant Lavender/Iris soft flow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD6C8F6).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.12f),
                    radius = size.width * 1.2f
                ),
                radius = size.width * 1.2f,
                center = Offset(size.width * 0.95f, size.height * 0.12f)
            )

            // Top Left: Glowing Aquamarine / Air Mint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBBE5F1).copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.25f),
                    radius = size.width * 1.1f
                ),
                radius = size.width * 1.1f,
                center = Offset(size.width * 0.05f, size.height * 0.25f)
            )

            // Bottom Right: Warm Amber/Sunrise Peach
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBDDC5).copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.width * 1.3f
                ),
                radius = size.width * 1.3f,
                center = Offset(size.width * 0.85f, size.height * 0.85f)
            )

            // Central: Deep Royal Cobalt Glow (Professional base warmth)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCAD8FF).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = size.width * 1.4f
                ),
                radius = size.width * 1.4f,
                center = Offset(size.width * 0.5f, size.height * 0.55f)
            )
        }
    }
}

/**
 * Custom Jetpack Compose modifier that implements a Framer Motion-like interactive scale and hover glow effect
 * on click/touch/pointer-enter events to elevate the premium glassmorphic feel of cards and buttons.
 */
@Composable
fun Modifier.framerHoverEffect(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            isHovered -> 1.03f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "framer_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .hoverable(interactionSource)
}
