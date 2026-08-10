package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Canvas-based real-time line chart component
 * specifically designed for rendering magnetic field intensity fluctuations (µT)
 * as live sensor data updates.
 */
@Composable
fun RealtimeLineChart(
    history: List<Float>,
    warningThreshold: Float = 80f,
    emfUnit: com.example.data.EmfUnit = com.example.data.EmfUnit.MICROTESLA,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    accentColor: Color = GlowCyan,
    showGrid: Boolean = true,
    showLabels: Boolean = true,
    showPulse: Boolean = true,
    showInspector: Boolean = true
) {
    var touchX by remember { mutableStateOf<Float?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadiusAnimation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlphaAnimation"
    )

    val currentVal = history.lastOrNull() ?: 0f
    val maxValInHistory = (history.maxOrNull() ?: currentVal).coerceAtLeast(warningThreshold * 1.1f)
    val minValInHistory = (history.minOrNull() ?: currentVal).coerceAtMost(0f)
    val avgVal = if (history.isNotEmpty()) history.average().toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1120))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Line Chart Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (currentVal > warningThreshold) GlowRed else accentColor)
                )
                Column {
                    Text(
                        text = "REAL-TIME FLUCTUATION GRAPH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "MAGNETIC INTENSITY WAVE (${emfUnit.symbol})",
                        fontSize = 9.sp,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // High/Avg/Low Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PEAK",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = emfUnit.formatValueOnly(maxValInHistory),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GlowRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AVG",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = emfUnit.formatValueOnly(avgVal),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GlowAmber
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = emfUnit.formatValueOnly(currentVal),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        // Selected Inspector Banner (When touched)
        if (showInspector && selectedIndex != null && selectedIndex!! in history.indices) {
            val inspectedVal = history[selectedIndex!!]
            val timeOffsetSec = (history.size - 1 - selectedIndex!!) * 0.1f // ~10Hz update sample rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "INSPECTING T -%.1fs", timeOffsetSec),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan
                )
                Text(
                    text = "FIELD: ${emfUnit.format(inspectedVal)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (inspectedVal > warningThreshold) GlowRed else Color.White
                )
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF030712))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .testTag("realtime_line_chart")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(showInspector) {
                        if (!showInspector) return@pointerInput
                        detectTapGestures(
                            onTap = { offset ->
                                touchX = offset.x
                            },
                            onPress = { offset ->
                                touchX = offset.x
                                tryAwaitRelease()
                                touchX = null
                                selectedIndex = null
                            }
                        )
                    }
                    .pointerInput(showInspector) {
                        if (!showInspector) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset -> touchX = offset.x },
                            onDragEnd = {
                                touchX = null
                                selectedIndex = null
                            },
                            onDragCancel = {
                                touchX = null
                                selectedIndex = null
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                touchX = change.position.x
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height

                // Margin paddings for axis labels
                val leftPadding = if (showLabels) 36.dp.toPx() else 8.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = if (showLabels) 24.dp.toPx() else 8.dp.toPx()

                val drawWidth = width - leftPadding - rightPadding
                val drawHeight = height - topPadding - bottomPadding

                if (drawWidth <= 0 || drawHeight <= 0) return@Canvas

                // Compute scaling limits
                val maxVal = max(warningThreshold * 1.25f, maxValInHistory + 10f)
                val minVal = min(0f, minValInHistory)
                val range = max(10f, maxVal - minVal)

                // 1. Draw Grid Lines & Y-Axis Labels
                if (showGrid) {
                    val gridSteps = 4
                    val stepVal = range / gridSteps
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    for (i in 0..gridSteps) {
                        val v = minVal + i * stepVal
                        val normY = 1f - ((v - minVal) / range)
                        val y = topPadding + normY * drawHeight

                        // Grid horizontal line
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )

                        // Y-Axis Label
                        if (showLabels && (i == 0 || i == gridSteps / 2 || i == gridSteps)) {
                            val text = String.format(Locale.US, "%.0f", v)
                            drawContext.canvas.nativeCanvas.drawText(
                                text,
                                leftPadding - 6.dp.toPx(),
                                y + 3.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#64748B")
                                    textSize = 9.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.MONOSPACE
                                }
                            )
                        }
                    }

                    // Vertical grid lines (time ticks)
                    val timeSteps = 5
                    for (i in 0..timeSteps) {
                        val x = leftPadding + (i / timeSteps.toFloat()) * drawWidth
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.4f),
                            start = Offset(x, topPadding),
                            end = Offset(x, height - bottomPadding),
                            strokeWidth = 1f
                        )

                        if (showLabels && i % 2 == 0) {
                            val timeLabel = when (i) {
                                0 -> "-6s"
                                2 -> "-3s"
                                4 -> "NOW"
                                else -> ""
                            }
                            if (timeLabel.isNotEmpty()) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    timeLabel,
                                    x,
                                    height - 6.dp.toPx(),
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#64748B")
                                        textSize = 8.sp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                        typeface = android.graphics.Typeface.MONOSPACE
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Draw Threshold Warning Line
                val thresholdNormY = 1f - ((warningThreshold - minVal) / range)
                val thresholdY = topPadding + thresholdNormY * drawHeight
                if (thresholdY in topPadding..(height - bottomPadding)) {
                    drawLine(
                        color = GlowRed.copy(alpha = 0.7f),
                        start = Offset(leftPadding, thresholdY),
                        end = Offset(width - rightPadding, thresholdY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Threshold tag
                    drawContext.canvas.nativeCanvas.drawText(
                        "LIMIT ${warningThreshold.toInt()}µT",
                        width - rightPadding - 4.dp.toPx(),
                        thresholdY - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FF1744")
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )
                }

                // 3. Render Magnetic Field Intensity Line & Fill
                if (history.size >= 2) {
                    val maxPoints = 60
                    val points = history.takeLast(maxPoints)
                    val stepX = drawWidth / (maxPoints - 1).coerceAtLeast(1)

                    val linePath = Path()
                    val areaPath = Path()

                    val firstVal = points.first()
                    val firstNormY = 1f - ((firstVal - minVal) / range)
                    val firstX = leftPadding
                    val firstY = topPadding + firstNormY * drawHeight

                    linePath.moveTo(firstX, firstY)
                    areaPath.moveTo(firstX, height - bottomPadding)
                    areaPath.lineTo(firstX, firstY)

                    // Calculate touch selection index if user is scrubbing canvas
                    val currentTouchX = touchX
                    if (currentTouchX != null) {
                        val clampedTouchX = currentTouchX.coerceIn(leftPadding, width - rightPadding)
                        val ratio = (clampedTouchX - leftPadding) / drawWidth
                        val approxIndex = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                        selectedIndex = approxIndex
                    }

                    // Build smooth spline path
                    for (i in 0 until points.size - 1) {
                        val p0Val = points[i]
                        val p1Val = points[i + 1]

                        val x0 = leftPadding + i * stepX
                        val y0 = topPadding + (1f - ((p0Val - minVal) / range)) * drawHeight
                        val x1 = leftPadding + (i + 1) * stepX
                        val y1 = topPadding + (1f - ((p1Val - minVal) / range)) * drawHeight

                        val controlX1 = x0 + (x1 - x0) / 2f
                        val controlY1 = y0
                        val controlX2 = x0 + (x1 - x0) / 2f
                        val controlY2 = y1

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                    }

                    val lastX = leftPadding + (points.size - 1) * stepX
                    val lastY = topPadding + (1f - ((points.last() - minVal) / range)) * drawHeight

                    areaPath.lineTo(lastX, height - bottomPadding)
                    areaPath.close()

                    // Gradient Area Fill
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        )
                    )

                    // Glowing backdrop line stroke
                    drawPath(
                        path = linePath,
                        color = accentColor.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Sharp primary wave stroke
                    drawPath(
                        path = linePath,
                        color = accentColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 4. Draw Live Pulse Indicator Dot at latest point
                    if (showPulse) {
                        // Pulse aura ring
                        drawCircle(
                            color = accentColor.copy(alpha = pulseAlpha),
                            radius = pulseRadius.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )

                        // Core glowing dot
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = accentColor,
                            radius = 2.5.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    }

                    // 5. Draw Touch Inspection Line & Highlight Marker
                    val selIdx = selectedIndex
                    if (showInspector && selIdx != null && selIdx in points.indices) {
                        val inspectX = leftPadding + selIdx * stepX
                        val inspectVal = points[selIdx]
                        val inspectY = topPadding + (1f - ((inspectVal - minVal) / range)) * drawHeight

                        // Vertical inspection guideline
                        drawLine(
                            color = GlowCyan.copy(alpha = 0.8f),
                            start = Offset(inspectX, topPadding),
                            end = Offset(inspectX, height - bottomPadding),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        // Target crosshair rings
                        drawCircle(
                            color = GlowCyan,
                            radius = 6.dp.toPx(),
                            center = Offset(inspectX, inspectY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(inspectX, inspectY)
                        )
                    }
                }
            }
        }
    }
}
