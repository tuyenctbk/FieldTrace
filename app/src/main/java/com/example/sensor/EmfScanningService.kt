package com.example.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlin.math.sqrt

class EmfScanningService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var maxEmf = 0f
    private val audioPlayer = EmfAudioPlayer()
    private var isAudioEnabled = true

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        maxEmf = 0f
        isAudioEnabled = intent?.getBooleanExtra(EXTRA_ENABLE_AUDIO, true) ?: true
        saveScanState(true, 0f)

        if (isAudioEnabled) {
            audioPlayer.setSonificationEnabled(true)
            audioPlayer.start()
        }

        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        updateAllWidgets()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        audioPlayer.stop()
        saveScanState(false, maxEmf)
        updateAllWidgets()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > maxEmf) {
            maxEmf = magnitude
            saveScanState(true, maxEmf)
            updateAllWidgets()
        }

        // Map real-time magnetic field sensor intensity to audio frequency in background
        if (isAudioEnabled) {
            audioPlayer.updateSound(magnitude, isMuted = false, warningThreshold = 80f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveScanState(active: Boolean, peakValue: Float) {
        val prefs = getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_widget_scanning", active)
            putFloat("widget_peak_emf", peakValue)
            apply()
        }
    }

    private fun updateAllWidgets() {
        val intent = Intent(this, EmfWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(application, EmfWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EMF Background Audio & Scanning Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maps real-time EMF sensor intensity to audio frequency in background."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EMF Audio Sonification Active")
            .setContentText("Listening to magnetic field intensity fluctuations...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_ENABLE_AUDIO = "extra_enable_audio"
        private const val CHANNEL_ID = "emf_background_scanning_channel"
        private const val NOTIFICATION_ID = 2026
    }
}
