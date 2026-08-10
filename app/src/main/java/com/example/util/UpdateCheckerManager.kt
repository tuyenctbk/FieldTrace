package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager responsible for checking version updates and providing notice states.
 */
object UpdateCheckerManager {

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable

    private val _latestVersionName = MutableStateFlow("1.2.0")
    val latestVersionName: StateFlow<String> = _latestVersionName

    /**
     * Checks if an app update is available comparing local versionCode against latest version build.
     */
    fun checkForUpdates(context: Context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }

            // Simulated version check: version code 4 is current (v1.1.0).
            // If latest release is version code 5 (v1.2.0), trigger update prompt banner.
            val latestServerVersionCode = 5L
            if (latestServerVersionCode > currentVersionCode) {
                _latestVersionName.value = "1.2.0"
                _isUpdateAvailable.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissUpdateNotice() {
        _isUpdateAvailable.value = false
    }
}
