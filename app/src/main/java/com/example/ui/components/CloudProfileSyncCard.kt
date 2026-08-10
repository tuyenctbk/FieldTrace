package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TraceRepository
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.util.FirebaseAuthManager
import com.example.util.FirestoreSyncService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * A visually polished, Material 3 user profile and cloud database synchronization card.
 * Integrates real Firebase Auth (Anonymous & Email accounts) and basic Room-to-Firestore sync service.
 */
@Composable
fun CloudProfileSyncCard(
    repository: TraceRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Auth Flows
    val currentUser by FirebaseAuthManager.currentUser.collectAsState()
    val isAnonymous by FirebaseAuthManager.isAnonymous.collectAsState()
    val userEmail by FirebaseAuthManager.userEmail.collectAsState()
    val authStatusMessage by FirebaseAuthManager.authStatusMessage.collectAsState()

    // Sync Flows
    val isSyncing by FirestoreSyncService.isSyncing.collectAsState()
    val lastSyncTime by FirestoreSyncService.lastSyncTime.collectAsState()
    val syncedCount by FirestoreSyncService.syncedSessionsCount.collectAsState()
    val syncStatusText by FirestoreSyncService.syncStatusText.collectAsState()

    var showAuthPanel by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    val formattedSyncTime = remember(lastSyncTime) {
        if (lastSyncTime > 0L) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(lastSyncTime))
        } else {
            "Never"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cloud_profile_sync_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Account Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAnonymous) GlowAmber.copy(alpha = 0.15f) else GlowCyan.copy(alpha = 0.15f))
                            .border(0.5.dp, if (isAnonymous) GlowAmber else GlowCyan, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAnonymous) Icons.Default.CloudQueue else Icons.Default.CloudDone,
                            contentDescription = "Cloud Icon",
                            tint = if (isAnonymous) GlowAmber else GlowCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isAnonymous) "GUEST CLOUD SAFE" else "AUTHENTICATED VAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnonymous) GlowAmber else GlowCyan,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (isAnonymous) "Temporary Sandbox Profile" else (userEmail ?: "Cloud Account Connected"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Auth Action Toggle Button
                IconButton(
                    onClick = {
                        showAuthPanel = !showAuthPanel
                        errorMsg = null
                        infoMsg = null
                    },
                    modifier = Modifier.testTag("toggle_auth_panel_button")
                ) {
                    Icon(
                        imageVector = if (showAuthPanel) Icons.Default.Close else Icons.Default.AccountCircle,
                        contentDescription = "Sign In Options",
                        tint = if (isAnonymous) TextMuted else GlowCyan
                    )
                }
            }

            // Expanding Authentication Panel
            AnimatedVisibility(visible = showAuthPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "CREATE NEW CLOUD ARCHIVE" else "SIGN IN TO YOUR VAULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GlowCyan,
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("email_input_field"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Master Password", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GlowCyan,
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("password_input_field"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    errorMsg?.let { msg ->
                        Text(text = "Error: $msg", color = GlowRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    infoMsg?.let { msg ->
                        Text(text = msg, color = GlowCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (emailInput.isBlank() || passwordInput.isBlank()) {
                                    errorMsg = "Please fill in all credentials"
                                    return@Button
                                }
                                errorMsg = null
                                infoMsg = null
                                if (isRegisterMode) {
                                    FirebaseAuthManager.registerWithEmailAndPassword(emailInput, passwordInput) { success, err ->
                                        if (success) {
                                            infoMsg = "Account created successfully!"
                                            showAuthPanel = false
                                        } else {
                                            errorMsg = err
                                        }
                                    }
                                } else {
                                    FirebaseAuthManager.signInWithEmailAndPassword(emailInput, passwordInput) { success, err ->
                                        if (success) {
                                            infoMsg = "Signed in successfully!"
                                            showAuthPanel = false
                                        } else {
                                            errorMsg = err
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                            modifier = Modifier.weight(1f).testTag("auth_submit_button")
                        ) {
                            Text(
                                text = if (isRegisterMode) "REGISTER" else "SIGN IN",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        if (!isAnonymous) {
                            Button(
                                onClick = {
                                    FirebaseAuthManager.signOut()
                                    infoMsg = "Signed out"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.weight(1f).testTag("auth_signout_button")
                            ) {
                                Text("SIGN OUT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    TextButton(
                        onClick = { isRegisterMode = !isRegisterMode },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an archive? Sign In" else "New to cloud? Create an archive",
                            color = GlowCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Connection Status & Last Sync Time Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CLOUD CONNECTION STATUS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(text = "LAST SYNC COMPLETED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = authStatusMessage.uppercase(),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAnonymous) GlowAmber else GlowCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedSyncTime,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sync Actions Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLOUD DISK COPIES: $syncedCount SESSIONS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = syncStatusText,
                        fontSize = 10.sp,
                        color = if (syncStatusText.contains("Error")) GlowRed else Color.White
                    )
                }

                Button(
                    onClick = {
                        FirestoreSyncService.performFullSync(repository)
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowCyan,
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("sync_cloud_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Black
                            )
                            Text(
                                "SYNC NOW",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
