package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FieldTraceViewModel
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TracerScreen
import com.example.ui.screens.OnboardingScreen
import android.content.Context
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.BackgroundBlack
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted

import com.example.util.FirebaseManager
import com.example.util.FirebaseAuthManager

import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseManager.init(this)
        FirebaseAuthManager.init(this)
        com.example.util.FirestoreSyncWorker.schedule(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: FieldTraceViewModel = viewModel()
            val isNightMode by viewModel.isNightMode.collectAsState()
            MyApplicationTheme(darkTheme = isNightMode) {
                MainAppFlow()
            }
        }
    }
}

enum class FieldTraceTab {
    Scanner,
    Tracer,
    History
}

@Composable
fun MainAppFlow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE) }
    var isOnboardingCompleted by remember {
        mutableStateOf(prefs.getBoolean("onboarding_completed", false))
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onCompleteOnboarding = {
                prefs.edit().putBoolean("onboarding_completed", true).apply()
                isOnboardingCompleted = true
            }
        )
    } else {
        MainContent()
    }
}

@Composable
fun MainContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: FieldTraceViewModel = viewModel()
    var currentTab by remember { mutableStateOf(FieldTraceTab.Scanner) }

    // Dynamic runtime permissions request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle results silently; the LocationTracker reacts gracefully
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(currentTab) {
        FirebaseManager.logScreenView(context, currentTab.name)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TAB 1: SCANNER
                val scannerActive = currentTab == FieldTraceTab.Scanner
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentTab = FieldTraceTab.Scanner }
                        .testTag("nav_scanner_tab")
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (scannerActive) GlowCyan else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = stringResource(R.string.desc_tab_scanner),
                        tint = if (scannerActive) GlowCyan else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tab_live),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (scannerActive) GlowCyan else TextMuted.copy(alpha = 0.4f)
                    )
                }

                // TAB 2: MAPPER
                val tracerActive = currentTab == FieldTraceTab.Tracer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentTab = FieldTraceTab.Tracer }
                        .testTag("nav_tracer_tab")
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (tracerActive) GlowCyan else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.desc_tab_tracer),
                        tint = if (tracerActive) GlowCyan else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tab_map),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (tracerActive) GlowCyan else TextMuted.copy(alpha = 0.4f)
                    )
                }

                // TAB 3: ARCHIVE
                val historyActive = currentTab == FieldTraceTab.History
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { currentTab = FieldTraceTab.History }
                        .testTag("nav_history_tab")
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (historyActive) GlowCyan else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.desc_tab_history),
                        tint = if (historyActive) GlowCyan else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tab_vault),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (historyActive) GlowCyan else TextMuted.copy(alpha = 0.4f)
                    )
                }
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                FieldTraceTab.Scanner -> ScannerScreen(viewModel = viewModel)
                FieldTraceTab.Tracer -> TracerScreen(viewModel = viewModel)
                FieldTraceTab.History -> HistoryScreen(viewModel = viewModel)
            }
        }
    }
}
