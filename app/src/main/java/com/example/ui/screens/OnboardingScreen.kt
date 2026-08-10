package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.BackgroundBlack
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var isPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        isPermissionGranted = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FIELDTRACE PRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = GlowCyan,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = if (onClose != null) "USER GUIDE & ONBOARDING" else "SYSTEM ONBOARDING",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (onClose != null) GlowCyan else Color(0xFF1E293B))
                    .clickable {
                        if (onClose != null) {
                            onClose()
                        } else {
                            onCompleteOnboarding()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("skip_onboarding_button")
            ) {
                Text(
                    text = if (onClose != null) "CLOSE GUIDE" else "SKIP TO APP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (onClose != null) Color.Black else TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        // Middle Content Pager Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (currentPage) {
                0 -> OnboardingPageScanner()
                1 -> OnboardingPageSpatialHeatmap()
                2 -> OnboardingPageCalibration()
                3 -> OnboardingPagePermissions(
                    isPermissionGranted = isPermissionGranted,
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..3) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentPage) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentPage) GlowCyan else Color(0xFF1E293B)
                            )
                            .clickable { currentPage = i }
                    )
                }
            }

            // Navigation Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("onboarding_back_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Text(
                            text = "PREVIOUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < 3) {
                            currentPage++
                        } else {
                            onCompleteOnboarding()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (currentPage < 3) "NEXT STEP" else "GET STARTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageScanner() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(GlowCyan.copy(alpha = 0.15f))
                            .border(1.dp, GlowCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillBadge(text = "LIVE OSCILLOSCOPE", color = GlowCyan)
                        PillBadge(text = "SONIFICATION SYNTH", color = GlowAmber)
                        PillBadge(text = "AC CONDUIT ALERT", color = GlowRed)
                    }

                    Text(
                        text = "84.2 µT",
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = GlowAmber
                    )
                }
            }
        }

        Text(
            text = "REAL-TIME EMF SCANNER & AUDIO",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Scan magnetic intensity values in microteslas (µT). Detect concealed AC electrical wiring and turn on Real-Time Sonification to 'hear' electromagnetic fields through an audio frequency synthesizer.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun OnboardingPageSpatialHeatmap() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val radius = 30.dp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowRed.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(w * 0.35f, h * 0.4f),
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = Offset(w * 0.35f, h * 0.4f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowCyan.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(w * 0.7f, h * 0.6f),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(w * 0.7f, h * 0.6f)
                    )

                    drawCircle(color = GlowRed, radius = 5.dp.toPx(), center = Offset(w * 0.35f, h * 0.4f))
                    drawCircle(color = GlowCyan, radius = 5.dp.toPx(), center = Offset(w * 0.7f, h * 0.6f))
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "2D SPATIAL COORDINATE GRID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Text(
            text = "SPATIAL HEATMAP MAPPING",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Record multi-point trace sessions to render coordinate-based 2D spatial heatmaps. Adjust heat blur radiuses, filter noise floor thresholds, and inspect individual spatial nodes.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FigureEightMotionIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration_animation")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "illustration_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val scaleX = size.width * 0.35f
        val scaleY = size.height * 0.25f

        // Figure-eight parametric guide path
        val path = Path()
        val numPoints = 80
        for (i in 0..numPoints) {
            val t = (i.toFloat() / numPoints) * 2 * Math.PI.toFloat()
            val x = centerX + scaleX * kotlin.math.sin(t)
            val y = centerY + scaleY * kotlin.math.sin(t) * kotlin.math.cos(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Draw guide line
        drawPath(
            path = path,
            color = Color(0xFF1E293B),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // Draw active tracking line
        drawPath(
            path = path,
            color = GlowCyan.copy(alpha = 0.3f),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Traveling guiding light particle
        val tx = centerX + scaleX * kotlin.math.sin(animPhase)
        val ty = centerY + scaleY * kotlin.math.sin(animPhase) * kotlin.math.cos(animPhase)

        drawCircle(
            color = GlowCyan.copy(alpha = 0.4f),
            radius = 12.dp.toPx(),
            center = Offset(tx, ty)
        )
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(tx, ty)
        )
    }
}

@Composable
private fun OnboardingPageCalibration() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                FigureEightMotionIllustration()
            }
        }

        Text(
            text = "MAGNETOMETER CALIBRATION",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Zero out local phone hard-iron offsets and metallic interference before scanning. Rotate your phone in a 3D figure-eight pattern until sensor baseline coverage reaches high precision.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun OnboardingPagePermissions(
    isPermissionGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlowCyan)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isPermissionGranted) GlowCyan.copy(alpha = 0.2f) else GlowAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPermissionGranted) GlowCyan else GlowAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isPermissionGranted) "SYSTEM PERMISSIONS GRANTED" else "GRANT LOCATION & SENSORS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "GPS Location access allows FieldTrace Pro to tag latitude and longitude coordinates onto spatial heatmaps during trace sessions.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("request_permission_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPermissionGranted) Color(0xFF1E293B) else GlowCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isPermissionGranted) GlowCyan else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPermissionGranted) "PERMISSION ACTIVE" else "GRANT LOCATION ACCESS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPermissionGranted) Color.White else Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "READY TO START TRACING",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Click 'GET STARTED' below to launch directly into the Scanner console.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PillBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
