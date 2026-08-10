package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TracePoint
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

enum class HeatmapMapMode {
    GRID,
    THERMAL,
    HYBRID
}

/**
 * Coordinate-based spatial heat map grid component overlaying recorded magnetic intensity values.
 * Includes Map Mode Toggle (Grid/Thermal/Hybrid), Peak Anomaly Marker, and Zoom/Recenter controls.
 */
@Composable
fun SpatialHeatmapGrid(
    points: List<TracePoint>,
    threshold: Float = 80f,
    emfUnit: com.example.data.EmfUnit = com.example.data.EmfUnit.MICROTESLA,
    modifier: Modifier = Modifier
) {
    var heatRadiusDp by remember { mutableFloatStateOf(28f) }
    var minCutoffEmf by remember { mutableFloatStateOf(20f) }
    var showControls by remember { mutableStateOf(false) }
    var mapMode by remember { mutableStateOf(HeatmapMapMode.GRID) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val peakPoint = remember(points) { points.maxByOrNull { it.emfMagnitude } }
    val peakPointIndex = remember(points, peakPoint) {
        if (peakPoint != null) points.indexOf(peakPoint) else null
    }

    val selectedPoint = selectedPointIndex?.let { idx ->
        if (idx in points.indices) points[idx] else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spatial_heatmap_grid_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Control Bar Header & Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Spatial Grid",
                    tint = GlowCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SPATIAL HEATMAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // Mode Selector Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HeatmapMapMode.values().forEach { mode ->
                    val isSelected = mapMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) GlowCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, if (isSelected) GlowCyan else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { mapMode = mode }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = mode.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) GlowCyan else TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showControls) GlowAmber.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { showControls = !showControls }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune Heatmap",
                        tint = if (showControls) GlowAmber else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Expanded Tuning Sliders
        AnimatedVisibility(visible = showControls) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "HEAT BLUR RADIUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(text = String.format(Locale.US, "%.0f dp", heatRadiusDp), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GlowCyan)
                    }
                    Slider(
                        value = heatRadiusDp,
                        onValueChange = { heatRadiusDp = it },
                        valueRange = 12f..50f,
                        colors = SliderDefaults.colors(thumbColor = GlowCyan, activeTrackColor = GlowCyan)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "NOISE FLOOR CUTOFF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(text = String.format(Locale.US, "%.0f µT", minCutoffEmf), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GlowAmber)
                    }
                    Slider(
                        value = minCutoffEmf,
                        onValueChange = { minCutoffEmf = it },
                        valueRange = 0f..60f,
                        colors = SliderDefaults.colors(thumbColor = GlowAmber, activeTrackColor = GlowAmber)
                    )
                }
            }
        }

        // Coordinate-Based Spatial Canvas Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF070C15))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("spatial_heatmap_canvas")
                    .pointerInput(points, zoomScale) {
                        detectTapGestures { tapOffset ->
                            if (points.isEmpty()) return@detectTapGestures

                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            val latitudes = points.map { it.latitude }
                            val longitudes = points.map { it.longitude }
                            val minLat = latitudes.minOrNull() ?: 0.0
                            val maxLat = latitudes.maxOrNull() ?: 1.0
                            val minLon = longitudes.minOrNull() ?: 0.0
                            val maxLon = longitudes.maxOrNull() ?: 1.0
                            val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
                            val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)
                            val pad = 40.dp.toPx()

                            var closestIdx: Int? = null
                            var minDistance = Float.MAX_VALUE

                            points.forEachIndexed { index, pt ->
                                val normX = (pt.longitude - minLon) / lonRange
                                val normY = 1.0 - ((pt.latitude - minLat) / latRange)
                                val baseCx = pad + normX.toFloat() * (w - 2 * pad)
                                val baseCy = pad + normY.toFloat() * (h - 2 * pad)

                                val cx = w / 2 + (baseCx - w / 2) * zoomScale
                                val cy = h / 2 + (baseCy - h / 2) * zoomScale

                                val dist = sqrt((tapOffset.x - cx).pow(2) + (tapOffset.y - cy).pow(2))
                                if (dist < minDistance && dist < 50.dp.toPx()) {
                                    minDistance = dist
                                    closestIdx = index
                                }
                            }
                            selectedPointIndex = closestIdx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val pad = 36.dp.toPx()

                // 1. Map Background Styling per Mode
                if (mapMode == HeatmapMapMode.GRID || mapMode == HeatmapMapMode.HYBRID) {
                    val numGridLines = 5
                    val stepX = (w - 2 * pad) / numGridLines
                    val stepY = (h - 2 * pad) / numGridLines

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#64748B")
                        textSize = 9.dp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val yTextPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#64748B")
                        textSize = 9.dp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }

                    for (i in 0..numGridLines) {
                        val gx = pad + i * stepX
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            start = Offset(gx, pad),
                            end = Offset(gx, h - pad),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        val valX = ((i.toFloat() / numGridLines) * 20f - 10f).toInt()
                        val labelX = if (valX > 0) "+${valX}m" else "${valX}m"
                        drawContext.canvas.nativeCanvas.drawText(
                            labelX,
                            gx,
                            h - 10.dp.toPx(),
                            textPaint
                        )
                    }

                    for (j in 0..numGridLines) {
                        val gy = pad + j * stepY
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            start = Offset(pad, gy),
                            end = Offset(w - pad, gy),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        val valY = (10f - (j.toFloat() / numGridLines) * 20f).toInt()
                        val labelY = if (valY > 0) "+${valY}m" else "${valY}m"
                        drawContext.canvas.nativeCanvas.drawText(
                            labelY,
                            pad - 6.dp.toPx(),
                            gy + 3.dp.toPx(),
                            yTextPaint
                        )
                    }

                    drawLine(
                        color = GlowCyan.copy(alpha = 0.3f),
                        start = Offset(w / 2, pad / 2),
                        end = Offset(w / 2, h - pad / 2),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = GlowCyan.copy(alpha = 0.3f),
                        start = Offset(pad / 2, h / 2),
                        end = Offset(w - pad / 2, h / 2),
                        strokeWidth = 1.5f
                    )
                }

                if (points.isEmpty()) {
                    drawCircle(
                        color = GlowCyan.copy(alpha = 0.1f),
                        radius = 80.dp.toPx(),
                        center = Offset(w / 2, h / 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    return@Canvas
                }

                // Map points to canvas coordinates with Zoom Scale transform
                val latitudes = points.map { it.latitude }
                val longitudes = points.map { it.longitude }
                val minLat = latitudes.minOrNull() ?: 0.0
                val maxLat = latitudes.maxOrNull() ?: 1.0
                val minLon = longitudes.minOrNull() ?: 0.0
                val maxLon = longitudes.maxOrNull() ?: 1.0
                val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
                val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)

                val canvasOffsets = points.map { pt ->
                    val normX = (pt.longitude - minLon) / lonRange
                    val normY = 1.0 - ((pt.latitude - minLat) / latRange)
                    val baseCx = pad + normX.toFloat() * (w - 2 * pad)
                    val baseCy = pad + normY.toFloat() * (h - 2 * pad)

                    Offset(
                        w / 2 + (baseCx - w / 2) * zoomScale,
                        h / 2 + (baseCy - h / 2) * zoomScale
                    )
                }

                // 2. Heatmap Gradient Blobs Layer (Thermal / Hybrid Modes)
                if (mapMode == HeatmapMapMode.THERMAL || mapMode == HeatmapMapMode.HYBRID || mapMode == HeatmapMapMode.GRID) {
                    val radiusPx = heatRadiusDp.dp.toPx() * zoomScale
                    val maxEmfInSession = (points.maxOfOrNull { it.emfMagnitude } ?: 100f).coerceAtLeast(threshold * 1.25f)
                    val rangeSpan = (maxEmfInSession - minCutoffEmf).coerceAtLeast(1f)

                    points.forEachIndexed { idx, pt ->
                        if (pt.emfMagnitude >= minCutoffEmf) {
                            val center = canvasOffsets[idx]
                            val intensityFraction = ((pt.emfMagnitude - minCutoffEmf) / rangeSpan).coerceIn(0f, 1f)

                            val centerColor = when {
                                pt.emfMagnitude >= threshold -> GlowRed.copy(alpha = 0.6f)
                                pt.emfMagnitude >= threshold * 0.65f -> GlowAmber.copy(alpha = 0.5f)
                                else -> GlowCyan.copy(alpha = 0.35f)
                            }

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(centerColor, centerColor.copy(alpha = 0.2f), centerColor.copy(alpha = 0f)),
                                    center = center,
                                    radius = radiusPx * (0.8f + intensityFraction * 0.6f)
                                ),
                                radius = radiusPx * (0.8f + intensityFraction * 0.6f),
                                center = center
                            )
                        }
                    }
                }

                // 3. Node Connection Lines & Nodes
                for (i in 0 until canvasOffsets.size - 1) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = canvasOffsets[i],
                        end = canvasOffsets[i + 1],
                        strokeWidth = 1.dp.toPx()
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val center = canvasOffsets[idx]
                    val isSelected = selectedPointIndex == idx
                    val isPeak = peakPointIndex == idx && pt.emfMagnitude >= threshold * 0.7f

                    val nodeColor = when {
                        pt.emfMagnitude >= threshold -> GlowRed
                        pt.emfMagnitude >= threshold * 0.65f -> GlowAmber
                        else -> GlowCyan
                    }

                    if (isPeak) {
                        // Pulsing Peak Anomaly Target Reticle
                        drawCircle(
                            color = GlowRed.copy(alpha = 0.4f),
                            radius = 16.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = GlowRed,
                            radius = 10.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    drawCircle(
                        color = if (isPeak) GlowRed else nodeColor,
                        radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                        center = center
                    )
                }
            }

            // Zoom & Recenter Control Buttons Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(0.5.dp, Color(0xFF1E293B), CircleShape)
                        .clickable { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = GlowCyan, modifier = Modifier.size(16.dp))
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(0.5.dp, Color(0xFF1E293B), CircleShape)
                        .clickable { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = GlowCyan, modifier = Modifier.size(16.dp))
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(0.5.dp, Color(0xFF1E293B), CircleShape)
                        .clickable { zoomScale = 1.0f; selectedPointIndex = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = "Recenter", tint = GlowAmber, modifier = Modifier.size(16.dp))
                }
            }

            // Legend & Mode Badge Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegendItem(label = "<40", color = GlowCyan)
                    LegendItem(label = "40-70", color = GlowAmber)
                    LegendItem(label = ">70 µT", color = GlowRed)
                }
            }
        }

        // Inspector Tooltip Panel
        selectedPoint?.let { pt ->
            var showPhotoDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("heatmap_inspector_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlowCyan)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "INSPECTED SPATIAL NODE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowCyan,
                                    letterSpacing = 1.sp
                                )
                                if (!pt.photoPath.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GlowAmber.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("📷 PHOTO EVIDENCE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = GlowAmber)
                                    }
                                }
                            }
                            Text(
                                text = String.format(Locale.US, "GPS: %.5f, %.5f", pt.latitude, pt.longitude),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = String.format(Locale.US, "VECTOR: X: %.1f | Y: %.1f | Z: %.1f", pt.emfX, pt.emfY, pt.emfZ),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = emfUnit.format(pt.emfMagnitude),
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    pt.emfMagnitude >= threshold -> GlowRed
                                    pt.emfMagnitude >= threshold * 0.65f -> GlowAmber
                                    else -> GlowCyan
                                }
                            )
                            Text(text = "FIELD INTENSITY", fontSize = 8.sp, color = TextMuted)
                        }
                    }

                    if (!pt.photoPath.isNullOrBlank()) {
                        Button(
                            onClick = { showPhotoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlowAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VIEW STAMPED PHOTO EVIDENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }

                if (showPhotoDialog && !pt.photoPath.isNullOrBlank()) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showPhotoDialog = false }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("STAMPED ANOMALY EVIDENCE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = GlowAmber)
                                coil.compose.AsyncImage(
                                    model = pt.photoPath,
                                    contentDescription = "Stamped Anomaly Evidence",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Button(
                                    onClick = { showPhotoDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Text("CLOSE PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
    }
}
