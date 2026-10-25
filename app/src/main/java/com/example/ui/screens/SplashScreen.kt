package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }

    val transition = rememberInfiniteTransition(label = "splash_wave_anim")
    val pulseRadius by transition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        logoAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700)
        )
        delay(1600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C192E), Color(0xFF03070E)),
                    center = Offset.Unspecified,
                    radius = 900f
                )
            )
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        // Animated Electromagnetic Radial Pulse
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = GlowCyan.copy(alpha = (1.0f - (pulseRadius / 120f)).coerceIn(0f, 0.4f)),
                radius = pulseRadius * 3.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
                .padding(24.dp)
        ) {
            // Glowing Scanner Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = GlowCyan.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = GlowCyan,
                        radius = 28.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = GlowCyan.copy(alpha = 0.5f),
                        radius = 16.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = GlowCyan,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FIELDTRACE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TextWhite,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlowCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "v1.7.0 • NOAA SWPC & OpenCelliD Integrated",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}
