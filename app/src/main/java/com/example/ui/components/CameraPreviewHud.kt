package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.EmfUnit
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowRed
import com.example.ui.theme.TextMuted
import com.example.util.PhotoEvidenceManager

@Composable
fun CameraPreviewHud(
    emfMag: Float,
    emfUnit: EmfUnit,
    latitude: Double,
    longitude: Double,
    isAutoCaptureEnabled: Boolean,
    onToggleAutoCapture: (Boolean) -> Unit,
    onPhotoCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        // Adjust rotation if needed
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val rotatedBitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                        } else {
                            originalBitmap
                        }

                        val savedPath = PhotoEvidenceManager.saveStampedEvidencePhoto(
                            context = context,
                            rawBitmap = rotatedBitmap,
                            emfMagnitude = emfMag,
                            emfUnit = emfUnit,
                            latitude = latitude,
                            longitude = longitude
                        )

                        if (savedPath != null) {
                            onPhotoCaptured(savedPath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        image.close()
                        isCapturing = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    isCapturing = false
                }
            }
        )
    }

    // Auto-Capture Trigger on High Field Spikes
    val convertedEmf = emfUnit.convert(emfMag)
    val isAnomaly = convertedEmf > (80f * emfUnit.scaleFactor)

    LaunchedEffect(isAnomaly, isAutoCaptureEnabled) {
        if (isAutoCaptureEnabled && isAnomaly && !isCapturing) {
            capturePhoto()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cap = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = cap

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                cap
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("CAMERA HUD PERMISSION REQUIRED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Grant camera access to view live field overlay and capture watermarked anomaly photos.", fontSize = 11.sp, color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowCyan)
                ) {
                    Text("GRANT CAMERA PERMISSION", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // HUD Overlay Visualizer (Target Reticle Grid)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2, h / 2)
            val color = if (isAnomaly) GlowRed else GlowCyan

            // Reticle Circles
            drawCircle(color = color.copy(alpha = 0.25f), radius = 80.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            drawCircle(color = color.copy(alpha = 0.1f), radius = 150.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

            // Crosshairs
            drawLine(color = color.copy(alpha = 0.3f), start = Offset(center.x - 40.dp.toPx(), center.y), end = Offset(center.x + 40.dp.toPx(), center.y), strokeWidth = 2f)
            drawLine(color = color.copy(alpha = 0.3f), start = Offset(center.x, center.y - 40.dp.toPx()), end = Offset(center.x, center.y + 40.dp.toPx()), strokeWidth = 2f)
        }

        // Top HUD Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CAMERA HUD EVIDENCE MODE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GlowCyan, letterSpacing = 1.sp)
                Text(emfUnit.format(emfMag), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = if (isAnomaly) GlowRed else Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AUTO-SNAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isAutoCaptureEnabled) GlowAmber else TextMuted)
                Switch(
                    checked = isAutoCaptureEnabled,
                    onCheckedChange = onToggleAutoCapture,
                    colors = SwitchDefaults.colors(checkedThumbColor = GlowAmber, checkedTrackColor = GlowAmber.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("auto_snap_switch")
                )
            }
        }

        // Bottom Shutter Button
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) GlowAmber else GlowCyan)
                        .clickable { capturePhoto() }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Camera, contentDescription = "Snap Evidence Photo", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
