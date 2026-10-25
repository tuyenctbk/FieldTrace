package com.example.ui.screens

import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.example.ui.components.VibrationCustomizerDialog
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import com.example.R
import com.example.ui.components.RealtimeLineChart
import com.example.ui.components.WiringDetectionOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FieldTraceViewModel
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScannerScreen(
    viewModel: FieldTraceViewModel,
    modifier: Modifier = Modifier
) {
    val emfX by viewModel.emfX.collectAsState()
    val emfY by viewModel.emfY.collectAsState()
    val emfZ by viewModel.emfZ.collectAsState()
    val emfMag by viewModel.emfMagnitude.collectAsState()
    val accuracy by viewModel.sensorAccuracy.collectAsState()
    val history by viewModel.history.collectAsState()
    val historyX by viewModel.historyX.collectAsState()
    val historyY by viewModel.historyY.collectAsState()
    val historyZ by viewModel.historyZ.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSonificationActive by viewModel.isSonificationActive.collectAsState()
    val audioFreq by viewModel.currentAudioFrequency.collectAsState()
    val threshold by viewModel.warningThreshold.collectAsState()
    val isVibeEnabled by viewModel.isVibrationEnabled.collectAsState()

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showVibrationCustomizer by remember { mutableStateOf(false) }
    var selectedVisualizerMode by remember { mutableStateOf(0) }

    // Dynamic warning coloring
    val accentColor = when {
        emfMag > threshold -> GlowRed
        emfMag > threshold * 0.6f -> GlowAmber
        else -> GlowCyan
    }

    val statusText = when {
        emfMag > threshold -> "CRITICAL"
        emfMag > threshold * 0.6f -> "ELEVATED"
        else -> "NORMAL"
    }

    val statusDesc = when {
        emfMag > threshold -> "Possible hidden wiring or magnetic appliance nearby."
        emfMag > threshold * 0.6f -> "Mild electronic field activity detected."
        else -> "Quiet background ambient electromagnetic fields."
    }

    val showRatingDialog by viewModel.showRatingDialog.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsState()
    val latestVersionName by viewModel.latestVersionName.collectAsState()
    val emfUnit by viewModel.emfUnit.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // In-App Update Notice Banner
        androidx.compose.animation.AnimatedVisibility(visible = isUpdateAvailable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("update_notice_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowCyan)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GlowCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Update Notice",
                                tint = GlowCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.update_available, latestVersionName),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.update_available_desc),
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { viewModel.dismissUpdateNotice() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.btn_dismiss),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Smart Calibration Recommendation Banner (Low Accuracy / Hard-iron Drift)
        val needsCalibration by viewModel.needsCalibration.collectAsState()
        val calibrationReason by viewModel.calibrationReason.collectAsState()
        val isCalibrating by viewModel.isCalibrating.collectAsState()

        AnimatedVisibility(visible = needsCalibration && !isCalibrating) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCalibrationDialog = true }
                    .testTag("calibration_reminder_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261A05)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlowAmber)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlowAmber)
                        )
                        Column {
                            Text(
                                text = "CALIBRATION RECOMMENDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlowAmber,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = calibrationReason ?: "Sensor precision low. Tap to calibrate baseline.",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlowAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CALIBRATE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = GlowAmber
                        )
                    }
                }
            }
        }

        var showGuideDialog by remember { mutableStateOf(false) }

        // App Title/Brand (Header block)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.live_emf_scanner),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "FLUX",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Replay Onboarding Guide Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { showGuideDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("top_bar_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "User Guide",
                        tint = GlowCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "GUIDE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan
                    )
                }

                // Calibrate Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { showCalibrationDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("top_bar_calibrate_button")
                ) {
                    Box(modifier = Modifier.size(width = 4.dp, height = 12.dp).background(GlowCyan))
                    Box(modifier = Modifier.size(width = 4.dp, height = 12.dp).background(GlowCyan))
                    Box(modifier = Modifier.size(width = 4.dp, height = 12.dp).background(Color(0xFF334155)))
                }
            }
        }

        // Visualizer Mode Selector Pill Row
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
                        .background(if (selectedVisualizerMode == 0) GlowCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(0.5.dp, if (selectedVisualizerMode == 0) GlowCyan else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { selectedVisualizerMode = 0 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("visualizer_mode_radar")
                ) {
                    Text("🎯 " + androidx.compose.ui.res.stringResource(com.example.R.string.radar_sweep), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selectedVisualizerMode == 0) GlowCyan else TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedVisualizerMode == 1) GlowAmber.copy(alpha = 0.25f) else Color.Transparent)
                        .border(0.5.dp, if (selectedVisualizerMode == 1) GlowAmber else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { selectedVisualizerMode = 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("visualizer_mode_heatmap")
                ) {
                    Text("🔥 " + androidx.compose.ui.res.stringResource(com.example.R.string.heatmap_canvas), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selectedVisualizerMode == 1) GlowAmber else TextMuted)
                }
            }
        }

        // Central visualizer & metrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .testTag("radar_visualizer"),
            contentAlignment = Alignment.Center
        ) {
            // Threshold exceeded pulsing warning overlay (screen-flash alert)
            if (emfMag > threshold) {
                val infiniteTransition = rememberInfiniteTransition(label = "threshold_flash_transition")
                val flashAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(350, easing = LinearEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "threshold_flash_alpha"
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(GlowRed.copy(alpha = flashAlpha))
                )
            }

            if (selectedVisualizerMode == 1) {
                com.example.ui.components.RealtimeEmfHeatmapOverlay(
                    emfMag = emfMag,
                    threshold = threshold,
                    modifier = Modifier.fillMaxSize(),
                    showControlsHeader = false,
                    alphaMultiplier = 0.7f
                )
            } else {
                // Concentric design lines
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(0.7.dp, GlowCyan.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .border(0.7.dp, GlowCyan.copy(alpha = 0.25f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(0.7.dp, GlowCyan.copy(alpha = 0.45f), CircleShape)
                )

                // Scanning radar graphic
                RadarSweepCanvas(emfMag = emfMag, maxThreshold = threshold, accentColor = accentColor)
            }

            // Primary Metric display
            val convertedEmf = emfUnit.convert(emfMag)
            val wholePart = convertedEmf.toInt().toString()
            val decimalPart = String.format(Locale.US, "%01d", ((convertedEmf - convertedEmf.toInt()) * 10).toInt())

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = wholePart,
                        fontSize = 110.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = Color.White,
                        letterSpacing = (-4).sp,
                        lineHeight = 95.sp
                    )
                    Text(
                        text = ".$decimalPart",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlowCyan,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
                Text(
                    text = if (emfUnit == com.example.data.EmfUnit.MICROTESLA) "MICROTESLA (µT)" else "MILLIGAUSS (mG)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }

            // High-intensity electrical wiring pattern detection indicator overlay
            WiringDetectionOverlay(
                emfMag = emfMag,
                threshold = threshold,
                history = history
            )
        }

        // Axis Detail - Artistic Mono Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column X
            Column(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "X-AXIS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%+.2f", emfX),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan
                )
            }

            // Column Y
            Column(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Y-AXIS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%+.2f", emfY),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowAmber
                )
            }

            // Column Z
            Column(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Z-AXIS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%+.2f", emfZ),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowRed
                )
            }
        }

        // Live status indicator & Trace summary (Interaction Zone)
        val isRecording by viewModel.isRecording.collectAsState()
        val pointsCount by viewModel.recordedPointsCount.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                .border(0.5.dp, Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) GlowRed else Color(0xFF475569))
                )
                Column {
                    Text(
                        text = "SPATIAL TRACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isRecording) "Logging coordinate path ($pointsCount nodes)..." else "Idle - Waiting for scan trace mapping...",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            Text(
                text = if (isRecording) "ACTIVE" else "OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isRecording) GlowCyan else TextMuted
            )
        }

        // Control buttons (Hold/Press to Map, Mute, Vibration)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording("Quick Diagnostic Scan", "Initialized from live view")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("start_record_button"), // Support automated tests expecting this tag
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) GlowRed else GlowCyan
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isRecording) "STOP ACTIVE MAP" else "START FIELD MAP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = if (isRecording) Color.White else Color.Black
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.setMute(!isMuted) }
                    .testTag("audio_mute_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Toggle Audio Mute",
                    tint = if (isMuted) TextMuted else GlowCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.setVibrationEnabled(!isVibeEnabled) }
                    .testTag("vibe_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVibeEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = "Toggle Tactile Feedback",
                    tint = if (isVibeEnabled) GlowRed else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Unit Toggle Pill (µT vs mG)
            val activeUnit by viewModel.emfUnit.collectAsState()
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(0.5.dp, GlowCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.toggleEmfUnit() }
                    .testTag("unit_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeUnit.symbol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = GlowCyan
                )
            }
        }

        // Real-Time Audio Sonification Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSonificationActive && !isMuted) Color(0xFF0D1D30) else Color(0xFF0F172A).copy(alpha = 0.4f)
                )
                .border(
                    0.5.dp,
                    if (isSonificationActive && !isMuted) GlowCyan.copy(alpha = 0.5f) else Color(0xFF1E293B),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
                .testTag("sonification_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSonificationActive && !isMuted) GlowCyan.copy(alpha = 0.2f) else Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Sonification Frequency Generator",
                                tint = if (isSonificationActive && !isMuted) GlowCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "REAL-TIME SONIFICATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isSonificationActive && !isMuted) "Audio Frequency Synthesizer Active" else "Sonification Audio Tone Muted/Paused",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isSonificationActive && !isMuted,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (isMuted) viewModel.setMute(false)
                                viewModel.setSonificationEnabled(true)
                            } else {
                                viewModel.setSonificationEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GlowCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("sonification_toggle")
                    )
                }

                if (isSonificationActive && !isMuted) {
                    val currentWaveform by viewModel.waveformType.collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF08111D))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SONIC MODE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            com.example.sensor.WaveformType.values().forEach { type ->
                                val isSelected = currentWaveform == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) GlowCyan.copy(alpha = 0.25f) else Color.Transparent)
                                        .border(0.5.dp, if (isSelected) GlowCyan else Color.Transparent, RoundedCornerShape(4.dp))
                                        .clickable { viewModel.setWaveform(type) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = type.displayName,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) GlowCyan else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF08111D))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUDIO TONE FREQUENCY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f Hz", audioFreq),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = GlowCyan
                        )
                    }

                    // Background Service Sonification Toggle Row
                    val isBgServiceRunning by viewModel.isBackgroundServiceRunning.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBgServiceRunning) GlowCyan.copy(alpha = 0.15f) else Color(0xFF08111D))
                            .border(0.5.dp, if (isBgServiceRunning) GlowCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleBackgroundAudioService(context) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("toggle_background_audio_service"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isBgServiceRunning) GlowCyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.bg_service_hear_emf),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBgServiceRunning) GlowCyan else TextWhite,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = if (isBgServiceRunning) androidx.compose.ui.res.stringResource(com.example.R.string.bg_service_running) else androidx.compose.ui.res.stringResource(com.example.R.string.bg_service_start),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBgServiceRunning) GlowCyan else TextMuted
                        )
                    }
                }
            }
        }

        // Real-time Line Chart Component (Magnetic Field Intensity Fluctuations)
        RealtimeLineChart(
            history = history,
            historyX = historyX,
            historyY = historyY,
            historyZ = historyZ,
            warningThreshold = threshold,
            emfUnit = emfUnit,
            accentColor = accentColor,
            chartHeight = 170.dp
        )

        // Space Weather (NOAA SWPC Planetary Kp-Index) Card
        val spaceData by viewModel.spaceWeatherData.collectAsState()
        spaceData?.let { data ->
            com.example.ui.components.SpaceWeatherGauge(spaceData = data)
        }

        // OpenCelliD RF Infrastructure & Cell Tower Radar Overlay
        val rfSummary by viewModel.rfSummary.collectAsState()
        rfSummary?.let { summary ->
            com.example.ui.components.CellTowerRadarOverlay(rfSummary = summary)
        }

        // Settings & Precision matrix
        val isNightMode by viewModel.isNightMode.collectAsState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SONIC ALERT THRESHOLD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = emfUnit.format(threshold, decimals = 0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan
                    )
                }

                Slider(
                    value = threshold,
                    onValueChange = { viewModel.setWarningThreshold(it) },
                    valueRange = 30f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = GlowCyan,
                        activeTrackColor = GlowCyan,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("threshold_slider")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                // Vibration Feedback Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setVibrationEnabled(!isVibeEnabled) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isVibeEnabled) Icons.Default.Vibration else Icons.Default.NotificationsOff,
                            contentDescription = "Vibration feedback icon",
                            tint = if (isVibeEnabled) GlowCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "TACTILE VIBRATION FEEDBACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Vibrate dynamically based on magnetic field intensity.",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = isVibeEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GlowCyan,
                            checkedTrackColor = GlowCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("vibration_settings_switch")
                    )
                }

                if (isVibeEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVibrationCustomizer = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "CUSTOMIZE VIBRATION PATTERNS →",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("customize_vibration_patterns_trigger")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                // Global Theme Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setNightMode(!isNightMode) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme mode icon",
                            tint = if (isNightMode) GlowCyan else GlowAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "SURVEYING NIGHT MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Toggle high-contrast dark palette vs. daylight mode.",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = isNightMode,
                        onCheckedChange = { viewModel.setNightMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GlowCyan,
                            checkedTrackColor = GlowCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("theme_settings_switch")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                // Calibration Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCalibrationDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Calibration info icon",
                            tint = GlowCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "CALIBRATION HELPER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Press to calibrate built-in sensors.",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    val accuracyText = when (accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MED"
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW"
                        else -> "UNRELIABLE"
                    }
                    val accuracyColor = when (accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> GlowCyan
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> GlowAmber
                        else -> GlowRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accuracyColor.copy(alpha = 0.15f))
                            .border(0.5.dp, accuracyColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRECISION: $accuracyText",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accuracyColor
                        )
                    }
                }
            }
        }

        if (showGuideDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showGuideDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                com.example.ui.screens.OnboardingScreen(
                    onCompleteOnboarding = { showGuideDialog = false },
                    onClose = { showGuideDialog = false }
                )
            }
        }

        if (showCalibrationDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showCalibrationDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                CalibrationScreen(
                    viewModel = viewModel,
                    onDismiss = { showCalibrationDialog = false }
                )
            }
        }

        if (showVibrationCustomizer) {
            VibrationCustomizerDialog(
                viewModel = viewModel,
                onDismiss = { showVibrationCustomizer = false }
            )
        }

        if (showRatingDialog) {
            com.example.ui.components.RatingDialog(
                onDismiss = { viewModel.dismissRatingDialog() }
            )
        }

        if (showShareDialog) {
            com.example.ui.components.ShareAppDialog(
                onDismiss = { viewModel.dismissShareDialog() }
            )
        }

        Spacer(modifier = Modifier.height(72.dp)) // Padding for bottom navigation
    }
}

