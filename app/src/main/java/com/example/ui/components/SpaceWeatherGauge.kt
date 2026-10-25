package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SpaceWeatherData
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SpaceWeatherGauge(
    spaceData: SpaceWeatherData,
    modifier: Modifier = Modifier
) {
    val animatedKp by animateFloatAsState(
        targetValue = spaceData.kpIndex,
        animationSpec = tween(durationMillis = 1200),
        label = "kp_gauge_anim"
    )

    val gaugeColor = when {
        spaceData.kpIndex >= 7.0f -> GlowRed
        spaceData.kpIndex >= 5.0f -> GlowAmber
        spaceData.kpIndex >= 3.0f -> Color(0xFFFFD54F)
        else -> GlowGreen
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070E1A))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("space_weather_gauge_card")
    ) {
        Column {
            // Header Row
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
                            .background(gaugeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = gaugeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.space_weather_title),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(R.string.noaa_swpc_sub),
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // Live status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (spaceData.isLiveNoaaData) GlowGreen.copy(alpha = 0.2f) else Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (spaceData.isLiveNoaaData) "NOAA LIVE" else "SWPC MODEL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (spaceData.isLiveNoaaData) GlowGreen else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kp Index Sweep Arc & Storm Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Arc Meter
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val stroke = 8.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val topLeft = Offset(stroke / 2, stroke / 2)
                        val arcSize = Size(diameter, diameter)

                        // Background arc
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // Filled gauge arc
                        val sweep = (animatedKp / 9.0f) * 270f
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(GlowGreen, Color(0xFFFFD54F), GlowAmber, GlowRed)
                            ),
                            startAngle = 135f,
                            sweepAngle = sweep.coerceIn(2f, 270f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", animatedKp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = gaugeColor
                        )
                        Text(
                            text = "Kp-INDEX",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Storm Status Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spaceData.stormCategory.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = gaugeColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PLANETARY IMPACT: ${spaceData.planetaryEmfImpact}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = GlowAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "FLARE: ${spaceData.solarFlareClass}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        Text(
                            text = "WIND: ${spaceData.solarWindSpeedKmS} km/s",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
