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
import androidx.compose.ui.graphics.StrokeCap
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
            "dashboard" -> {
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
            "insights", "stats", "pie_chart", "chart", "analytics" -> {
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
            "wallet", "salary" -> {
                // Wallet shape with flap
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.25f),
                    size = Size(w * 0.76f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Flap
                drawPath(
                    path = Path().apply {
                        moveTo(w * 0.5f, h * 0.38f)
                        lineTo(w * 0.78f, h * 0.38f)
                        lineTo(w * 0.78f, h * 0.64f)
                        lineTo(w * 0.5f, h * 0.64f)
                        close()
                    },
                    color = tint,
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(color = tint, center = Offset(w * 0.64f, h * 0.51f), radius = w * 0.04f)
            }
            "hand_coins", "allowance" -> {
                // Friendly hand holding coin representations
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.28f), radius = w * 0.12f, style = Stroke(width = strokeWidth))
                drawCircle(color = tint, center = Offset(w * 0.35f, h * 0.34f), radius = w * 0.1f, style = Stroke(width = strokeWidth))
                // Simple open platform line (hand)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.68f), end = Offset(w * 0.82f, h * 0.68f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.68f), end = Offset(w * 0.34f, h * 0.82f), strokeWidth = strokeWidth)
            }
            "trending_up", "business_profit" -> {
                // Zigzag trend up arrow
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.75f)
                    lineTo(w * 0.42f, h * 0.52f)
                    lineTo(w * 0.62f, h * 0.64f)
                    lineTo(w * 0.85f, h * 0.32f)
                }
                drawPath(path = path, color = tint, style = Stroke(width = strokeWidth))
                // Arrow tip
                drawLine(color = tint, start = Offset(w * 0.64f, h * 0.32f), end = Offset(w * 0.85f, h * 0.32f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.85f, h * 0.32f), end = Offset(w * 0.85f, h * 0.54f), strokeWidth = strokeWidth)
            }
            "gift", "bonus" -> {
                // Gift box and ribbon
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.36f),
                    size = Size(w * 0.64f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                // Ribbon lines
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.36f), end = Offset(w * 0.5f, h * 0.88f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.62f), end = Offset(w * 0.82f, h * 0.62f), strokeWidth = strokeWidth)
                // Bow loops
                drawCircle(color = tint, center = Offset(w * 0.42f, h * 0.28f), radius = w * 0.08f, style = Stroke(width = strokeWidth))
                drawCircle(color = tint, center = Offset(w * 0.58f, h * 0.28f), radius = w * 0.08f, style = Stroke(width = strokeWidth))
            }
            "circle", "other" -> {
                drawCircle(
                    color = tint,
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.28f,
                    style = Stroke(width = strokeWidth)
                )
            }
            "utensils", "food" -> {
                // Utensils base
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.24f),
                    size = Size(w * 0.3f, h * 0.26f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color = tint, start = Offset(w * 0.33f, h * 0.44f), end = Offset(w * 0.33f, h * 0.82f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.24f, h * 0.14f), end = Offset(w * 0.24f, h * 0.28f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.33f, h * 0.14f), end = Offset(w * 0.33f, h * 0.28f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.14f), end = Offset(w * 0.42f, h * 0.28f), strokeWidth = strokeWidth)
                
                // Knife
                val kPath = Path().apply {
                    moveTo(w * 0.68f, h * 0.16f)
                    quadraticBezierTo(w * 0.78f, h * 0.24f, w * 0.68f, h * 0.5f)
                    lineTo(w * 0.68f, h * 0.82f)
                }
                drawPath(path = kPath, color = tint, style = Stroke(width = strokeWidth))
            }
            "shirt", "apparel" -> {
                val sPath = Path().apply {
                    moveTo(w * 0.34f, h * 0.22f)
                    lineTo(w * 0.22f, h * 0.34f)
                    lineTo(w * 0.12f, h * 0.31f)
                    lineTo(w * 0.22f, h * 0.56f)
                    lineTo(w * 0.32f, h * 0.51f)
                    lineTo(w * 0.32f, h * 0.84f)
                    lineTo(w * 0.68f, h * 0.84f)
                    lineTo(w * 0.68f, h * 0.51f)
                    lineTo(w * 0.78f, h * 0.56f)
                    lineTo(w * 0.88f, h * 0.31f)
                    lineTo(w * 0.78f, h * 0.34f)
                    lineTo(w * 0.66f, h * 0.22f)
                    quadraticBezierTo(w * 0.5f, h * 0.34f, w * 0.34f, h * 0.22f)
                    close()
                }
                drawPath(path = sPath, color = tint, style = Stroke(width = strokeWidth))
            }
            "home", "house" -> {
                // Roof outline
                val rPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.44f)
                    lineTo(w * 0.12f, h * 0.44f)
                    close()
                }
                drawPath(path = rPath, color = tint, style = Stroke(width = strokeWidth))
                // Frame outline
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.44f),
                    size = Size(w * 0.56f, h * 0.42f),
                    style = Stroke(width = strokeWidth)
                )
                // Door outline
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.62f),
                    size = Size(w * 0.16f, h * 0.24f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "building", "rent" -> {
                // High-rise building
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.25f, h * 0.16f),
                    size = Size(w * 0.5f, h * 0.7f),
                    style = Stroke(width = strokeWidth)
                )
                // Door
                drawRect(color = tint, topLeft = Offset(w * 0.44f, h * 0.72f), size = Size(w * 0.12f, h * 0.14f))
                // Windows
                drawCircle(color = tint, center = Offset(w * 0.38f, h * 0.3f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.62f, h * 0.3f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.38f, h * 0.45f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.62f, h * 0.45f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.38f, h * 0.6f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.62f, h * 0.6f), radius = w * 0.04f)
            }
            "wifi", "internet_bill" -> {
                // Dot
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.78f), radius = w * 0.07f)
                // Arc 1
                drawArc(
                    color = tint,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.52f),
                    size = Size(w * 0.32f, h * 0.32f),
                    style = Stroke(width = strokeWidth)
                )
                // Arc 2
                drawArc(
                    color = tint,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.32f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "bus", "transport" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.24f),
                    size = Size(w * 0.7f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Windows line
                drawLine(color = tint, start = Offset(w * 0.15f, h * 0.45f), end = Offset(w * 0.85f, h * 0.45f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.24f), end = Offset(w * 0.5f, h * 0.45f), strokeWidth = strokeWidth)
                // Wheels
                drawCircle(color = tint, center = Offset(w * 0.32f, h * 0.76f), radius = w * 0.09f)
                drawCircle(color = tint, center = Offset(w * 0.68f, h * 0.76f), radius = w * 0.09f)
            }
            "heart_pulse", "health" -> {
                // Heart shape outline
                val heartPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.28f)
                    cubicTo(w * 0.34f, h * 0.12f, w * 0.12f, h * 0.26f, w * 0.12f, h * 0.46f)
                    cubicTo(w * 0.12f, h * 0.68f, w * 0.34f, h * 0.82f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.66f, h * 0.82f, w * 0.88f, h * 0.68f, w * 0.88f, h * 0.46f)
                    cubicTo(w * 0.88f, h * 0.26f, w * 0.66f, h * 0.12f, w * 0.5f, h * 0.28f)
                    close()
                }
                drawPath(path = heartPath, color = tint, style = Stroke(width = strokeWidth))
            }
            "sparkles", "beauty" -> {
                // 3 stars
                fun drawStar(cx: Float, cy: Float, r: Float) {
                    val p = Path().apply {
                        moveTo(cx, cy - r)
                        quadraticBezierTo(cx, cy, cx + r, cy)
                        quadraticBezierTo(cx, cy, cx, cy + r)
                        quadraticBezierTo(cx, cy, cx - r, cy)
                        quadraticBezierTo(cx, cy, cx, cy - r)
                        close()
                    }
                    drawPath(path = p, color = tint)
                }
                drawStar(w * 0.48f, h * 0.33f, w * 0.16f)
                drawStar(w * 0.24f, h * 0.56f, w * 0.10f)
                drawStar(w * 0.74f, h * 0.52f, w * 0.12f)
            }
            "graduation_cap", "education" -> {
                // Diamond top
                val dPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.22f)
                    lineTo(w * 0.86f, h * 0.42f)
                    lineTo(w * 0.5f, h * 0.62f)
                    lineTo(w * 0.14f, h * 0.42f)
                    close()
                }
                drawPath(path = dPath, color = tint)
                // Cap base under it
                val bPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.55f)
                    lineTo(w * 0.28f, h * 0.72f)
                    quadraticBezierTo(w * 0.5f, h * 0.88f, w * 0.72f, h * 0.72f)
                    lineTo(w * 0.72f, h * 0.55f)
                }
                drawPath(path = bPath, color = tint, style = Stroke(width = strokeWidth))
                // Tassel line
                drawLine(color = tint, start = Offset(w * 0.86f, h * 0.42f), end = Offset(w * 0.88f, h * 0.65f), strokeWidth = strokeWidth * 0.8f)
                drawCircle(color = tint, center = Offset(w * 0.88f, h * 0.7f), radius = w * 0.04f)
            }
            "gamepad", "entertainment" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.3f),
                    size = Size(w * 0.76f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.12f),
                    style = Stroke(width = strokeWidth)
                )
                // D-pad cross on left
                drawLine(color = tint, start = Offset(w * 0.28f, h * 0.51f), end = Offset(w * 0.44f, h * 0.51f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.36f, h * 0.43f), end = Offset(w * 0.36f, h * 0.59f), strokeWidth = strokeWidth)
                // Buttons on right
                drawCircle(color = tint, center = Offset(w * 0.62f, h * 0.51f), radius = w * 0.045f)
                drawCircle(color = tint, center = Offset(w * 0.72f, h * 0.51f), radius = w * 0.045f)
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
            "contactless", "waves" -> {
                // Contactless 3 concentric wave arcs
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(-w * 0.1f, h * 0.1f),
                    size = Size(w * 0.5f, h * 0.8f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.22f),
                    size = Size(w * 0.5f, h * 0.56f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.4f, h * 0.34f),
                    size = Size(w * 0.45f, h * 0.32f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            "bell", "notification" -> {
                // Sleek notification bell outline
                val bellPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    cubicTo(w * 0.35f, h * 0.15f, w * 0.25f, h * 0.35f, w * 0.25f, h * 0.58f)
                    lineTo(w * 0.15f, h * 0.72f)
                    lineTo(w * 0.85f, h * 0.72f)
                    lineTo(w * 0.75f, h * 0.58f)
                    cubicTo(w * 0.75f, h * 0.35f, w * 0.65f, h * 0.15f, w * 0.5f, h * 0.15f)
                    close()
                }
                drawPath(path = bellPath, color = tint, style = Stroke(width = strokeWidth))
                // Bell clapper
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.4f, h * 0.74f),
                    size = Size(w * 0.2f, h * 0.14f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "btc", "bitcoin" -> {
                // Bitcoin B with two vertical notches
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.44f, style = Stroke(width = strokeWidth))
                // B symbol
                drawLine(color = tint, start = Offset(w * 0.38f, h * 0.25f), end = Offset(w * 0.38f, h * 0.75f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.45f, h * 0.18f), end = Offset(w * 0.45f, h * 0.82f), strokeWidth = strokeWidth * 0.7f)
                drawLine(color = tint, start = Offset(w * 0.55f, h * 0.18f), end = Offset(w * 0.55f, h * 0.82f), strokeWidth = strokeWidth * 0.7f)
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.38f, h * 0.28f),
                    size = Size(w * 0.26f, h * 0.24f),
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.38f, h * 0.48f),
                    size = Size(w * 0.30f, h * 0.26f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "eth", "ethereum" -> {
                // Diamond top & bottom
                val topPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.52f)
                    lineTo(w * 0.5f, h * 0.65f)
                    lineTo(w * 0.18f, h * 0.52f)
                    close()
                }
                drawPath(path = topPath, color = tint, style = Stroke(width = strokeWidth))
                val botPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.7f)
                    lineTo(w * 0.82f, h * 0.56f)
                    lineTo(w * 0.5f, h * 0.9f)
                    lineTo(w * 0.18f, h * 0.56f)
                    close()
                }
                drawPath(path = botPath, color = tint, style = Stroke(width = strokeWidth))
            }
            "sol", "solana" -> {
                // 3 slanted gradient bars
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.3f), end = Offset(w * 0.8f, h * 0.3f), strokeWidth = strokeWidth * 1.3f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.25f, h * 0.5f), end = Offset(w * 0.75f, h * 0.5f), strokeWidth = strokeWidth * 1.3f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.7f), end = Offset(w * 0.8f, h * 0.7f), strokeWidth = strokeWidth * 1.3f, cap = StrokeCap.Round)
            }
            "usdc" -> {
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.42f, style = Stroke(width = strokeWidth))
                // S shape
                drawArc(
                    color = tint,
                    startAngle = 90f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(w * 0.36f, h * 0.3f),
                    size = Size(w * 0.28f, h * 0.22f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(w * 0.36f, h * 0.48f),
                    size = Size(w * 0.28f, h * 0.22f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            "hexagon_gear", "gear_dock" -> {
                // Modern hexagonal settings dock icon
                val hexPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.86f, h * 0.31f)
                    lineTo(w * 0.86f, h * 0.69f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.14f, h * 0.69f)
                    lineTo(w * 0.14f, h * 0.31f)
                    close()
                }
                drawPath(path = hexPath, color = tint, style = Stroke(width = strokeWidth))
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.15f, style = Stroke(width = strokeWidth))
            }
            "cards_dock" -> {
                // Dual layered cards dock icon
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.34f), end = Offset(w * 0.86f, h * 0.34f), strokeWidth = strokeWidth)
            }
            "gift_dock" -> {
                // Clean gift box outline
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.38f),
                    size = Size(w * 0.64f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Ribbon
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.86f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.58f), end = Offset(w * 0.82f, h * 0.58f), strokeWidth = strokeWidth)
                // Loops
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.22f),
                    size = Size(w * 0.22f, h * 0.18f),
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.5f, h * 0.22f),
                    size = Size(w * 0.22f, h * 0.18f),
                    style = Stroke(width = strokeWidth)
                )
            }
            "wallet_dock" -> {
                // Clean solid or outlined wallet icon
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.22f),
                    size = Size(w * 0.70f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                // Pocket flap
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.55f, h * 0.38f),
                    size = Size(w * 0.32f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(color = tint, center = Offset(w * 0.73f, h * 0.5f), radius = w * 0.035f)
            }
            "fingerprint", "biometric" -> {
                // Biometric fingerprint ridges
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.5f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.32f, h * 0.28f),
                    size = Size(w * 0.36f, h * 0.34f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Left & Right descending ridge lines
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.4f), end = Offset(w * 0.2f, h * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.45f), end = Offset(w * 0.32f, h * 0.82f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.45f), end = Offset(w * 0.5f, h * 0.88f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.68f, h * 0.45f), end = Offset(w * 0.68f, h * 0.82f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.8f, h * 0.4f), end = Offset(w * 0.8f, h * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "calendar", "date" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.2f),
                    size = Size(w * 0.7f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color = tint, start = Offset(w * 0.15f, h * 0.4f), end = Offset(w * 0.85f, h * 0.4f), strokeWidth = strokeWidth)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.12f), end = Offset(w * 0.32f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.68f, h * 0.12f), end = Offset(w * 0.68f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawCircle(color = tint, center = Offset(w * 0.35f, h * 0.55f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.65f, h * 0.55f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.35f, h * 0.72f), radius = w * 0.04f)
                drawCircle(color = tint, center = Offset(w * 0.65f, h * 0.72f), radius = w * 0.04f)
            }
            "clock", "time" -> {
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.4f, style = Stroke(width = strokeWidth))
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.5f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.72f, h * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "palette", "color" -> {
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.4f, style = Stroke(width = strokeWidth))
                drawCircle(color = tint, center = Offset(w * 0.35f, h * 0.35f), radius = w * 0.06f)
                drawCircle(color = tint, center = Offset(w * 0.65f, h * 0.35f), radius = w * 0.06f)
                drawCircle(color = tint, center = Offset(w * 0.35f, h * 0.65f), radius = w * 0.06f)
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
            "receipt_long", "ledger", "statement" -> {
                // Receipt sheet with zigzag or clean edges
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.7f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                // Receipt lines
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.32f), end = Offset(w * 0.68f, h * 0.32f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.48f), end = Offset(w * 0.68f, h * 0.48f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.64f), end = Offset(w * 0.52f, h * 0.64f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "drag_handle", "reorder", "drag_indicator" -> {
                // 3 horizontal bars for reorder handle
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.32f), end = Offset(w * 0.8f, h * 0.32f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.5f), end = Offset(w * 0.8f, h * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.2f, h * 0.68f), end = Offset(w * 0.8f, h * 0.68f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "file_upload", "export", "backup" -> {
                // Up arrow out of a tray
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.55f),
                    size = Size(w * 0.6f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.65f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.35f, h * 0.35f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.65f, h * 0.35f), end = Offset(w * 0.5f, h * 0.2f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "file_download", "import", "restore" -> {
                // Down arrow into a tray
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.55f),
                    size = Size(w * 0.6f, h * 0.3f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.2f), end = Offset(w * 0.5f, h * 0.62f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.35f, h * 0.47f), end = Offset(w * 0.5f, h * 0.62f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.65f, h * 0.47f), end = Offset(w * 0.5f, h * 0.62f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "arrow_up" -> {
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.75f), end = Offset(w * 0.5f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.3f, h * 0.45f), end = Offset(w * 0.5f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.7f, h * 0.45f), end = Offset(w * 0.5f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            "arrow_down" -> {
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.25f), end = Offset(w * 0.5f, h * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.3f, h * 0.55f), end = Offset(w * 0.5f, h * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.7f, h * 0.55f), end = Offset(w * 0.5f, h * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
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
            "dark_mode", "moon", "night" -> {
                // Crescent moon
                val moonPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    cubicTo(w * 0.25f, h * 0.25f, w * 0.25f, h * 0.75f, w * 0.55f, h * 0.85f)
                    cubicTo(w * 0.75f, h * 0.85f, w * 0.85f, h * 0.7f, w * 0.85f, h * 0.7f)
                    cubicTo(w * 0.6f, h * 0.7f, w * 0.45f, h * 0.45f, w * 0.5f, h * 0.15f)
                    close()
                }
                drawPath(path = moonPath, color = tint)
            }
            "light_mode", "sun", "day" -> {
                // Sun center circle
                drawCircle(color = tint, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.22f)
                // 8 sun rays
                val rayLen = w * 0.12f
                val rayDist = w * 0.35f
                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians(angle.toDouble())
                    val startX = (w * 0.5f + Math.cos(rad) * rayDist).toFloat()
                    val startY = (h * 0.5f + Math.sin(rad) * rayDist).toFloat()
                    val endX = (w * 0.5f + Math.cos(rad) * (rayDist + rayLen)).toFloat()
                    val endY = (h * 0.5f + Math.sin(rad) * (rayDist + rayLen)).toFloat()
                    drawLine(color = tint, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
                }
            }
            else -> {
                // Default fallback grid icon
                drawRect(color = tint, topLeft = Offset(w * 0.25f, h * 0.25f), size = Size(w * 0.5f, h * 0.5f), style = Stroke(width = strokeWidth))
            }
        }
    }
}
