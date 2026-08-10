package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sensor.VibrationPattern
import com.example.ui.FieldTraceViewModel
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted

@Composable
fun VibrationCustomizerDialog(
    viewModel: FieldTraceViewModel,
    onDismiss: () -> Unit
) {
    val isVibeEnabled by viewModel.isVibrationEnabled.collectAsState()
    val patternLow by viewModel.patternLow.collectAsState()
    val patternMedium by viewModel.patternMedium.collectAsState()
    val patternHigh by viewModel.patternHigh.collectAsState()

    var expandedLow by remember { mutableStateOf(false) }
    var expandedMedium by remember { mutableStateOf(false) }
    var expandedHigh by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(0.5.dp, GlowCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "TACTILE SYNTHESIZER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlowCyan,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Vibration Patterns",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_vibration_customizer")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Assign distinct haptic profiles to different EMF field intensities. This lets you blind-survey magnetic boundaries entirely through physical touch sensations.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )

                // 1. LOW INTENSITY CONFIG (< 35 uT)
                VibrationTierSelector(
                    label = "LOW INTENSITY FIELD (< 35 uT)",
                    color = GlowCyan,
                    currentPattern = patternLow,
                    expanded = expandedLow,
                    onExpandedChange = { expandedLow = it },
                    onPatternSelect = {
                        viewModel.setVibrationPatternForTier("low", it)
                        expandedLow = false
                    },
                    onTestClick = {
                        // Test haptic response instantly
                        viewModel.setVibrationPatternForTier("low", patternLow)
                        viewModel.triggerManualVibrationTest(30f)
                    },
                    testTag = "low"
                )

                // 2. MEDIUM INTENSITY CONFIG (35 - 80 uT)
                VibrationTierSelector(
                    label = "MEDIUM INTENSITY FIELD (35 - 80 uT)",
                    color = GlowAmber,
                    currentPattern = patternMedium,
                    expanded = expandedMedium,
                    onExpandedChange = { expandedMedium = it },
                    onPatternSelect = {
                        viewModel.setVibrationPatternForTier("medium", it)
                        expandedMedium = false
                    },
                    onTestClick = {
                        viewModel.setVibrationPatternForTier("medium", patternMedium)
                        viewModel.triggerManualVibrationTest(60f)
                    },
                    testTag = "medium"
                )

                // 3. HIGH INTENSITY CONFIG (> 80 uT)
                VibrationTierSelector(
                    label = "HIGH / WARNING FIELD (> 80 uT)",
                    color = GlowRed,
                    currentPattern = patternHigh,
                    expanded = expandedHigh,
                    onExpandedChange = { expandedHigh = it },
                    onPatternSelect = {
                        viewModel.setVibrationPatternForTier("high", it)
                        expandedHigh = false
                    },
                    onTestClick = {
                        viewModel.setVibrationPatternForTier("high", patternHigh)
                        viewModel.triggerManualVibrationTest(120f)
                    },
                    testTag = "high"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowCyan)
                ) {
                    Text("SAVE & CONFIRM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VibrationTierSelector(
    label: String,
    color: Color,
    currentPattern: VibrationPattern,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPatternSelect: (VibrationPattern) -> Unit,
    onTestClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .clickable { onExpandedChange(true) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("pattern_selector_$testTag")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentPattern.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(currentPattern.description, fontSize = 8.sp, color = TextMuted)
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) },
                        modifier = Modifier.background(Color(0xFF0F172A))
                    ) {
                        VibrationPattern.values().forEach { pattern ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(pattern.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(pattern.description, color = TextMuted, fontSize = 8.sp)
                                    }
                                },
                                onClick = { onPatternSelect(pattern) }
                            )
                        }
                    }
                }

                // Test click haptic button
                IconButton(
                    onClick = onTestClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(0.5.dp, color, RoundedCornerShape(8.dp))
                        .testTag("test_pattern_$testTag")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test vibration",
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
