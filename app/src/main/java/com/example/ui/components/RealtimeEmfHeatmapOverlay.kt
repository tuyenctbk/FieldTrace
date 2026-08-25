package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time Canvas component that overlays EMF intensity levels dynamically on the screen
 * as a thermal heat map visualization.
 */
@Composable
fun RealtimeEmfHeatmapOverlay(
    emfMag: Float,
    threshold: Float = 80f,
    modifier: Modifier = Modifier,
    showControlsHeader: Boolean = true,
    alphaMultiplier: Float = 0.85f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeatmapPulse")
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseFactor"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    val normalizedIntensity = (emfMag / threshold).coerceIn(0f, 2.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("realtime_emf_heatmap_overlay")
    ) {
        // Real-Time Canvas Heatmap Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2, h / 2)

            if (w <= 0 || h <= 0) return@Canvas

            // Core Heat Color Palette based on current live EMF magnitude
            val coreColor = when {
                emfMag >= threshold -> GlowRed
                emfMag >= threshold * 0.6f -> GlowAmber
                else -> GlowCyan
            }

            val outerColor = when {
                emfMag >= threshold -> GlowRed.copy(alpha = 0.35f * alphaMultiplier)
                emfMag >= threshold * 0.6f -> GlowAmber.copy(alpha = 0.25f * alphaMultiplier)
                else -> GlowCyan.copy(alpha = 0.15f * alphaMultiplier)
            }

            // 1. Primary Central Thermal Blob
            val primaryRadius = (w * 0.38f * (0.6f + normalizedIntensity * 0.4f) * pulseRadiusFactor)
                .coerceAtMost(w * 0.75f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = (0.7f * alphaMultiplier).coerceAtMost(1f)),
                        outerColor,
                        coreColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = primaryRadius
                ),
                radius = primaryRadius,
                center = center
            )

            // 2. Secondary Satellite Anomaly Hotspots (driven by X, Y vector dispersion simulation)
            val satelliteCount = 5
            for (i in 0 until satelliteCount) {
                val angle = waveOffset + (i * (6.28318f / satelliteCount))
                val distance = (w * 0.22f) + sin(angle * 2) * (w * 0.08f)
                val satX = center.x + cos(angle) * distance
                val satY = center.y + sin(angle) * distance
                val satCenter = Offset(satX, satY)

                val satRadius = (40.dp.toPx() * (0.8f + normalizedIntensity * 0.5f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.45f * alphaMultiplier),
                            coreColor.copy(alpha = 0.15f * alphaMultiplier),
                            Color.Transparent
                        ),
                        center = satCenter,
                        radius = satRadius
                    ),
                    radius = satRadius,
                    center = satCenter
                )
            }

            // 3. Thermal Intensity Iso-Contour Rings
            val ringCount = 3
            for (r in 1..ringCount) {
                val ringRadius = primaryRadius * (r / ringCount.toFloat())
                val ringAlpha = (0.35f - r * 0.08f).coerceAtLeast(0.05f) * alphaMultiplier

                drawCircle(
                    color = coreColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Header indicator badge
        if (showControlsHeader) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Real-Time Thermal Heatmap",
                    tint = if (emfMag >= threshold) GlowRed else GlowAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.canvas_heatmap_overlay),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (emfMag >= threshold) GlowRed else GlowCyan)
                )
            }
        }
    }
}
