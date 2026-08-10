package com.example.sensor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class EmfWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SCAN) {
            val prefs = context.getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE)
            val isScanning = prefs.getBoolean("is_widget_scanning", false)

            val serviceIntent = Intent(context, EmfScanningService::class.java)
            if (isScanning) {
                context.stopService(serviceIntent)
                prefs.edit().putBoolean("is_widget_scanning", false).apply()
            } else {
                ContextCompat.startForegroundService(context, serviceIntent)
                prefs.edit().putBoolean("is_widget_scanning", true).apply()
            }

            // Update widgets immediately
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EmfWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_TOGGLE_SCAN = "com.example.sensor.ACTION_TOGGLE_SCAN"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE)
            val isScanning = prefs.getBoolean("is_widget_scanning", false)
            val peakValue = prefs.getFloat("widget_peak_emf", 0f)

            val views = RemoteViews(context.packageName, R.layout.emf_widget_layout)

            // Setup display values
            views.setTextViewText(R.id.widget_peak_value, String.format("%.1f uT", peakValue))

            if (isScanning) {
                views.setTextViewText(R.id.widget_status, "SCAN ACTIVE")
                views.setTextColor(R.id.widget_status, Color.parseColor("#10B981")) // active emerald green
                views.setTextViewText(R.id.widget_toggle_button, "STOP")
            } else {
                views.setTextViewText(R.id.widget_status, "SCAN INACTIVE")
                views.setTextColor(R.id.widget_status, Color.parseColor("#64748B")) // slate gray
                views.setTextViewText(R.id.widget_toggle_button, "START")
            }

            // PendingIntent for button click to toggle scanner
            val toggleIntent = Intent(context, EmfWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_SCAN
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)

            // PendingIntent to launch MainActivity when clicking the widget body
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_peak_value, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