@Composable
fun AxisDisplay(label: String, value: Float, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "%+.1f", value),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
        Text(
            text = "µT",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
    }
}

@Composable
fun RadarSweepCanvas(emfMag: Float, maxThreshold: Float, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "RadarSweepRotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)
        val radius = width / 2

        // Draw radar circular gridlines
        drawCircle(
            color = accentColor.copy(alpha = 0.1f),
            radius = radius * 0.33f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.1f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.2f),
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Draw crosshair axes
        drawLine(
            color = accentColor.copy(alpha = 0.15f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f
        )
        drawLine(
            color = accentColor.copy(alpha = 0.15f),
            start = Offset(width / 2, 0f),
            end = Offset(width / 2, height),
            strokeWidth = 1f
        )

        // Draw sweeping sector
        val rotationRadians = Math.toRadians(rotation.toDouble())
        val sweepX = center.x + radius * 0.95f * cos(rotationRadians).toFloat()
        val sweepY = center.y + radius * 0.95f * sin(rotationRadians).toFloat()

        // Sector gradient trail
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.25f)),
                center = center
            ),
            startAngle = rotation - 45f,
            sweepAngle = 45f,
            useCenter = true
        )

        // Sweeping front edge line
        drawLine(
            color = accentColor.copy(alpha = 0.5f),
            start = center,
            end = Offset(sweepX, sweepY),
            strokeWidth = 1.5f
        )

        // Dynamic center pulse circle based on real-time field strength
        val maxMagFactor = (emfMag / maxThreshold).coerceIn(0.05f, 1f)
        drawCircle(
            color = accentColor.copy(alpha = 0.15f),
            radius = radius * 0.9f * maxMagFactor,
            center = center
        )
    }
}

@Composable
fun OscilloscopeCanvas(history: List<Float>, accentColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.Black)
            .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
    ) {
        if (history.size < 2) return@Canvas

        val path = Path()
        val width = size.width
        val height = size.height

        val dx = width / 60f // Step distance for horizontal points
        
        // Find local bounds for nice dynamic vertical scaling
        val maxVal = (history.maxOrNull() ?: 100f).coerceAtLeast(60f)
        val minVal = (history.minOrNull() ?: 0f).coerceAtMost(maxVal - 10f)
        val range = (maxVal - minVal).coerceAtLeast(10f)

        history.forEachIndexed { idx, value ->
            // Scale points vertically
            val normalizedY = 1f - ((value - minVal) / range)
            val y = 10.dp.toPx() + normalizedY * (height - 20.dp.toPx())
            val x = idx * dx

            if (idx == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw the glowing signal line
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3f)
        )

        // Draw secondary transparent glow fill
        val areaPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}
