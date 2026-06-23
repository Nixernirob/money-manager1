package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AureumVectorIcon(
    name: String,
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f

        when (name.lowercase()) {
            "account_balance", "bank" -> {
                // Bank outline
                // Base
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.8f),
                    size = Size(w * 0.8f, h * 0.1f)
                )
                // Roof (triangle)
                val roofPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.9f, h * 0.4f)
                    lineTo(w * 0.1f, h * 0.4f)
                    close()
                }
                drawPath(path = roofPath, color = tint)
                // Columns (3 columns)
                val colW = w * 0.1f
                val colH = h * 0.35f
                val colY = h * 0.45f
                drawRect(color = tint, topLeft = Offset(w * 0.22f, colY), size = Size(colW, colH))
                drawRect(color = tint, topLeft = Offset(w * 0.45f, colY), size = Size(colW, colH))
                drawRect(color = tint, topLeft = Offset(w * 0.68f, colY), size = Size(colW, colH))
            }
            "savings", "vault" -> {
                // Vault safe or Piggy structure with soft curves
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.7f, h * 0.7f),
                    cornerRadius = CornerRadius(w * 0.15f, h * 0.15f),
                    style = Stroke(width = strokeWidth)
                )
                // Vault handle dial
                drawCircle(
                    color = tint,
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.15f,
                    style = Stroke(width = strokeWidth)
                )
                // Spindles
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.25f),
                    end = Offset(w * 0.5f, h * 0.75f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.25f, h * 0.5f),
                    end = Offset(w * 0.75f, h * 0.5f),
                    strokeWidth = strokeWidth
                )
            }
            "credit_card" -> {
                // Outer Card
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.22f),
                    size = Size(w * 0.8f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Magnetic strip
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.33f),
                    size = Size(w * 0.8f, h * 0.12f)
                )
                // Chip node
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.52f),
                    size = Size(w * 0.16f, h * 0.12f),
                    cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
                )
            }
            "shopping_bag", "retail" -> {
                // Bag outline
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.35f),
                    size = Size(w * 0.64f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Handle curve
                val handlePath = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(w * 0.32f, h * 0.15f, w * 0.68f, h * 0.5f),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                }
                drawPath(path = handlePath, color = tint, style = Stroke(width = strokeWidth))
            }
            "restaurant", "dining" -> {
                // Fork details
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.16f), end = Offset(w * 0.32f, h * 0.5f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.22f, h * 0.16f), end = Offset(w * 0.22f, h * 0.36f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.16f), end = Offset(w * 0.42f, h * 0.36f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.22f, h * 0.36f), end = Offset(w * 0.42f, h * 0.36f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.5f), end = Offset(w * 0.32f, h * 0.84f), strokeWidth = strokeWidth)

                // Knife details
                val knifePath = Path().apply {
                    moveTo(w * 0.68f, h * 0.16f)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(w * 0.58f, h * 0.16f, w * 0.78f, h * 0.56f),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(w * 0.68f, h * 0.84f)
                    lineTo(w * 0.64f, h * 0.84f)
                    lineTo(w * 0.64f, h * 0.16f)
                    close()
                }
                drawPath(path = knifePath, color = tint)
            }
            "arrow_upward", "income" -> {
                // Up arrow with beautiful angle
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.84f),
                    end = Offset(w * 0.5f, h * 0.16f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.24f, h * 0.42f),
                    end = Offset(w * 0.5f, h * 0.16f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.76f, h * 0.42f),
                    end = Offset(w * 0.5f, h * 0.16f),
                    strokeWidth = strokeWidth
                )
            }
            "arrow_downward", "expense" -> {
                // Down arrow
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.16f),
                    end = Offset(w * 0.5f, h * 0.84f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.24f, h * 0.58f),
                    end = Offset(w * 0.5f, h * 0.84f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.76f, h * 0.58f),
                    end = Offset(w * 0.5f, h * 0.84f),
                    strokeWidth = strokeWidth
                )
            }
            "transfer" -> {
                // Double horizontal arrow loop
                // Upper right arrow
                drawLine(color = tint, start = Offset(w * 0.16f, h * 0.35f), end = Offset(w * 0.84f, h * 0.35f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.6f, h * 0.18f), end = Offset(w * 0.84f, h * 0.35f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.6f, h * 0.52f), end = Offset(w * 0.84f, h * 0.35f), strokeWidth = strokeWidth)

                // Bottom left arrow
                drawLine(color = tint, start = Offset(w * 0.84f, h * 0.65f), end = Offset(w * 0.16f, h * 0.65f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.4f, h * 0.48f), end = Offset(w * 0.16f, h * 0.65f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.4f, h * 0.82f), end = Offset(w * 0.16f, h * 0.65f), strokeWidth = strokeWidth)
            }
            "dashboard", "home" -> {
                // Grid of 4 rounded rectangles
                val pad = w * 0.08f
                val sizeVal = w * 0.34f
                drawRoundRect(color = tint, topLeft = Offset(pad, pad), size = Size(sizeVal, sizeVal), cornerRadius = CornerRadius(w * 0.06f))
                drawRoundRect(color = tint, topLeft = Offset(w * 0.5f + pad/2f, pad), size = Size(sizeVal, sizeVal), cornerRadius = CornerRadius(w * 0.06f))
                drawRoundRect(color = tint, topLeft = Offset(pad, h * 0.5f + pad/2f), size = Size(sizeVal, sizeVal), cornerRadius = CornerRadius(w * 0.06f))
                drawRoundRect(color = tint, topLeft = Offset(w * 0.5f + pad/2f, h * 0.5f + pad/2f), size = Size(sizeVal, sizeVal), cornerRadius = CornerRadius(w * 0.06f))
            }
            "person", "profile" -> {
                // User avatar silhouette
                drawCircle(
                    color = tint,
                    center = Offset(w * 0.5f, h * 0.33f),
                    radius = w * 0.22f,
                    style = Stroke(width = strokeWidth)
                )
                val bodyPath = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(w * 0.16f, h * 0.55f, w * 0.84f, h * 1.1f),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                }
                drawPath(path = bodyPath, color = tint, style = Stroke(width = strokeWidth))
            }
            "insights", "stats" -> {
                // Polyline chart
                drawLine(color = tint, start = Offset(w * 0.15f, h * 0.72f), end = Offset(w * 0.38f, h * 0.42f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.38f, h * 0.42f), end = Offset(w * 0.61f, h * 0.58f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.61f, h * 0.58f), end = Offset(w * 0.85f, h * 0.22f), strokeWidth = strokeWidth)
                
                // Dots at segments
                drawCircle(color = tint, center = Offset(w * 0.15f, h * 0.72f), radius = w * 0.08f)
                drawCircle(color = tint, center = Offset(w * 0.38f, h * 0.42f), radius = w * 0.08f)
                drawCircle(color = tint, center = Offset(w * 0.61f, h * 0.58f), radius = w * 0.08f)
                drawCircle(color = tint, center = Offset(w * 0.85f, h * 0.22f), radius = w * 0.08f)
            }
            "settings", "more" -> {
                // Modern sliders / dots adjustment dials
                // Line 1
                drawLine(color = tint, start = Offset(w * 0.1f, h * 0.3f), end = Offset(w * 0.9f, h * 0.3f), strokeWidth = strokeWidth)
                drawCircle(color = tint, center = Offset(w * 0.4f, h * 0.3f), radius = w * 0.10f)
                // Line 2
                drawLine(color = tint, start = Offset(w * 0.1f, h * 0.7f), end = Offset(w * 0.9f, h * 0.7f), strokeWidth = strokeWidth)
                drawCircle(color = tint, center = Offset(w * 0.7f, h * 0.7f), radius = w * 0.10f)
            }
            "add", "plus" -> {
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.18f), end = Offset(w * 0.5f, h * 0.82f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.5f), end = Offset(w * 0.82f, h * 0.5f), strokeWidth = strokeWidth)
            }
            "loans" -> {
                // Intersecting link loops or scales
                drawCircle(color = tint, center = Offset(w * 0.36f, h * 0.5f), radius = w * 0.22f, style = Stroke(width = strokeWidth))
                drawCircle(color = tint, center = Offset(w * 0.64f, h * 0.5f), radius = w * 0.22f, style = Stroke(width = strokeWidth))
            }
            "visibility", "eye" -> {
                // Eye contour path
                val eyePath = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.18f, w * 0.85f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.82f, w * 0.15f, h * 0.5f)
                }
                drawPath(path = eyePath, color = tint, style = Stroke(width = strokeWidth))
                // Central pupil
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.16f)
            }
            "visibility_off", "eye_off" -> {
                // Eye contour path
                val eyePath = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.18f, w * 0.85f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.82f, w * 0.15f, h * 0.5f)
                }
                drawPath(path = eyePath, color = tint.copy(alpha = 0.4f), style = Stroke(width = strokeWidth))
                // Central pupil
                drawCircle(color = tint.copy(alpha = 0.4f), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.16f)
                // Diagonal slash
                drawLine(
                    color = tint,
                    start = Offset(w * 0.22f, h * 0.22f),
                    end = Offset(w * 0.78f, h * 0.78f),
                    strokeWidth = strokeWidth
                )
            }
            else -> {
                // Default fallback grid icon
                drawRect(color = tint, topLeft = Offset(w * 0.25f, h * 0.25f), size = Size(w * 0.5f, h * 0.5f), style = Stroke(width = strokeWidth))
            }
        }
    }
}
