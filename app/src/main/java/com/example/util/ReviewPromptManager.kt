package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager handling engagement triggers, smart calculations, cooldowns, and preferences
 * for auto-suggesting the 5-star app rating and app sharing popups at optimal moments.
 */
object ReviewPromptManager {

    private const val PREFS_NAME = "fieldtrace_review_prefs"

    // Counters
    private const val KEY_SUCCESSFUL_SCANS = "successful_scans_count"
    private const val KEY_CALIBRATIONS = "calibrations_count"
    private const val KEY_EXPORTS = "exports_count"
    private const val KEY_EVIDENCE_PHOTOS = "evidence_photos_count"

    // Rating State
    private const val KEY_HAS_RATED = "has_rated_or_declined"
    private const val KEY_LAST_RATE_PROMPT_TIME = "last_rate_prompt_timestamp"

    // Sharing State
    private const val KEY_HAS_SHARED = "has_shared_or_declined"
    private const val KEY_LAST_SHARE_PROMPT_TIME = "last_share_prompt_timestamp"

    // Cooldown Intervals
    private const val RATE_COOLDOWN_MS = 3 * 24 * 60 * 60 * 1000L // 3 days cooldown after dismissal
    private const val SHARE_COOLDOWN_MS = 2 * 24 * 60 * 60 * 1000L // 2 days cooldown after share prompt dismissal

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Increments the count of completed successful trace sessions.
     */
    fun incrementScanCount(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SUCCESSFUL_SCANS, 0) + 1
        prefs.edit().putInt(KEY_SUCCESSFUL_SCANS, count).apply()
    }

    /**
     * Increments the count of completed magnetometer calibration sessions.
     */
    fun incrementCalibrationCount(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_CALIBRATIONS, 0) + 1
        prefs.edit().putInt(KEY_CALIBRATIONS, count).apply()
    }

    /**
     * Increments the count of trace data exports (GPX/CSV/JSON).
     */
    fun incrementExportCount(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_EXPORTS, 0) + 1
        prefs.edit().putInt(KEY_EXPORTS, count).apply()
    }

    /**
     * Increments the count of photo evidence captured with HUD overlay.
     */
    fun incrementPhotoCount(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_EVIDENCE_PHOTOS, 0) + 1
        prefs.edit().putInt(KEY_EVIDENCE_PHOTOS, count).apply()
    }

    /**
     * Smart calculation evaluating whether to auto-suggest the 5-star rating popup.
     * Evaluates user satisfaction milestones (completed scans, calibration).
     */
    fun shouldShowRatePrompt(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_HAS_RATED, false)) return false

        val scanCount = prefs.getInt(KEY_SUCCESSFUL_SCANS, 0)
        val calibCount = prefs.getInt(KEY_CALIBRATIONS, 0)
        val lastPrompt = prefs.getLong(KEY_LAST_RATE_PROMPT_TIME, 0L)
        val now = System.currentTimeMillis()

        // Respect 3-day cooldown
        if (now - lastPrompt < RATE_COOLDOWN_MS) return false

        // Smart milestone threshold: At least 1 completed scan session OR at least 1 calibration session
        return scanCount >= 1 || calibCount >= 1
    }

    /**
     * Deprecated compatibility method mapping to [shouldShowRatePrompt].
     */
    fun shouldShowPrompt(context: Context): Boolean = shouldShowRatePrompt(context)

    /**
     * Smart calculation evaluating whether to auto-suggest the App Sharing popup.
     * Evaluates high value moments (data exports, photo evidence captured, or high session counts).
     */
    fun shouldShowSharePrompt(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_HAS_SHARED, false)) return false

        val exportCount = prefs.getInt(KEY_EXPORTS, 0)
        val photoCount = prefs.getInt(KEY_EVIDENCE_PHOTOS, 0)
        val scanCount = prefs.getInt(KEY_SUCCESSFUL_SCANS, 0)
        val lastSharePrompt = prefs.getLong(KEY_LAST_SHARE_PROMPT_TIME, 0L)
        val now = System.currentTimeMillis()

        // Respect 2-day cooldown
        if (now - lastSharePrompt < SHARE_COOLDOWN_MS) return false

        // Smart milestone threshold: User exported data OR took evidence photo OR created >= 3 scan sessions
        return exportCount >= 1 || photoCount >= 1 || scanCount >= 3
    }

    /**
     * Records that the 5-star rating prompt was displayed and updates the cooldown timestamp.
     */
    fun markPromptShown(context: Context) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_RATE_PROMPT_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Records that the user has rated or permanently dismissed the 5-star rating prompt.
     */
    fun markUserRatedOrDismissed(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_HAS_RATED, true)
            .apply()
    }

    /**
     * Records that the share prompt was displayed and updates the cooldown timestamp.
     */
    fun markSharePromptShown(context: Context) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_SHARE_PROMPT_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Records that the user has shared or permanently dismissed the share prompt.
     */
    fun markUserSharedOrDismissed(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_HAS_SHARED, true)
            .apply()
    }
}

