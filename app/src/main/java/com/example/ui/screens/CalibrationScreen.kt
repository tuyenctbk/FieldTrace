package com.example.ui.screens

import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FieldTraceViewModel
import com.example.ui.theme.BackgroundBlack
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CalibrationScreen(
    viewModel: FieldTraceViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCalibrating by viewModel.isCalibrating.collectAsState()
    val progress by viewModel.calibrationProgress.collectAsState()
    val isCalibrated by viewModel.isCalibrated.collectAsState()
    val accuracy by viewModel.sensorAccuracy.collectAsState()

    val xCoverage by viewModel.xCoverage.collectAsState()
    val yCoverage by viewModel.yCoverage.collectAsState()
    val zCoverage by viewModel.zCoverage.collectAsState()

    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()

    val rawMag by viewModel.rawMagnitude.collectAsState()
    val normMag by viewModel.emfMagnitude.collectAsState()
    val biasX by viewModel.biasX.collectAsState()
    val biasY by viewModel.biasY.collectAsState()
    val biasZ by viewModel.biasZ.collectAsState()

    // Auto-start calibration session if not already running
    LaunchedEffect(Unit) {
        if (!isCalibrating && !isCalibrated) {
            viewModel.startCalibration()
        }
    }

    val accuracyText = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH PRECISION"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM ACCURACY"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW ACCURACY"
        else -> "UNRELIABLE / STUCK"
    }

    val accuracyColor = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> GlowCyan
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> GlowAmber
        else -> GlowRed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("calibration_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "MAGNETOMETER SERVICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "SENSOR CALIBRATION",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accuracyColor.copy(alpha = 0.15f))
                    .border(0.5.dp, accuracyColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = accuracyText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accuracyColor
                )
            }
        }

        // Animated Figure-Eight Canvas Visualizer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("figure_eight_canvas_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                FigureEightMotionCanvas(
                    pitch = pitch,
                    roll = roll,
                    progress = progress,
                    isCalibrating = isCalibrating
                )

                // Overlay Text guidance
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (progress >= 1f) "CALIBRATION COMPLETE!" else "WAVE DEVICE IN ∞ MOTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (progress >= 1f) GlowCyan else Color.White,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (progress >= 1f) "Magnetometer sensor baseline normalized" else "Rotate across 3D axes to expand field envelope",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Overall Progress Bar & Metric
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "3D ENVELOPE COVERAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f%%", progress * 100f),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (progress >= 1f) GlowCyan else GlowAmber
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("calibration_progress_bar"),
                    color = if (progress >= 1f) GlowCyan else GlowAmber,
                    trackColor = Color(0xFF1E293B)
                )

                // Individual 3-Axis Sweep Completion Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AxisSweepMeter(axisLabel = "X-AXIS SWEEP", coverage = xCoverage, color = GlowCyan)
                    AxisSweepMeter(axisLabel = "Y-AXIS SWEEP", coverage = yCoverage, color = GlowAmber)
                    AxisSweepMeter(axisLabel = "Z-AXIS SWEEP", coverage = zCoverage, color = GlowRed)
                }
            }
        }

        // Raw vs Normalized Magnetometer Values Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "LIVE SENSOR NORMALIZATION DATA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                val activeUnit by viewModel.emfUnit.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "RAW MAGNITUDE", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = activeUnit.format(rawMag),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "CALIBRATED MAGNITUDE", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = activeUnit.format(normMag),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "BIAS CORRECTION: ΔX: %.1f | ΔY: %.1f | ΔZ: %.1f", biasX, biasY, biasZ),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        // Guidance & Instructions List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.3f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOW TO CALIBRATE YOUR SENSOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                InstructionItem(step = "1", text = "Hold device upright in open space away from large metallic objects or speaker magnets.")
                InstructionItem(step = "2", text = "Trace a smooth, continuous figure-eight (∞) loop in front of you.")
                InstructionItem(step = "3", text = "Tilt the phone forward and sideways to cover all X, Y, and Z axes until the gauge hits 100%.")
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (isCalibrating) {
                        viewModel.stopCalibration()
                    } else {
                        viewModel.startCalibration()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("toggle_calibration_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCalibrating) GlowAmber else GlowCyan
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCalibrating) "PAUSE CALIBRATION" else "RE-CALIBRATE SENSOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.Black
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("finish_calibration_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GlowCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLY & START SCANNING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun AxisSweepMeter(
    axisLabel: String,
    coverage: Float,
    color: Color
) {
    Column(
        modifier = Modifier.width(95.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = axisLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(
                text = String.format(Locale.US, "%.0f%%", coverage * 100f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { coverage.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun InstructionItem(step: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(GlowCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GlowCyan)
        }
        Text(
            text = text,
            fontSize = 10.sp,
            color = TextMuted,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun FigureEightMotionCanvas(
    pitch: Float,
    roll: Float,
    progress: Float,
    isCalibrating: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FigureEightAnimation")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoopPhase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val scaleX = width * 0.35f
        val scaleY = height * 0.22f

        // Build Lemniscate of Gerono / Bernoulli Figure-8 Path
        val figureEightPath = Path()
        val numPoints = 100
        for (i in 0..numPoints) {
            val t = (i.toFloat() / numPoints) * 2 * PI.toFloat()
            // Lemniscate parametric equations: x = scaleX * sin(t), y = scaleY * sin(t)*cos(t)
            val x = centerX + scaleX * sin(t)
            val y = centerY + scaleY * sin(t) * cos(t)

            if (i == 0) {
                figureEightPath.moveTo(x, y)
            } else {
                figureEightPath.lineTo(x, y)
            }
        }
        figureEightPath.close()

        // Background guide path
        drawPath(
            path = figureEightPath,
            color = Color(0xFF1E293B),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        )

        // Progress illuminated path
        val activeColor = if (progress >= 1f) GlowCyan else GlowAmber
        drawPath(
            path = figureEightPath,
            color = activeColor.copy(alpha = 0.4f + progress * 0.4f),
            style = Stroke(
                width = (3 + progress * 3).dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Animated traveling beam particle along figure-eight
        val tBeam = animPhase
        val beamX = centerX + scaleX * sin(tBeam)
        val beamY = centerY + scaleY * sin(tBeam) * cos(tBeam)

        drawCircle(
            color = activeColor.copy(alpha = 0.3f),
            radius = 16.dp.toPx(),
            center = Offset(beamX, beamY)
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(beamX, beamY)
        )
        drawCircle(
            color = activeColor,
            radius = 4.dp.toPx(),
            center = Offset(beamX, beamY)
        )

        // Real-time device tilt position dot (driven by accelerometer roll and pitch)
        val clampedRoll = roll.coerceIn(-45f, 45f) / 45f
        val clampedPitch = pitch.coerceIn(-45f, 45f) / 45f

        val deviceX = centerX + clampedRoll * scaleX
        val deviceY = centerY - clampedPitch * scaleY

        // Target crosshair for actual physical tilt
        drawCircle(
            color = GlowCyan.copy(alpha = 0.2f),
            radius = 20.dp.toPx(),
            center = Offset(deviceX, deviceY)
        )
        drawCircle(
            color = GlowCyan,
            radius = 8.dp.toPx(),
            center = Offset(deviceX, deviceY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(deviceX, deviceY)
        )
    }
}
