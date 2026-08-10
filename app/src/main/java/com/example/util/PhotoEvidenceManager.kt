package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.EmfUnit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoEvidenceManager {

    /**
     * Watermarks a bitmap with live sensor metrics, location, and hazard rating badge.
     * Saves the processed image to context.filesDir/trace_photos/ and returns the absolute filepath.
     */
    fun saveStampedEvidencePhoto(
        context: Context,
        rawBitmap: Bitmap,
        emfMagnitude: Float,
        emfUnit: EmfUnit,
        latitude: Double,
        longitude: Double,
        sessionName: String? = null
    ): String? {
        return try {
            val width = rawBitmap.width
            val height = rawBitmap.height

            // Create mutable working copy
            val mutableBitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val formattedEmf = emfUnit.format(emfMagnitude)
            val convertedVal = emfUnit.convert(emfMagnitude)

            val hazardText = when {
                convertedVal > (80f * emfUnit.scaleFactor) -> "CRITICAL ANOMALY"
                convertedVal > (50f * emfUnit.scaleFactor) -> "ELEVATED FIELD"
                else -> "SAFE LEVEL"
            }
            val hazardColor = when {
                convertedVal > (80f * emfUnit.scaleFactor) -> Color.parseColor("#EF4444")
                convertedVal > (50f * emfUnit.scaleFactor) -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#06B6D4")
            }

            // Draw Bottom Cyberpunk Translucent Watermark Banner
            val bannerHeight = (height * 0.18f).coerceAtLeast(140f)
            val bannerRect = RectF(0f, height - bannerHeight, width.toFloat(), height.toFloat())

            val shadowPaint = Paint().apply {
                shader = LinearGradient(
                    0f, height - bannerHeight, 0f, height.toFloat(),
                    Color.TRANSPARENT, Color.parseColor("#E60B1120"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(bannerRect, shadowPaint)

            // Paint configurations
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = (bannerHeight * 0.18f).coerceAtLeast(24f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }

            val accentPaint = Paint().apply {
                color = hazardColor
                textSize = (bannerHeight * 0.28f).coerceAtLeast(36f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }

            val mutedPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = (bannerHeight * 0.14f).coerceAtLeast(18f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                isAntiAlias = true
            }

            // Render Banner Content
            val marginX = width * 0.04f
            val startY = height - bannerHeight + (bannerHeight * 0.3f)

            // Header Row: App Identity & Hazard Badge
            canvas.drawText("FIELDTRACE PRO · ANOMALY EVIDENCE", marginX, startY, textPaint)
            
            // Large EMF Value Readout
            val emfY = startY + (bannerHeight * 0.32f)
            canvas.drawText("⚡ $formattedEmf", marginX, emfY, accentPaint)

            // Bottom Subtext: GPS & Timestamp
            val gpsStr = String.format(Locale.US, "GPS: %.5f°, %.5f°  |  %s", latitude, longitude, dateStr)
            val subY = emfY + (bannerHeight * 0.22f)
            canvas.drawText(gpsStr, marginX, subY, mutedPaint)

            // Hazard Status Badge Box on top-right of banner
            val badgePaint = Paint().apply {
                color = hazardColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val badgeFill = Paint().apply {
                color = hazardColor
                alpha = 40
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val badgeTextPaint = Paint().apply {
                color = hazardColor
                textSize = (bannerHeight * 0.16f).coerceAtLeast(20f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val badgeWidth = badgeTextPaint.measureText(hazardText) + 30f
            val badgeLeft = width - marginX - badgeWidth
            val badgeTop = startY - 20f
            val badgeRight = width - marginX
            val badgeBottom = startY + 25f

            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
            canvas.drawRoundRect(badgeRect, 10f, 10f, badgeFill)
            canvas.drawRoundRect(badgeRect, 10f, 10f, badgePaint)
            canvas.drawText(hazardText, badgeLeft + 15f, startY + 10f, badgeTextPaint)

            // Save to file
            val storageDir = File(context.filesDir, "trace_photos").apply { if (!exists()) mkdirs() }
            val fileName = "TRACE_EVIDENCE_${System.currentTimeMillis()}.jpg"
            val file = File(storageDir, fileName)

            FileOutputStream(file).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Purges orphaned photo files when a session is deleted.
     */
    fun deletePhotoFile(photoPath: String?) {
        if (photoPath.isNullOrBlank()) return
        try {
            val file = File(photoPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
