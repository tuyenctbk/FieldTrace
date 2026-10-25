package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TracePoint
import com.example.data.TraceSession
import com.example.ui.FieldTraceViewModel
import com.example.ui.components.ComparisonLineChart
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.flow.take

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCompareScreen(
    viewModel: FieldTraceViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.savedSessions.collectAsState()
    val activeUnit by viewModel.emfUnit.collectAsState()
    val warningThreshold by viewModel.warningThreshold.collectAsState()

    var sessionA by remember { mutableStateOf<TraceSession?>(null) }
    var sessionB by remember { mutableStateOf<TraceSession?>(null) }

    val pointsA = remember { mutableStateListOf<TracePoint>() }
    val pointsB = remember { mutableStateListOf<TracePoint>() }

    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }

    // Load points for Session A reactively
    LaunchedEffect(sessionA) {
        sessionA?.let { sa ->
            viewModel.getPointsForSession(sa.id).take(1).collect { fetched ->
                pointsA.clear()
                pointsA.addAll(fetched)
            }
        } ?: pointsA.clear()
    }

    // Load points for Session B reactively
    LaunchedEffect(sessionB) {
        sessionB?.let { sb ->
            viewModel.getPointsForSession(sb.id).take(1).collect { fetched ->
                pointsB.clear()
                pointsB.addAll(fetched)
            }
        } ?: pointsB.clear()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Custom Nav Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("back_button_comparison")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "ANALYTICAL OVERLAYS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Compare Sessions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Selection Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dropdown A
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, GlowCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { expandedA = true }
                    .padding(12.dp)
                    .testTag("compare_dropdown_a")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SESSION A", fontSize = 8.sp, color = GlowCyan, fontWeight = FontWeight.Bold)
                        Text(
                            text = sessionA?.name ?: "Select Session",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = GlowCyan)
                }
                DropdownMenu(
                    expanded = expandedA,
                    onDismissRequest = { expandedA = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    sessions.forEach { session ->
                        if (session.id != sessionB?.id) {
                            DropdownMenuItem(
                                text = { Text(session.name, color = Color.White) },
                                onClick = {
                                    sessionA = session
                                    expandedA = false
                                }
                            )
                        }
                    }
                }
            }

            // Dropdown B
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, GlowAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { expandedB = true }
                    .padding(12.dp)
                    .testTag("compare_dropdown_b")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SESSION B", fontSize = 8.sp, color = GlowAmber, fontWeight = FontWeight.Bold)
                        Text(
                            text = sessionB?.name ?: "Select Session",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = GlowAmber)
                }
                DropdownMenu(
                    expanded = expandedB,
                    onDismissRequest = { expandedB = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    sessions.forEach { session ->
                        if (session.id != sessionA?.id) {
                            DropdownMenuItem(
                                text = { Text(session.name, color = Color.White) },
                                onClick = {
                                    sessionB = session
                                    expandedB = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Content Area
        if (sessionA == null || sessionB == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.3f))
                    .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "SELECT TWO SESSIONS TO OVERLAY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Choose different surveying missions above to compare magnetic field peaks and spatial variance lines side-by-side.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Line Chart Overlay Composable
                ComparisonLineChart(
                    session1 = sessionA!!,
                    points1 = pointsA,
                    session2 = sessionB!!,
                    points2 = pointsB,
                    warningThreshold = warningThreshold,
                    emfUnit = activeUnit,
                    modifier = Modifier.fillMaxWidth()
                )

                // Comparison matrix cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "METRIC VARIATIONS MATRIX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan,
                            letterSpacing = 1.sp
                        )

                        // Peak comparison row
                        MetricComparisonRow(
                            label = "PEAK MAGNITUDE",
                            valA = activeUnit.format(sessionA!!.maxEmf),
                            valB = activeUnit.format(sessionB!!.maxEmf),
                            compareResult = if (sessionA!!.maxEmf > sessionB!!.maxEmf) "A is higher" else "B is higher"
                        )

                        // Avg comparison row
                        MetricComparisonRow(
                            label = "AVERAGE INTENSITY",
                            valA = activeUnit.format(sessionA!!.avgEmf),
                            valB = activeUnit.format(sessionB!!.avgEmf),
                            compareResult = if (sessionA!!.avgEmf > sessionB!!.avgEmf) "A is higher" else "B is higher"
                        )

                        // Points count
                        MetricComparisonRow(
                            label = "SAMPLED LOCATIONS",
                            valA = "${pointsA.size} pts",
                            valB = "${pointsB.size} pts",
                            compareResult = if (pointsA.size > pointsB.size) "A has more density" else "B has more density"
                        )
                    }
                }

                // Insights section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SPATIAL VARIANCE ANALYSIS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowAmber,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val peakDiff = kotlin.math.abs(sessionA!!.maxEmf - sessionB!!.maxEmf)
                        val analysisText = if (peakDiff < 5f) {
                            "Both sessions exhibit a highly matching magnetic profile with minimal peak variance (${activeUnit.format(peakDiff)}). This indicates structural stability in the surveyed environments."
                        } else {
                            val higherSession = if (sessionA!!.maxEmf > sessionB!!.maxEmf) sessionA!!.name else sessionB!!.name
                            "Anomalous variance detected! Session '$higherSession' registers significantly elevated peak intensity (${activeUnit.format(peakDiff)} higher). This suggests potential power line leaks, sub-surface conduits, or localized equipment interference."
                        }
                        Text(
                            text = analysisText,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun MetricComparisonRow(
    label: String,
    valA: String,
    valB: String,
    compareResult: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(text = compareResult.uppercase(), fontSize = 8.sp, color = GlowAmber, fontWeight = FontWeight.Black)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // A value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = valA,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan
                )
            }
            // B value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = valB,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GlowAmber
                )
            }
        }
    }
}
