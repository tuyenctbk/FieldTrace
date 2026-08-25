package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.TracePoint
import com.example.data.TraceSession
import com.example.ui.FieldTraceViewModel
import com.example.ui.components.SpatialHeatmapGrid
import com.example.ui.components.CloudProfileSyncCard
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.CsvJsonExporter
import com.example.util.GpxExporter
import com.example.util.TraceAnalyticsCalculator
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: FieldTraceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessions by viewModel.savedSessions.collectAsState()
    val warningThreshold by viewModel.warningThreshold.collectAsState()

    var selectedSessionForMapViewer by remember { mutableStateOf<TraceSession?>(null) }
    var selectedSessionForExport by remember { mutableStateOf<TraceSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<TraceSession?>(null) }
    var showComparisonScreen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.vault_title),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.vault_subtitle),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TOTAL RECORDS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sessions.size}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = GlowCyan
                )
            }
        }

        // Cloud Profile & Firestore Sync Card
        CloudProfileSyncCard(
            repository = viewModel.repository,
            modifier = Modifier.fillMaxWidth()
        )

        if (sessions.size >= 2) {
            Button(
                onClick = { showComparisonScreen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("compare_sessions_trigger_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GlowCyan.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowCyan)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    tint = GlowCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("OVERLAY & COMPARE TWO SESSIONS", color = GlowCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }

        if (sessions.isEmpty()) {
            EmptyHistoryPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("sessions_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val activeUnit by viewModel.emfUnit.collectAsState()
                    SessionHistoryCard(
                        session = session,
                        emfUnit = activeUnit,
                        onShare = { selectedSessionForExport = session },
                        onDelete = { sessionToDelete = session },
                        onViewMapAndAnalytics = { selectedSessionForMapViewer = session }
                    )
                }
            }
        }

        // Export Format Selector Dialog
        selectedSessionForExport?.let { session ->
            AlertDialog(
                onDismissRequest = { selectedSessionForExport = null },
                title = { Text("EXPORT SESSION DATA") },
                text = {
                    Text(
                        text = "Select format to export '${session.name}':",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.getPointsForSession(session.id).take(1).collect { points ->
                                        val gpx = GpxExporter.exportToGpx(session, points)
                                        shareTextData(context, session, gpx, "application/gpx+xml", "gpx")
                                        viewModel.onExportCompleted("GPX", points.size)
                                        selectedSessionForExport = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GlowCyan)
                        ) {
                            Text("EXPORT GPX (GPS TRACK)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.getPointsForSession(session.id).take(1).collect { points ->
                                        val csv = CsvJsonExporter.exportToCsv(session, points)
                                        shareTextData(context, session, csv, "text/csv", "csv")
                                        viewModel.onExportCompleted("CSV", points.size)
                                        selectedSessionForExport = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("EXPORT CSV (SPREADSHEET)", color = GlowCyan, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.getPointsForSession(session.id).take(1).collect { points ->
                                        val json = CsvJsonExporter.exportToJson(session, points)
                                        shareTextData(context, session, json, "application/json", "json")
                                        viewModel.onExportCompleted("JSON", points.size)
                                        selectedSessionForExport = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("EXPORT JSON (DATA OBJECT)", color = GlowAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Delete Confirmation Alert
        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(stringResource(R.string.purge_title)) },
                text = {
                    Text(
                        stringResource(R.string.purge_confirm),
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSession(session.id)
                            sessionToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.btn_purge))
                    }
                },
                dismissButton = {
                    Button(onClick = { sessionToDelete = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // Full-Screen Interactive Session Map & Analytics Modal
        selectedSessionForMapViewer?.let { session ->
            Dialog(
                onDismissRequest = { selectedSessionForMapViewer = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                TraceSessionViewerModal(
                    session = session,
                    viewModel = viewModel,
                    warningThreshold = warningThreshold,
                    onDismiss = { selectedSessionForMapViewer = null }
                )
            }
        }

        // Full-Screen Session Comparison Modal
        if (showComparisonScreen) {
            Dialog(
                onDismissRequest = { showComparisonScreen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                SessionCompareScreen(
                    viewModel = viewModel,
                    onDismiss = { showComparisonScreen = false }
                )
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun TraceSessionViewerModal(
    session: TraceSession,
    viewModel: FieldTraceViewModel,
    warningThreshold: Float,
    onDismiss: () -> Unit
) {
    val points = remember { mutableStateListOf<TracePoint>() }

    LaunchedEffect(session.id) {
        viewModel.getPointsForSession(session.id).take(1).collect { fetched ->
            points.clear()
            points.addAll(fetched)
        }
    }

    val distanceMeters = remember(points) { TraceAnalyticsCalculator.calculateDistanceMeters(points) }
    val durationMs = remember(points) { TraceAnalyticsCalculator.calculateDurationMs(points) }
    val anomalyPct = remember(points, warningThreshold) { TraceAnalyticsCalculator.calculateAnomalyPercentage(points, warningThreshold) }
    val peakPoint = remember(points) { TraceAnalyticsCalculator.getPeakPoint(points) }
    val riskRating = remember(session.maxEmf, anomalyPct) { TraceAnalyticsCalculator.getRiskRating(session.maxEmf, anomalyPct) }

    val formattedDuration = remember(durationMs) {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        String.format(Locale.US, "%02dm %02ds", minutes, seconds)
    }

    val formattedDate = remember(session.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        sdf.format(Date(session.timestamp))
    }

    val riskColor = when (riskRating) {
        "CRITICAL" -> GlowRed
        "ELEVATED" -> GlowAmber
        else -> GlowCyan
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Modal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HISTORICAL SPATIAL MAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlowCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = session.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_session_viewer_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        val activeUnit by viewModel.emfUnit.collectAsState()
        SpatialHeatmapGrid(
            points = points,
            threshold = warningThreshold,
            emfUnit = activeUnit
        )

        // Historical Trend Line Chart for EMF intensity
        if (points.isNotEmpty()) {
            com.example.ui.components.HistoricalTrendLineChart(
                points = points,
                warningThreshold = warningThreshold,
                emfUnit = activeUnit
            )
        }

        // Session Analytics Card Summary Matrix
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SESSION ANALYTICS & INSIGHTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.2f))
                            .border(0.5.dp, riskColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "RISK: $riskRating",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = riskColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "PATH DISTANCE", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (distanceMeters >= 1000) String.format(Locale.US, "%.2f km", distanceMeters / 1000.0) else String.format(Locale.US, "%.1f m", distanceMeters),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Column {
                        Text(text = "DURATION", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = formattedDuration,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "ANOMALY RATE", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = String.format(Locale.US, "%.1f%%", anomalyPct),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (anomalyPct > 20f) GlowRed else GlowCyan
                        )
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                // Peak Anomaly Node Summary
                peakPoint?.let { pt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "PEAK MAGNITUDE NODE", fontSize = 9.sp, color = GlowAmber, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "GPS: %.5f, %.5f", pt.latitude, pt.longitude),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        val activeUnit by viewModel.emfUnit.collectAsState()
                        Text(
                            text = activeUnit.format(pt.emfMagnitude),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = GlowRed
                        )
                    }
                }

                if (session.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${session.notes}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Close Action Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "CLOSE MAP VIEWER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_vault_vector_illustration),
            contentDescription = "Empty history vault illustration",
            modifier = Modifier.size(140.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.no_records_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_records_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun SessionHistoryCard(
    session: TraceSession,
    emfUnit: com.example.data.EmfUnit = com.example.data.EmfUnit.MICROTESLA,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onViewMapAndAnalytics: () -> Unit
) {
    val formattedDate = remember(session.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        sdf.format(Date(session.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_card_${session.id}")
            .border(0.5.dp, Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                val severityColor = when {
                    session.maxEmf > 100f -> GlowRed
                    session.maxEmf > 60f -> GlowAmber
                    else -> GlowCyan
                }
                val severityText = when {
                    session.maxEmf > 100f -> "HIGH"
                    session.maxEmf > 60f -> "ELEVATED"
                    else -> "SAFE"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .border(0.5.dp, severityColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = severityText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Stats summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "PEAK FIELD", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = emfUnit.format(session.maxEmf),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(text = "AVG FIELD", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = emfUnit.format(session.avgEmf),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "RECORD DATA", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = "GPX/CSV/JSON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan
                    )
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onViewMapAndAnalytics,
                    modifier = Modifier.testTag("view_session_map_button_${session.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = "View Map", tint = GlowCyan, modifier = Modifier.size(16.dp))
                        Text(text = "MAP & ANALYTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlowCyan)
                    }
                }

                Row {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_data_button_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share data file",
                            tint = GlowCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_session_button_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete record",
                            tint = GlowRed
                        )
                    }
                }
            }
        }
    }
}

fun shareTextData(context: Context, session: TraceSession, content: String, mimeType: String, fileExt: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "FieldTrace Export [${fileExt.uppercase()}]: ${session.name}")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Session ($fileExt)"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
