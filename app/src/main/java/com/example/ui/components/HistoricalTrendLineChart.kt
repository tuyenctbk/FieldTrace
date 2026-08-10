package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.EmfUnit
import com.example.data.TracePoint
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data visualization component for displaying historical magnetic field intensity (µT)
 * trends over time for saved EMF scan sessions on an interactive line chart.
 */
@Composable
fun HistoricalTrendLineChart(
    points: List<TracePoint>,
    warningThreshold: Float = 80f,
    emfUnit: EmfUnit = EmfUnit.MICROTESLA,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp
) {
    var touchX by remember { mutableStateOf<Float?>(null) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val values = points.map { it.emfMagnitude }
    val maxValInPoints = (values.maxOrNull() ?: 0f).coerceAtLeast(warningThreshold * 1.1f)
    val minValInPoints = (values.minOrNull() ?: 0f).coerceAtMost(0f)
    val avgVal = if (values.isNotEmpty()) values.average().toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card Header
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
                        .background(GlowCyan)
                )
                Column {
                    Text(
                        text = "HISTORICAL INTENSITY TREND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "RECORDED EMF OVER SESSION TIME (${points.size} DATA POINTS)",
                        fontSize = 9.sp,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "PEAK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(
                        text = emfUnit.formatValueOnly(maxValInPoints),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GlowRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "AVG", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(
                        text = emfUnit.formatValueOnly(avgVal),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GlowAmber
                    )
                }
            }
        }

        // Inspector Banner when scrubbing chart
        if (selectedPointIndex != null && selectedPointIndex!! in points.indices) {
            val pt = points[selectedPointIndex!!]
            val timeSdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POINT #${selectedPointIndex!! + 1} · ${timeSdf.format(Date(pt.timestamp))}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan
                )
                Text(
                    text = "FIELD: ${emfUnit.format(pt.emfMagnitude)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (pt.emfMagnitude > warningThreshold) GlowRed else Color.White
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
                .testTag("historical_trend_chart")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> touchX = offset.x }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> touchX = offset.x },
                            onDragEnd = {
                                touchX = null
                                selectedPointIndex = null
                            },
                            onDragCancel = {
                                touchX = null
                                selectedPointIndex = null
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

                val leftPadding = 36.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val drawWidth = width - leftPadding - rightPadding
                val drawHeight = height - topPadding - bottomPadding

                if (drawWidth <= 0 || drawHeight <= 0) return@Canvas

                val maxVal = maxOf(warningThreshold * 1.2f, maxValInPoints + 10f)
                val minVal = minOf(0f, minValInPoints)
                val range = maxOf(10f, maxVal - minVal)

                // 1. Grid Lines
                val gridSteps = 4
                val stepVal = range / gridSteps
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                for (i in 0..gridSteps) {
                    val v = minVal + i * stepVal
                    val normY = 1f - ((v - minVal) / range)
                    val y = topPadding + normY * drawHeight

                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1f,
                        pathEffect = dashPathEffect
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.0f", v),
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

                // Threshold line
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
                }

                // 2. Plotting Points
                if (points.size >= 2) {
                    val stepX = drawWidth / (points.size - 1).coerceAtLeast(1)

                    val linePath = Path()
                    val areaPath = Path()

                    val firstVal = points.first().emfMagnitude
                    val firstY = topPadding + (1f - ((firstVal - minVal) / range)) * drawHeight
                    val firstX = leftPadding

                    linePath.moveTo(firstX, firstY)
                    areaPath.moveTo(firstX, height - bottomPadding)
                    areaPath.lineTo(firstX, firstY)

                    // Touch inspection calculation
                    val curTouchX = touchX
                    if (curTouchX != null) {
                        val clampedX = curTouchX.coerceIn(leftPadding, width - rightPadding)
                        val ratio = (clampedX - leftPadding) / drawWidth
                        val approxIndex = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                        selectedPointIndex = approxIndex
                    }

                    for (i in 0 until points.size - 1) {
                        val p0Val = points[i].emfMagnitude
                        val p1Val = points[i + 1].emfMagnitude

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
                    val lastY = topPadding + (1f - ((points.last().emfMagnitude - minVal) / range)) * drawHeight

                    areaPath.lineTo(lastX, height - bottomPadding)
                    areaPath.close()

                    // Gradient area
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlowCyan.copy(alpha = 0.35f),
                                GlowCyan.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        )
                    )

                    // Glow line stroke
                    drawPath(
                        path = linePath,
                        color = GlowCyan.copy(alpha = 0.3f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Main stroke
                    drawPath(
                        path = linePath,
                        color = GlowCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Touch selection crosshair
                    val selIdx = selectedPointIndex
                    if (selIdx != null && selIdx in points.indices) {
                        val inspectX = leftPadding + selIdx * stepX
                        val inspectVal = points[selIdx].emfMagnitude
                        val inspectY = topPadding + (1f - ((inspectVal - minVal) / range)) * drawHeight

                        drawLine(
                            color = GlowCyan,
                            start = Offset(inspectX, topPadding),
                            end = Offset(inspectX, height - bottomPadding),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        drawCircle(color = GlowCyan, radius = 6.dp.toPx(), center = Offset(inspectX, inspectY), style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(inspectX, inspectY))
                    }
                }
            }
        }
    }
}
