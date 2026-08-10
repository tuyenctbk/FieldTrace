package com.example.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FirebaseManager {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            if (firebaseAnalytics == null) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
                firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
            }
            if (crashlytics == null) {
                crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics?.setCrashlyticsCollectionEnabled(true)
            }
            isInitialized = true
            
            // Log initial app launch event
            logEvent("app_session_initialized")
        } catch (e: Exception) {
            // Graceful fallback if google-services config is absent in test/offline environments
        }
    }

    fun logScreenView(context: Context, screenName: String) {
        try {
            if (!isInitialized) {
                init(context)
            }
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            crashlytics?.setCustomKey("last_active_screen", screenName)
            crashlytics?.log("Navigated to screen: $screenName")
        } catch (e: Exception) {
            // Non-blocking catch
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params ?: Bundle())
        } catch (e: Exception) {
            // Non-blocking catch
        }
    }

    fun logTraceStarted(sessionName: String) {
        try {
            val bundle = Bundle().apply {
                putString("session_name", sessionName)
                putLong("timestamp", System.currentTimeMillis())
            }
            logEvent("trace_recording_started", bundle)
            crashlytics?.setCustomKey("is_recording", true)
            crashlytics?.log("Started trace recording: $sessionName")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logTraceStopped(pointsCount: Int, maxEmf: Float, avgEmf: Float) {
        try {
            val bundle = Bundle().apply {
                putInt("points_count", pointsCount)
                putFloat("max_emf", maxEmf)
                putFloat("avg_emf", avgEmf)
            }
            logEvent("trace_recording_stopped", bundle)
            crashlytics?.setCustomKey("is_recording", false)
            crashlytics?.setCustomKey("last_max_emf", maxEmf)
            crashlytics?.log("Stopped trace recording with $pointsCount points, max $maxEmf uT")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logCalibrationCompleted(accuracy: Int) {
        try {
            val bundle = Bundle().apply {
                putInt("accuracy_level", accuracy)
            }
            logEvent("calibration_completed", bundle)
            crashlytics?.setCustomKey("sensor_accuracy", accuracy)
            crashlytics?.log("Sensor calibration completed with accuracy $accuracy")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logWireDetectionTriggered(confidence: Float, frequency: Float) {
        try {
            val bundle = Bundle().apply {
                putFloat("confidence", confidence)
                putFloat("frequency_hz", frequency)
            }
            logEvent("wire_detection_triggered", bundle)
            crashlytics?.log("Wire detection triggered: confidence=$confidence, freq=$frequency")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logDataExported(format: String, pointsCount: Int) {
        try {
            val bundle = Bundle().apply {
                putString("export_format", format)
                putInt("points_count", pointsCount)
            }
            logEvent("data_exported", bundle)
            crashlytics?.log("Exported data format: $format ($pointsCount points)")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logPhotoCaptured(emfMagnitude: Float) {
        try {
            val bundle = Bundle().apply {
                putFloat("emf_magnitude", emfMagnitude)
            }
            logEvent("photo_evidence_captured", bundle)
            crashlytics?.log("Captured photo evidence at $emfMagnitude uT")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logRatingPromptShown() {
        try {
            logEvent("rating_prompt_shown")
            crashlytics?.log("Rating prompt shown to user")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logRatingAction(action: String) {
        try {
            val bundle = Bundle().apply {
                putString("action", action)
            }
            logEvent("rating_prompt_action", bundle)
            crashlytics?.setCustomKey("rating_action", action)
            crashlytics?.log("User took rating action: $action")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logSharePromptShown() {
        try {
            logEvent("share_prompt_shown")
            crashlytics?.log("Share prompt shown to user")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logShareAppAction(method: String) {
        try {
            val bundle = Bundle().apply {
                putString("share_method", method)
            }
            logEvent("share_app_action", bundle)
            crashlytics?.log("User shared app via method: $method")
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logError(throwable: Throwable, message: String? = null) {
        try {
            message?.let { crashlytics?.log(it) }
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            // Non-blocking catch
        }
    }

    fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics?.setUserProperty(name, value)
            crashlytics?.setCustomKey(name, value)
        } catch (e: Exception) {
            // Non-blocking catch
        }
    }
}

