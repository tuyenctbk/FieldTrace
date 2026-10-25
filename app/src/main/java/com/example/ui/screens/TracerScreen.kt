package com.example.ui.screens

import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TracePoint
import com.example.ui.FieldTraceViewModel
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale

@Composable
fun TracerScreen(
    viewModel: FieldTraceViewModel,
    modifier: Modifier = Modifier
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val pointsCount by viewModel.recordedPointsCount.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val emfMag by viewModel.emfMagnitude.collectAsState()
    val emfX by viewModel.emfX.collectAsState()
    val emfY by viewModel.emfY.collectAsState()
    val emfZ by viewModel.emfZ.collectAsState()
    val threshold by viewModel.warningThreshold.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    var sessionNameInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val sessionPointsList by viewModel.getPointsForSession(currentSessionId ?: -1)
        .collectAsState(initial = emptyList())

    val sessionPoints = if (isRecording) sessionPointsList else emptyList()

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val recordAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha_anim"
    )

    var isCameraHudActive by remember { mutableStateOf(false) }
    var isAutoCaptureEnabled by remember { mutableStateOf(true) }
    var mapperViewMode by remember { mutableStateOf(0) } // 0: Grid, 1: Spatial Heatmap, 2: Camera HUD
    val activeUnit by viewModel.emfUnit.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // Module Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.spatial_field_mapper),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.mapper),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Camera HUD Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCameraHudActive) GlowAmber.copy(alpha = 0.25f) else Color(0xFF1E293B))
                        .border(0.5.dp, if (isCameraHudActive) GlowAmber else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { isCameraHudActive = !isCameraHudActive }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_camera_hud_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera HUD",
                            tint = if (isCameraHudActive) GlowAmber else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isCameraHudActive) "CAM HUD ON" else "📷 CAM HUD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCameraHudActive) GlowAmber else Color.White
                        )
                    }
                }

                // GPS Lock Status Indicator
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (currentLocation != null) GlowCyan else GlowRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (currentLocation != null) stringResource(R.string.gps_sync) else stringResource(R.string.no_link),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentLocation != null) GlowCyan else GlowRed,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentLocation != null) {
                            String.format(Locale.US, "%.5f, %.5f", currentLocation!!.latitude, currentLocation!!.longitude)
                        } else {
                            stringResource(R.string.estimator_fallback)
                        },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        // Dedicated Top Mapping Status Header (Positioned Outside Canvas to Prevent Overlapping Nodes)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mapping_status_header"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) Color(0xFF1E101D) else Color(0xFF0F172A).copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isRecording) GlowRed.copy(alpha = 0.6f) else Color(0xFF1E293B)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GlowRed.copy(alpha = recordAlpha))
                        )
                        Text(
                            text = stringResource(R.string.mapping_active),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GlowRed,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = stringResource(R.string.logged_nodes), fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "$pointsCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val activeUnit by viewModel.emfUnit.collectAsState()
                            Text(text = stringResource(R.string.live_field), fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = activeUnit.format(emfMag),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (emfMag > threshold) GlowRed else if (emfMag > threshold * 0.6f) GlowAmber else GlowCyan
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = "Ready", tint = GlowCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.ready_for_scan_trace),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.canvas_ready),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GlowCyan
                    )
                }
            }
        }

        // Mapper Canvas View Mode Selector Pill Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mapperViewMode == 0) GlowCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(0.5.dp, if (mapperViewMode == 0) GlowCyan else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            mapperViewMode = 0
                            isCameraHudActive = false
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("tracer_mode_grid")
                ) {
                    Text("📍 " + androidx.compose.ui.res.stringResource(com.example.R.string.trace_grid), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (mapperViewMode == 0) GlowCyan else TextMuted)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mapperViewMode == 1) GlowAmber.copy(alpha = 0.25f) else Color.Transparent)
                        .border(0.5.dp, if (mapperViewMode == 1) GlowAmber else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            mapperViewMode = 1
                            isCameraHudActive = false
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("tracer_mode_heatmap")
                ) {
                    Text("🔥 " + androidx.compose.ui.res.stringResource(com.example.R.string.spatial_heatmap_tab), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (mapperViewMode == 1) GlowAmber else TextMuted)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mapperViewMode == 2 || isCameraHudActive) GlowRed.copy(alpha = 0.25f) else Color.Transparent)
                        .border(0.5.dp, if (mapperViewMode == 2 || isCameraHudActive) GlowRed else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            mapperViewMode = 2
                            isCameraHudActive = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("tracer_mode_cam")
                ) {
                    Text("📷 " + androidx.compose.ui.res.stringResource(com.example.R.string.cam_hud), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (mapperViewMode == 2 || isCameraHudActive) GlowRed else TextMuted)
                }
            }
        }

        // Vector Spatial Matrix Canvas or Camera Preview HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF070C15))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mapperViewMode == 2 || isCameraHudActive) {
                com.example.ui.components.CameraPreviewHud(
                    emfMag = emfMag,
                    emfUnit = activeUnit,
                    latitude = currentLocation?.latitude ?: 0.0,
                    longitude = currentLocation?.longitude ?: 0.0,
                    isAutoCaptureEnabled = isAutoCaptureEnabled,
                    onToggleAutoCapture = { isAutoCaptureEnabled = it },
                    onPhotoCaptured = { photoPath ->
                        viewModel.onPhotoCaptured(emfMag)
                        if (isRecording) {
                            val pt = com.example.data.TracePoint(
                                sessionId = currentSessionId ?: 0,
                                latitude = currentLocation?.latitude ?: 0.0,
                                longitude = currentLocation?.longitude ?: 0.0,
                                emfMagnitude = emfMag,
                                emfX = emfX,
                                emfY = emfY,
                                emfZ = emfZ,
                                photoPath = photoPath
                            )
                            viewModel.recordPoint(pt)
                        }
                    }
                )
            } else if (mapperViewMode == 1) {
                com.example.ui.components.SpatialHeatmapGrid(
                    points = sessionPoints,
                    threshold = threshold,
                    emfUnit = activeUnit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SpatialGridCanvas(
                    points = sessionPoints,
                    isRecording = isRecording,
                    threshold = threshold
                )
            }
        }

        // Recording Controls Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.3f))
                .border(0.5.dp, Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isRecording) {
                    Text(
                        text = stringResource(R.string.new_trace_parameters),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = sessionNameInput,
                        onValueChange = { sessionNameInput = it },
                        label = { Text(stringResource(R.string.trace_session_name)) },
                        placeholder = { Text(stringResource(R.string.default_session_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowCyan,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = GlowCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text(stringResource(R.string.diagnostic_notes)) },
                        placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_notes_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowCyan,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = GlowCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.startRecording(sessionNameInput, notesInput)
                            sessionNameInput = ""
                            notesInput = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_record_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Start recording icon",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.start_record_button),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.scan_trace_underway),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.scan_instructions),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = stringResource(R.string.scan_description),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GlowRed.copy(alpha = recordAlpha))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("stop_record_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowRed),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.finalize_record_button),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Spatial canvas rendering grid lines, X/Y axis meter scale values, and GPS trace nodes.
 */
@Composable
fun SpatialGridCanvas(
    points: List<TracePoint>,
    isRecording: Boolean,
    threshold: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val pad = 36.dp.toPx()

        val numDivisions = 5
        val stepX = (width - 2 * pad) / numDivisions
        val stepY = (height - 2 * pad) / numDivisions

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

        // 1. Draw Grid Lines & X/Y Axis Scale Values
        for (i in 0..numDivisions) {
            val gx = pad + i * stepX
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                start = Offset(gx, pad),
                end = Offset(gx, height - pad),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // X-Axis Meter Scale Values (-10m to +10m)
            val valX = ((i.toFloat() / numDivisions) * 20f - 10f).toInt()
            val labelX = if (valX > 0) "+${valX}m" else "${valX}m"
            drawContext.canvas.nativeCanvas.drawText(
                labelX,
                gx,
                height - 10.dp.toPx(),
                textPaint
            )
        }

        for (j in 0..numDivisions) {
            val gy = pad + j * stepY
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                start = Offset(pad, gy),
                end = Offset(width - pad, gy),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // Y-Axis Meter Scale Values (+10m to -10m)
            val valY = (10f - (j.toFloat() / numDivisions) * 20f).toInt()
            val labelY = if (valY > 0) "+${valY}m" else "${valY}m"
            drawContext.canvas.nativeCanvas.drawText(
                labelY,
                pad - 6.dp.toPx(),
                gy + 3.dp.toPx(),
                yTextPaint
            )
        }

        // Origin Crosshair Axes Lines
        drawLine(
            color = GlowCyan.copy(alpha = 0.35f),
            start = Offset(width / 2, pad / 2),
            end = Offset(width / 2, height - pad / 2),
            strokeWidth = 1.5f
        )
        drawLine(
            color = GlowCyan.copy(alpha = 0.35f),
            start = Offset(pad / 2, height / 2),
            end = Offset(width - pad / 2, height / 2),
            strokeWidth = 1.5f
        )

        if (points.isEmpty()) {
            // Concentric target radar rings when empty
            drawCircle(
                color = GlowCyan.copy(alpha = 0.15f),
                radius = 60.dp.toPx(),
                center = Offset(width / 2, height / 2),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = GlowCyan.copy(alpha = 0.05f),
                radius = 110.dp.toPx(),
                center = Offset(width / 2, height / 2),
                style = Stroke(width = 1.dp.toPx())
            )
            return@Canvas
        }

        // Project latitude & longitude into Canvas space
        val latitudes = points.map { it.latitude }
        val longitudes = points.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 1.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 1.0

        val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)

        fun mapToCanvas(pt: TracePoint): Offset {
            val normX = (pt.longitude - minLon) / lonRange
            val normY = 1.0 - ((pt.latitude - minLat) / latRange)

            val cx = pad + normX.toFloat() * (width - 2 * pad)
            val cy = pad + normY.toFloat() * (height - 2 * pad)
            return Offset(cx, cy)
        }

        val canvasPoints = points.map { mapToCanvas(it) }

        // Draw connecting track line
        for (i in 0 until canvasPoints.size - 1) {
            drawLine(
                color = GlowCyan.copy(alpha = 0.4f),
                start = canvasPoints[i],
                end = canvasPoints[i + 1],
                strokeWidth = 2.dp.toPx()
            )
        }

        // Draw nodes styled by EMF strength
        points.forEachIndexed { index, pt ->
            val center = canvasPoints[index]
            val nodeColor = when {
                pt.emfMagnitude > threshold -> GlowRed
                pt.emfMagnitude > threshold * 0.6f -> GlowAmber
                else -> GlowCyan
            }

            drawCircle(
                color = nodeColor.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = center
            )
            drawCircle(
                color = nodeColor,
                radius = 3.5.dp.toPx(),
                center = center
            )

            if (index == points.size - 1) {
                drawCircle(
                    color = nodeColor.copy(alpha = 0.25f),
                    radius = 20.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
