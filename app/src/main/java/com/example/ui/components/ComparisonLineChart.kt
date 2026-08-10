package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmfUnit
import com.example.data.TracePoint
import com.example.data.TraceSession
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import java.util.Locale

@Composable
fun ComparisonLineChart(
    session1: TraceSession,
    points1: List<TracePoint>,
    session2: TraceSession,
    points2: List<TracePoint>,
    warningThreshold: Float = 80f,
    emfUnit: EmfUnit = EmfUnit.MICROTESLA,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 220.dp
) {
    val maxVal1 = points1.maxOfOrNull { it.emfMagnitude } ?: 0f
    val maxVal2 = points2.maxOfOrNull { it.emfMagnitude } ?: 0f
    val overallMaxVal = maxOf(maxVal1, maxVal2, warningThreshold * 1.1f)

    val minVal1 = points1.minOfOrNull { it.emfMagnitude } ?: 0f
    val minVal2 = points2.minOfOrNull { it.emfMagnitude } ?: 0f
    val overallMinVal = minOf(minVal1, minVal2, 0f)

    val range = maxOf(10f, overallMaxVal - overallMinVal)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Legendary Header / Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "OVERLAID EMF INTENSITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "IDENTIFY SPATIAL VARIATIONS BETWEEN RUNS",
                    fontSize = 8.sp,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Color Coding Info Map
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Session 1 indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GlowCyan)
                )
                Text(
                    text = "${session1.name} (PEAK: ${emfUnit.formatValueOnly(maxVal1)})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    maxLines = 1
                )
            }

            // Session 2 indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GlowAmber)
                )
                Text(
                    text = "${session2.name} (PEAK: ${emfUnit.formatValueOnly(maxVal2)})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowAmber,
                    maxLines = 1
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
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val leftPadding = 36.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val drawWidth = width - leftPadding - rightPadding
                val drawHeight = height - topPadding - bottomPadding

                if (drawWidth <= 0 || drawHeight <= 0) return@Canvas

                val maxVal = overallMaxVal + 10f
                val minVal = overallMinVal
                val localRange = maxOf(10f, maxVal - minVal)

                // 1. Grid Lines
                val gridSteps = 4
                val stepVal = localRange / gridSteps
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                for (i in 0..gridSteps) {
                    val v = minVal + i * stepVal
                    val normY = 1f - ((v - minVal) / localRange)
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
                val thresholdNormY = 1f - ((warningThreshold - minVal) / localRange)
                val thresholdY = topPadding + thresholdNormY * drawHeight
                if (thresholdY in topPadding..(height - bottomPadding)) {
                    drawLine(
                        color = GlowRed.copy(alpha = 0.5f),
                        start = Offset(leftPadding, thresholdY),
                        end = Offset(width - rightPadding, thresholdY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Plotting Session 1
                if (points1.size >= 2) {
                    val stepX = drawWidth / (points1.size - 1).coerceAtLeast(1)
                    val linePath = Path()
                    val areaPath = Path()

                    val firstVal = points1.first().emfMagnitude
                    val firstY = topPadding + (1f - ((firstVal - minVal) / localRange)) * drawHeight
                    val firstX = leftPadding

                    linePath.moveTo(firstX, firstY)
                    areaPath.moveTo(firstX, height - bottomPadding)
                    areaPath.lineTo(firstX, firstY)

                    for (i in 0 until points1.size - 1) {
                        val p0Val = points1[i].emfMagnitude
                        val p1Val = points1[i + 1].emfMagnitude

                        val x0 = leftPadding + i * stepX
                        val y0 = topPadding + (1f - ((p0Val - minVal) / localRange)) * drawHeight
                        val x1 = leftPadding + (i + 1) * stepX
                        val y1 = topPadding + (1f - ((p1Val - minVal) / localRange)) * drawHeight

                        val controlX1 = x0 + (x1 - x0) / 2f
                        val controlY1 = y0
                        val controlX2 = x0 + (x1 - x0) / 2f
                        val controlY2 = y1

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                    }

                    val lastX = leftPadding + (points1.size - 1) * stepX
                    areaPath.lineTo(lastX, height - bottomPadding)
                    areaPath.close()

                    // Gradient Area Session 1 (Cyan)
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlowCyan.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        )
                    )

                    // Line Stroke Session 1
                    drawPath(
                        path = linePath,
                        color = GlowCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Plotting Session 2
                if (points2.size >= 2) {
                    val stepX = drawWidth / (points2.size - 1).coerceAtLeast(1)
                    val linePath = Path()
                    val areaPath = Path()

                    val firstVal = points2.first().emfMagnitude
                    val firstY = topPadding + (1f - ((firstVal - minVal) / localRange)) * drawHeight
                    val firstX = leftPadding

                    linePath.moveTo(firstX, firstY)
                    areaPath.moveTo(firstX, height - bottomPadding)
                    areaPath.lineTo(firstX, firstY)

                    for (i in 0 until points2.size - 1) {
                        val p0Val = points2[i].emfMagnitude
                        val p1Val = points2[i + 1].emfMagnitude

                        val x0 = leftPadding + i * stepX
                        val y0 = topPadding + (1f - ((p0Val - minVal) / localRange)) * drawHeight
                        val x1 = leftPadding + (i + 1) * stepX
                        val y1 = topPadding + (1f - ((p1Val - minVal) / localRange)) * drawHeight

                        val controlX1 = x0 + (x1 - x0) / 2f
                        val controlY1 = y0
                        val controlX2 = x0 + (x1 - x0) / 2f
                        val controlY2 = y1

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                    }

                    val lastX = leftPadding + (points2.size - 1) * stepX
                    areaPath.lineTo(lastX, height - bottomPadding)
                    areaPath.close()

                    // Gradient Area Session 2 (Amber)
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlowAmber.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        )
                    )

                    // Line Stroke Session 2
                    drawPath(
                        path = linePath,
                        color = GlowAmber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}
