package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DataLargeStyle
import com.example.ui.theme.DataSmallStyle
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceFill
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.MidnightPrimary

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.AureumThemeStyles
import com.example.ui.theme.aureumGlass
import com.example.ui.theme.contentPrimary
import com.example.ui.theme.contentSecondary
import com.example.ui.theme.headingColor
import com.example.ui.theme.framerHoverEffect

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderAccent: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable ColumnScope.() -> Unit
) {
    val specs = AureumThemeStyles.specs
    val grainColor = if (specs.isDark) Color.White.copy(alpha = 0.015f) else Color.Black.copy(alpha = 0.008f)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = modifier
            .framerHoverEffect(interactionSource)
            .aureumGlass(
                shape = RoundedCornerShape(24.dp),
                borderWidth = 1.dp,
                hasNeonGlow = false
            )
            .drawBehind {
                // Low-frequency subtle micro grain texture using noise points
                val step = 12
                val countX = (size.width / step).toInt()
                val countY = (size.height / step).toInt()
                
                for (x in 0..countX) {
                    for (y in 0..countY) {
                        val seed = (x * 12.9898f + y * 78.233f)
                        val rand = (Math.sin(seed.toDouble()) * 43758.5453f).let { it - Math.floor(it) }
                        if (rand > 0.85) {
                            drawCircle(
                                color = grainColor,
                                radius = 1f,
                                center = Offset(x * step.toFloat(), y * step.toFloat())
                            )
                        }
                    }
                }
            }
            .padding(20.dp)
    ) {
        Column {
            content()
        }
    }
}

data class PieSegment(
    val category: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutChart(
    segments: List<PieSegment>,
    modifier: Modifier = Modifier.size(200.dp),
    centerTextLabel: String = "Top Category",
    centerTextVal: String = ""
) {
    val specs = AureumThemeStyles.specs
    if (segments.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data", color = specs.contentSecondary)
        }
        return
    }

    val total = segments.sumOf { it.value }.toFloat()
    
    // Animate drawing percentages
    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(segments) {
        isAnimated = true
    }
    
    val sweepAnimationState by animateFloatAsState(
        targetValue = if (isAnimated) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spaceWidth = this.size.width
            val strokeW = spaceWidth * 0.13f
            val radius = (spaceWidth - strokeW) / 2f
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)

            // Draw base tracking background loop
            drawCircle(
                color = if (specs.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeW)
            )

            var startAngle = -90f
            segments.forEach { segment ->
                val percentage = if (total > 0) (segment.value / total).toFloat() else 0f
                val sweepAngle = percentage * 360f

                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * sweepAnimationState,
                    useCenter = false,
                    topLeft = Offset(strokeW / 2f, strokeW / 2f),
                    size = Size(this.size.width - strokeW, this.size.height - strokeW),
                    style = Stroke(width = strokeW)
                )
                startAngle += sweepAngle
            }
        }

        // Center typography layout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTextLabel,
                color = specs.contentSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = centerTextVal,
                color = specs.contentPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TrendLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier.fillMaxWidth().height(140.dp),
    accentColor: Color = Color.Unspecified
) {
    val specs = AureumThemeStyles.specs
    val actualAccent = if (accentColor == Color.Unspecified) specs.headingColor else accentColor

    if (values.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No transactions logged", color = specs.contentSecondary)
        }
        return
    }

    val maxVal = maxOf(1f, values.maxOrNull() ?: 100f)

    var triggerAnimate by remember { mutableStateOf(false) }
    LaunchedEffect(values) {
        triggerAnimate = true
    }

    val progress by animateFloatAsState(
        targetValue = if (triggerAnimate) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1200)
    )

    Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        val pointCount = values.size
        
        // Draw bottom zero axis line
        drawLine(
            color = if (specs.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 2f
        )

        if (pointCount < 2) {
            // Draw simple single dot or line representation
            drawCircle(
                color = actualAccent,
                radius = 8f,
                center = Offset(w / 2f, h / 2f)
            )
            return@Canvas
        }

        val colWidth = w / (pointCount - 1)
        val path = Path()

        for (i in 0 until pointCount) {
            val progressY = (values[i] / maxVal) * h
            val x = i * colWidth
            // Invert Y coordinate because 0 is at top
            val y = h - (progressY * progress)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw glowing line stroke path
        drawPath(
            path = path,
            color = actualAccent,
            style = Stroke(width = 4f)
        )

        // Draw visual vertical markers and text labels at bottom
        val stepDiv = maxOf(1, pointCount / 4)
        for (i in 0 until pointCount step stepDiv) {
            val labelX = i * colWidth
            drawLine(
                color = if (specs.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
                start = Offset(labelX, 0f),
                end = Offset(labelX, h),
                strokeWidth = 2f
            )
        }
    }
}
