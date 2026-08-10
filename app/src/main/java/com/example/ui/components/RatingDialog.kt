package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.TextMuted
import com.example.util.FirebaseManager
import com.example.util.ReviewPromptManager

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FirebaseManager.logRatingPromptShown()
    }

    AlertDialog(
        onDismissRequest = {
            ReviewPromptManager.markPromptShown(context)
            FirebaseManager.logRatingAction("dismissed_outside")
            onDismiss()
        },
        modifier = modifier.testTag("rating_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF0F172A),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(GlowAmber.copy(alpha = 0.2f))
                        .border(1.dp, GlowAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star Rating",
                        tint = GlowAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.enjoying_fieldtrace),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.rating_dialog_desc),
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rate 5 Stars Button
                Button(
                    onClick = {
                        ReviewPromptManager.markUserRatedOrDismissed(context)
                        FirebaseManager.logRatingAction("rate_5_stars")
                        openPlayStore(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("rate_5_stars_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.btn_rate_5_stars),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Share App Button
                OutlinedButton(
                    onClick = {
                        ReviewPromptManager.markPromptShown(context)
                        FirebaseManager.logRatingAction("share_app")
                        shareApp(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("share_app_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowCyan)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.btn_share_app),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Dismiss Button
                Button(
                    onClick = {
                        ReviewPromptManager.markPromptShown(context)
                        FirebaseManager.logRatingAction("remind_later")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("remind_later_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.btn_remind_later),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }
        }
    )
}

@Composable
fun ShareAppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FirebaseManager.logSharePromptShown()
    }

    AlertDialog(
        onDismissRequest = {
            ReviewPromptManager.markSharePromptShown(context)
            FirebaseManager.logShareAppAction("dismissed_outside")
            onDismiss()
        },
        modifier = modifier.testTag("share_app_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF0F172A),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(GlowCyan.copy(alpha = 0.2f))
                        .border(1.dp, GlowCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share App Icon",
                        tint = GlowCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "RECOMMEND FIELDTRACE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "Turn any smartphone into a precision EMF field scanner, audio tone sonifier, and 2D heatmap mapper! Share FieldTrace with your peers.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        ReviewPromptManager.markUserSharedOrDismissed(context)
                        FirebaseManager.logShareAppAction("share_confirm_clicked")
                        shareApp(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("share_app_confirm_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Text(
                            text = "SHARE FIELDTRACE NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        ReviewPromptManager.markSharePromptShown(context)
                        FirebaseManager.logShareAppAction("remind_later")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("share_app_dismiss_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "MAYBE LATER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }
        }
    )
}

private fun openPlayStore(context: Context) {
    try {
        val appPackage = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        val appPackage = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackage"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun shareApp(context: Context) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FieldTrace: EMF Scanner & 2D Heatmap Mapper")
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out FieldTrace! It turns your smartphone into a real-time EMF scanner, 2D spatial heatmap mapper, and audio synthesizer: https://play.google.com/store/apps/details?id=${context.packageName}"
            )
        }
        val chooser = Intent.createChooser(shareIntent, "Share FieldTrace via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

