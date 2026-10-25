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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CellTowerInfo
import com.example.data.RfInfrastructureSummary
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CellTowerRadarOverlay(
    rfSummary: RfInfrastructureSummary,
    onTowerSelected: (CellTowerInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "cell_radar_anim")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF060D17))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("cell_tower_radar_card")
    ) {
        Column {
            // Header Title
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
                            .background(GlowCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.rf_cell_towers_title),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(R.string.opencellid_sub),
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // RF Status Badge
                val badgeColor = when {
                    rfSummary.rfDensityStatus.contains("HIGH") -> GlowRed
                    rfSummary.rfDensityStatus.contains("MODERATE") -> GlowAmber
                    else -> GlowCyan
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = rfSummary.rfDensityStatus,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Radar Visual Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF03070D))
                    .border(0.5.dp, Color(0xFF131D2A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = minOf(size.width, size.height) / 2.2f

                    // Concentric Radar Rings
                    listOf(0.33f, 0.66f, 1.0f).forEach { scale ->
                        drawCircle(
                            color = GlowCyan.copy(alpha = 0.15f),
                            radius = maxRadius * scale,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Radar Crosshairs
                    drawLine(
                        color = GlowCyan.copy(alpha = 0.12f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = GlowCyan.copy(alpha = 0.12f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Sweeping Beam line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        center.x + (maxRadius * cos(sweepRad)).toFloat(),
                        center.y + (maxRadius * sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        color = GlowCyan.copy(alpha = 0.8f),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.dp.toPx()
                    )

                    // User Center GPS Dot
                    drawCircle(
                        color = GlowCyan,
                        radius = 4.dp.toPx(),
                        center = center
                    )

                    // Render Cell Towers on Radar
                    rfSummary.nearbyTowers.forEachIndexed { index, tower ->
                        val angleDeg = (index * 85.0 + 35.0) % 360.0
                        val angleRad = Math.toRadians(angleDeg)
                        val normDist = (tower.distanceMeters / 600.0f).coerceIn(0.2f, 0.95f)
                        val towerPos = Offset(
                            center.x + (maxRadius * normDist * cos(angleRad)).toFloat(),
                            center.y + (maxRadius * normDist * sin(angleRad)).toFloat()
                        )

                        val towerColor = if (tower.radioType.contains("5G")) GlowAmber else GlowCyan

                        // Pulsing Outer Ring
                        drawCircle(
                            color = towerColor.copy(alpha = 0.25f),
                            radius = 10.dp.toPx(),
                            center = towerPos
                        )
                        // Core Tower Pin
                        drawCircle(
                            color = towerColor,
                            radius = 4.dp.toPx(),
                            center = towerPos
                        )
                    }
                }

                // Overlay Text Label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC070E1A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEAREST TOWER: ${rfSummary.nearestTowerDistanceM}m | RF POWER: ${String.format("%.2f", rfSummary.totalRfPowerDensityMwM2)} mW/m²",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontally Scrollable Tower Cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rfSummary.nearbyTowers) { tower ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .clickable { onTowerSelected(tower) }
                            .padding(8.dp)
                            .testTag("cell_tower_item_${tower.id}")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = if (tower.radioType.contains("5G")) GlowAmber else GlowCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tower.radioType,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (tower.radioType.contains("5G")) GlowAmber else GlowCyan
                                )
                            }
                            Text(
                                text = tower.carrierName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "${tower.frequencyBand} • ${tower.distanceMeters}m",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
