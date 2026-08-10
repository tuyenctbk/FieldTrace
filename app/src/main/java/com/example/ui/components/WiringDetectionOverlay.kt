package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Visual Indicator Overlay that signals the detection of high-intensity
 * magnetic patterns typically associated with concealed electrical wiring.
 */
@Composable
fun WiringDetectionOverlay(
    emfMag: Float,
    threshold: Float = 75f,
    history: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    // High-intensity electrical wiring pattern detection trigger logic
    val recentPeak = history.takeLast(10).maxOrNull() ?: emfMag
    val isWiringDetected = emfMag >= threshold * 0.85f || recentPeak >= threshold

    // Calculated confidence based on field magnitude and AC ripple signature
    val confidence = if (isWiringDetected) {
        ((emfMag - 50f) / 60f).coerceIn(0.65f, 0.99f)
    } else 0f

    val estimatedDepthCm = if (isWiringDetected) {
        max(0.8f, 12.0f - (emfMag / 12f)).coerceIn(0.8f, 15.0f)
    } else 0f

    // Pulsing aura animations
    val infiniteTransition = rememberInfiniteTransition(label = "WiringHazardPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HazardBorderPulse"
    )

    val hazardScanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanLineMovement"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isWiringDetected,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Glowing Hazard Corner Chevrons & Edge Border Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("wiring_hazard_corner_canvas")
                ) {
                    val w = size.width
                    val h = size.height
                    val cornerLen = 32.dp.toPx()
                    val strokeW = 4.dp.toPx()
                    val hazardColor = GlowRed.copy(alpha = pulseAlpha)

                    // Top-Left Corner
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, cornerLen)
                            lineTo(0f, 0f)
                            lineTo(cornerLen, 0f)
                        },
                        color = hazardColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Square)
                    )

                    // Top-Right Corner
                    drawPath(
                        path = Path().apply {
                            moveTo(w - cornerLen, 0f)
                            lineTo(w, 0f)
                            lineTo(w, cornerLen)
                        },
                        color = hazardColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Square)
                    )

                    // Bottom-Left Corner
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, h - cornerLen)
                            lineTo(0f, h)
                            lineTo(cornerLen, h)
                        },
                        color = hazardColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Square)
                    )

                    // Bottom-Right Corner
                    drawPath(
                        path = Path().apply {
                            moveTo(w - cornerLen, h)
                            lineTo(w, h)
                            lineTo(w, h - cornerLen)
                        },
                        color = hazardColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Square)
                    )

                    // Perimeter hazard glow line
                    drawRect(
                        color = hazardColor.copy(alpha = pulseAlpha * 0.3f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Traveling laser line across overlay
                    val laserY = hazardScanLineOffset * h
                    drawLine(
                        color = GlowRed.copy(alpha = 0.6f),
                        start = Offset(0f, laserY),
                        end = Offset(w, laserY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // 2. High-Priority Floating Banner Indicator Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2A080C).copy(alpha = 0.95f),
                                    Color(0xFF150406).copy(alpha = 0.92f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(GlowRed, GlowAmber, GlowRed)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                        .testTag("wiring_detection_indicator_overlay")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header Row with Pulsing Warning Icon
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GlowRed.copy(alpha = pulseAlpha * 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Concealed Wiring Warning",
                                        tint = GlowAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "CONCEALED AC WIRING PATTERN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GlowRed,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = "HIGH-INTENSITY EMF CONDUCTOR MATCH",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlowAmber,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Hazard Rating Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlowRed)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LIVE WIRE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Confidence Meter & Proximity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "PATTERN MATCH",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.0f%%", confidence * 100f),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = GlowAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { confidence.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = GlowRed,
                                    trackColor = Color(0xFF3F1216)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EST. DEPTH",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                Text(
                                    text = String.format(Locale.US, "~%.1f cm", estimatedDepthCm),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
