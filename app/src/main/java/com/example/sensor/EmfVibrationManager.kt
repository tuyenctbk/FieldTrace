package com.example.sensor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VibrationPattern(val key: String, val displayName: String, val description: String) {
    GENTLE_HUM("GENTLE_HUM", "Gentle Hum", "Light, spaced out pulses"),
    STEADY("STEADY", "Steady Pulse", "Regular solid beats"),
    DOUBLE_PULSE("DOUBLE_PULSE", "Double Tap", "Heartbeat double beats"),
    RAPID_BUZZ("RAPID_BUZZ", "Rapid Buzz", "Extremely short, fast buzzes"),
    SOS("SOS", "S.O.S. Alert", "Distressed Morse signal warning")
}

class EmfVibrationManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val prefs = context.applicationContext.getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE)

    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean("is_vibration_enabled", true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _patternLow = MutableStateFlow(getPatternForTier("low", VibrationPattern.GENTLE_HUM))
    val patternLow: StateFlow<VibrationPattern> = _patternLow.asStateFlow()

    private val _patternMedium = MutableStateFlow(getPatternForTier("medium", VibrationPattern.STEADY))
    val patternMedium: StateFlow<VibrationPattern> = _patternMedium.asStateFlow()

    private val _patternHigh = MutableStateFlow(getPatternForTier("high", VibrationPattern.SOS))
    val patternHigh: StateFlow<VibrationPattern> = _patternHigh.asStateFlow()

    private var lastVibrateTime = 0L

    fun setVibrationEnabled(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
        prefs.edit().putBoolean("is_vibration_enabled", enabled).apply()
        if (!enabled) {
            cancel()
        }
    }

    fun setPatternForTier(tier: String, pattern: VibrationPattern) {
        prefs.edit().putString("vibrate_pattern_$tier", pattern.key).apply()
        when (tier) {
            "low" -> _patternLow.value = pattern
            "medium" -> _patternMedium.value = pattern
            "high" -> _patternHigh.value = pattern
        }
    }

    private fun getPatternForTier(tier: String, defaultPattern: VibrationPattern): VibrationPattern {
        val saved = prefs.getString("vibrate_pattern_$tier", defaultPattern.key)
        return VibrationPattern.values().firstOrNull { it.key == saved } ?: defaultPattern
    }

    /**
     * Triggers customized vibration patterns based on current EMF tier.
     */
    fun updateVibration(emfMagnitude: Float, warningThreshold: Float = 80f) {
        if (!_isVibrationEnabled.value || vibrator == null || !vibrator.hasVibrator()) return

        val now = System.currentTimeMillis()

        // 1. Determine Tier & Pattern
        val (tier, pattern) = when {
            emfMagnitude > warningThreshold -> "high" to _patternHigh.value
            emfMagnitude > 35f -> "medium" to _patternMedium.value
            emfMagnitude > 15f -> "low" to _patternLow.value
            else -> return // Ambient safe field: silent
        }

        // 2. Map Pattern to timing parameters & intervals
        val pulseIntervalMs = when (pattern) {
            VibrationPattern.GENTLE_HUM -> 800L
            VibrationPattern.STEADY -> 400L
            VibrationPattern.DOUBLE_PULSE -> 600L
            VibrationPattern.RAPID_BUZZ -> 200L
            VibrationPattern.SOS -> 1400L
        }

        if (now - lastVibrateTime < pulseIntervalMs) {
            return
        }
        lastVibrateTime = now

        // 3. Play selected pattern using Waveform (API 26+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (pattern) {
                    VibrationPattern.GENTLE_HUM -> {
                        VibrationEffect.createOneShot(30L, 50)
                    }
                    VibrationPattern.STEADY -> {
                        VibrationEffect.createOneShot(60L, 120)
                    }
                    VibrationPattern.DOUBLE_PULSE -> {
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 80, 30),
                            intArrayOf(0, 150, 0, 150),
                            -1
                        )
                    }
                    VibrationPattern.RAPID_BUZZ -> {
                        VibrationEffect.createOneShot(100L, 255)
                    }
                    VibrationPattern.SOS -> {
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 60, 40, 60, 40, 60, 100, 100, 100, 100, 100, 40, 60, 40, 60, 40, 60),
                            intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255),
                            -1
                        )
                    }
                }
                vibrator.vibrate(effect)
            } else {
                val duration = when (pattern) {
                    VibrationPattern.GENTLE_HUM -> 20L
                    VibrationPattern.STEADY -> 50L
                    VibrationPattern.DOUBLE_PULSE -> 150L
                    VibrationPattern.RAPID_BUZZ -> 100L
                    VibrationPattern.SOS -> 400L
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
