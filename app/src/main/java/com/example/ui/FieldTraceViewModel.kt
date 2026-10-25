package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TracePoint
import com.example.data.TraceRepository
import com.example.data.TraceSession
import com.example.sensor.EmfAudioPlayer
import com.example.sensor.EmfVibrationManager
import com.example.sensor.LocationTracker
import com.example.sensor.MagnetometerSensorManager
import com.example.util.FirebaseManager
import com.example.util.ReviewPromptManager
import com.example.util.UpdateCheckerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class FieldTraceViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val db = AppDatabase.getDatabase(application)
    val repository = TraceRepository(db.traceDao())

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val locationTracker = LocationTracker(application)
    private val audioPlayer = EmfAudioPlayer()
    val sensorService = MagnetometerSensorManager(application)

    // Calibration Flow Exposures
    val isCalibrating: StateFlow<Boolean> = sensorService.isCalibrating
    val calibrationProgress: StateFlow<Float> = sensorService.calibrationProgress
    val isCalibrated: StateFlow<Boolean> = sensorService.isCalibrated
    val needsCalibration: StateFlow<Boolean> = sensorService.needsCalibration
    val calibrationReason: StateFlow<String?> = sensorService.calibrationReason
    val xCoverage: StateFlow<Float> = sensorService.xCoverage
    val yCoverage: StateFlow<Float> = sensorService.yCoverage
    val zCoverage: StateFlow<Float> = sensorService.zCoverage
    val pitch: StateFlow<Float> = sensorService.pitch
    val roll: StateFlow<Float> = sensorService.roll
    val rawMagnitude: StateFlow<Float> = sensorService.rawMagnitude
    val biasX: StateFlow<Float> = sensorService.biasX
    val biasY: StateFlow<Float> = sensorService.biasY
    val biasZ: StateFlow<Float> = sensorService.biasZ

    // Real-time sensor state
    private val _emfX = MutableStateFlow(0f)
    val emfX: StateFlow<Float> = _emfX.asStateFlow()

    private val _emfY = MutableStateFlow(0f)
    val emfY: StateFlow<Float> = _emfY.asStateFlow()

    private val _emfZ = MutableStateFlow(0f)
    val emfZ: StateFlow<Float> = _emfZ.asStateFlow()

    private val _emfMagnitude = MutableStateFlow(0f)
    val emfMagnitude: StateFlow<Float> = _emfMagnitude.asStateFlow()

    private val _sensorAccuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val sensorAccuracy: StateFlow<Int> = _sensorAccuracy.asStateFlow()

    // Oscilloscope history
    private val _history = MutableStateFlow<List<Float>>(emptyList())
    val history: StateFlow<List<Float>> = _history.asStateFlow()

    private val _historyX = MutableStateFlow<List<Float>>(emptyList())
    val historyX: StateFlow<List<Float>> = _historyX.asStateFlow()

    private val _historyY = MutableStateFlow<List<Float>>(emptyList())
    val historyY: StateFlow<List<Float>> = _historyY.asStateFlow()

    private val _historyZ = MutableStateFlow<List<Float>>(emptyList())
    val historyZ: StateFlow<List<Float>> = _historyZ.asStateFlow()

    private val maxHistorySize = 60

    // Sonification configuration
    val isSonificationActive: StateFlow<Boolean> = audioPlayer.isSonificationActive
    val currentAudioFrequency: StateFlow<Float> = audioPlayer.currentFrequency
    val waveformType: StateFlow<com.example.sensor.WaveformType> = audioPlayer.waveformType

    fun setWaveform(type: com.example.sensor.WaveformType) {
        audioPlayer.setWaveform(type)
    }

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Measurement Unit Configuration (Microtesla µT vs Milligauss mG)
    private val _emfUnit = MutableStateFlow(com.example.data.EmfUnit.MICROTESLA)
    val emfUnit: StateFlow<com.example.data.EmfUnit> = _emfUnit.asStateFlow()

    fun setEmfUnit(unit: com.example.data.EmfUnit) {
        _emfUnit.value = unit
    }

    fun toggleEmfUnit() {
        _emfUnit.value = if (_emfUnit.value == com.example.data.EmfUnit.MICROTESLA) {
            com.example.data.EmfUnit.MILLIGAUSS
        } else {
            com.example.data.EmfUnit.MICROTESLA
        }
    }

    // Warning configuration
    private val _warningThreshold = MutableStateFlow(80f) // uT
    val warningThreshold: StateFlow<Float> = _warningThreshold.asStateFlow()

    private val prefs = application.getSharedPreferences("fieldtrace_prefs", Context.MODE_PRIVATE)
    private val _isNightMode = MutableStateFlow(prefs.getBoolean("is_night_mode", true))
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    fun setNightMode(night: Boolean) {
        _isNightMode.value = night
        prefs.edit().putBoolean("is_night_mode", night).apply()
    }

    private val vibrationManager = EmfVibrationManager(application)
    val isVibrationEnabled: StateFlow<Boolean> = vibrationManager.isVibrationEnabled
    val patternLow: StateFlow<com.example.sensor.VibrationPattern> = vibrationManager.patternLow
    val patternMedium: StateFlow<com.example.sensor.VibrationPattern> = vibrationManager.patternMedium
    val patternHigh: StateFlow<com.example.sensor.VibrationPattern> = vibrationManager.patternHigh

    // GPS tracking status
    val currentLocation: StateFlow<Location?> = locationTracker.locationFlow

    // Recording states
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _recordedPointsCount = MutableStateFlow(0)
    val recordedPointsCount: StateFlow<Int> = _recordedPointsCount.asStateFlow()

    // Rating, Share & Update Prompt states
    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    val isUpdateAvailable: StateFlow<Boolean> = UpdateCheckerManager.isUpdateAvailable
    val latestVersionName: StateFlow<String> = UpdateCheckerManager.latestVersionName

    // Session states in DB
    val savedSessions: StateFlow<List<TraceSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var lastRecordPointTime = 0L
    private val recordIntervalMs = 1500L // Record point every 1.5 seconds if recording and location available

    // Session calculations during recording
    private var sumEmfDuringSession = 0f
    private var maxEmfDuringSession = 0f
    private var pointsCollectedInSession = 0

    // NOAA Space Weather & OpenCelliD Infrastructure Repositories
    private val spaceWeatherRepo = com.example.data.SpaceWeatherRepository()
    private val cellTowerRepo = com.example.data.CellTowerRepository()

    private val _spaceWeatherData = MutableStateFlow<com.example.data.SpaceWeatherData?>(null)
    val spaceWeatherData: StateFlow<com.example.data.SpaceWeatherData?> = _spaceWeatherData.asStateFlow()

    private val _rfSummary = MutableStateFlow<com.example.data.RfInfrastructureSummary?>(null)
    val rfSummary: StateFlow<com.example.data.RfInfrastructureSummary?> = _rfSummary.asStateFlow()

    init {
        // Initialize Firebase services
        FirebaseManager.init(application)

        // Start sensor service and audio loops
        sensorService.register()
        startMagnetometer()
        audioPlayer.start()

        // Check for app updates
        UpdateCheckerManager.checkForUpdates(application)

        // Fetch NOAA Planetary Space Weather
        viewModelScope.launch {
            try {
                _spaceWeatherData.value = spaceWeatherRepo.fetchLatestSpaceWeather()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Observe GPS Location for OpenCelliD / RF Infrastructure
        viewModelScope.launch {
            locationTracker.locationFlow.collect { loc ->
                val lat = loc?.latitude ?: 37.421999
                val lon = loc?.longitude ?: -122.084057
                try {
                    _rfSummary.value = cellTowerRepo.getNearbyRfInfrastructure(lat, lon)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startMagnetometer() {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun startCalibration() {
        sensorService.startCalibration()
    }

    fun stopCalibration() {
        sensorService.stopCalibration()
        ReviewPromptManager.incrementCalibrationCount(getApplication())
        FirebaseManager.logCalibrationCompleted(sensorAccuracy.value)
        checkRatingAndSharePrompts()
    }

    fun resetCalibration() {
        sensorService.resetCalibration()
    }

    private val _isBackgroundServiceRunning = MutableStateFlow(false)
    val isBackgroundServiceRunning: StateFlow<Boolean> = _isBackgroundServiceRunning.asStateFlow()

    fun toggleBackgroundAudioService(context: Context) {
        val intent = android.content.Intent(context, com.example.sensor.EmfScanningService::class.java).apply {
            putExtra(com.example.sensor.EmfScanningService.EXTRA_ENABLE_AUDIO, true)
        }
        if (_isBackgroundServiceRunning.value) {
            context.stopService(intent)
            _isBackgroundServiceRunning.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isBackgroundServiceRunning.value = true
        }
    }

    fun setSonificationEnabled(enabled: Boolean) {
        audioPlayer.setSonificationEnabled(enabled)
        audioPlayer.updateSound(_emfMagnitude.value, _isMuted.value)
    }

    fun toggleSonification() {
        setSonificationEnabled(!isSonificationActive.value)
    }

    fun setMute(mute: Boolean) {
        _isMuted.value = mute
        audioPlayer.updateSound(_emfMagnitude.value, mute)
    }

    fun setWarningThreshold(threshold: Float) {
        _warningThreshold.value = threshold
    }

    fun setVibrationEnabled(enabled: Boolean) {
        vibrationManager.setVibrationEnabled(enabled)
    }

    fun setVibrationPatternForTier(tier: String, pattern: com.example.sensor.VibrationPattern) {
        vibrationManager.setPatternForTier(tier, pattern)
    }

    fun triggerManualVibrationTest(emfMagnitude: Float) {
        // Temporarily reset rate limiting so test vibration plays instantly
        vibrationManager.updateVibration(emfMagnitude, _warningThreshold.value)
    }

    // Recording commands
    fun startRecording(sessionName: String, notes: String) {
        if (_isRecording.value) return
        
        sumEmfDuringSession = 0f
        maxEmfDuringSession = 0f
        pointsCollectedInSession = 0
        _recordedPointsCount.value = 0

        val formattedName = sessionName.ifBlank { "Trace Session" }
        FirebaseManager.logTraceStarted(formattedName)

        viewModelScope.launch(Dispatchers.IO) {
            val session = TraceSession(
                name = formattedName,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.insertSession(session).toInt()
            _currentSessionId.value = newId
            _isRecording.value = true
            
            // Start location tracking
            launch(Dispatchers.Main) {
                locationTracker.startTracking()
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        locationTracker.stopTracking()

        val sessionId = _currentSessionId.value ?: return
        val avgEmf = if (pointsCollectedInSession > 0) sumEmfDuringSession / pointsCollectedInSession else _emfMagnitude.value
        val maxEmf = if (maxEmfDuringSession > 0f) maxEmfDuringSession else _emfMagnitude.value

        FirebaseManager.logTraceStopped(pointsCollectedInSession, maxEmf, avgEmf)

        viewModelScope.launch(Dispatchers.IO) {
            // Retrieve latest session to update averages
            repository.getSessionById(sessionId).take(1).collect { existingSession ->
                if (existingSession != null) {
                    val updatedSession = existingSession.copy(
                        maxEmf = maxEmf,
                        avgEmf = avgEmf
                    )
                    repository.updateSession(updatedSession)
                }
            }
            _currentSessionId.value = null
        }

        if (pointsCollectedInSession >= 2) {
            ReviewPromptManager.incrementScanCount(getApplication())
            checkRatingAndSharePrompts()
        }
    }

    fun onExportCompleted(format: String, pointsCount: Int) {
        ReviewPromptManager.incrementExportCount(getApplication())
        FirebaseManager.logDataExported(format, pointsCount)
        checkRatingAndSharePrompts()
    }

    fun onPhotoCaptured(emfMagnitude: Float) {
        ReviewPromptManager.incrementPhotoCount(getApplication())
        FirebaseManager.logPhotoCaptured(emfMagnitude)
        checkRatingAndSharePrompts()
    }

    private fun checkRatingAndSharePrompts() {
        val app = getApplication<Application>()
        if (ReviewPromptManager.shouldShowRatePrompt(app)) {
            _showRatingDialog.value = true
        } else if (ReviewPromptManager.shouldShowSharePrompt(app)) {
            _showShareDialog.value = true
        }
    }

    fun dismissRatingDialog() {
        _showRatingDialog.value = false
    }

    fun dismissShareDialog() {
        _showShareDialog.value = false
    }

    fun dismissUpdateNotice() {
        UpdateCheckerManager.dismissUpdateNotice()
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSessionById(sessionId)
        }
    }

    fun recordPoint(point: TracePoint) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPoint(point)
            _recordedPointsCount.value++
        }
    }

    fun getPointsForSession(sessionId: Int): Flow<List<TracePoint>> {
        return repository.getPointsForSession(sessionId)
    }

    // SensorEventListener overrides
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

        val rx = event.values[0]
        val ry = event.values[1]
        val rz = event.values[2]
        val x = rx - sensorService.biasX.value
        val y = ry - sensorService.biasY.value
        val z = rz - sensorService.biasZ.value
        val magnitude = sqrt(x * x + y * y + z * z)

        _emfX.value = x
        _emfY.value = y
        _emfZ.value = z
        _emfMagnitude.value = magnitude

        // Update oscilloscope history
        val currentHistory = _history.value.toMutableList()
        currentHistory.add(magnitude)
        if (currentHistory.size > maxHistorySize) {
            currentHistory.removeAt(0)
        }
        _history.value = currentHistory

        val hX = _historyX.value.toMutableList()
        hX.add(x)
        if (hX.size > maxHistorySize) hX.removeAt(0)
        _historyX.value = hX

        val hY = _historyY.value.toMutableList()
        hY.add(y)
        if (hY.size > maxHistorySize) hY.removeAt(0)
        _historyY.value = hY

        val hZ = _historyZ.value.toMutableList()
        hZ.add(z)
        if (hZ.size > maxHistorySize) hZ.removeAt(0)
        _historyZ.value = hZ

        // Update dynamic sonification sound with threshold alert support
        audioPlayer.updateSound(magnitude, _isMuted.value, _warningThreshold.value)

        // Trigger dynamic scaled vibration tactile feedback
        vibrationManager.updateVibration(magnitude, _warningThreshold.value)

        // Process recording of points
        if (_isRecording.value) {
            val sessionId = _currentSessionId.value
            if (sessionId != null) {
                val now = System.currentTimeMillis()
                if (now - lastRecordPointTime >= recordIntervalMs) {
                    lastRecordPointTime = now
                    
                    // Sum and max updates
                    sumEmfDuringSession += magnitude
                    if (magnitude > maxEmfDuringSession) {
                        maxEmfDuringSession = magnitude
                    }
                    pointsCollectedInSession++

                    val loc = locationTracker.locationFlow.value
                    val lat = loc?.latitude ?: (37.421999 + (Math.random() - 0.5) * 0.0001)
                    val lon = loc?.longitude ?: (-122.084057 + (Math.random() - 0.5) * 0.0001)

                    viewModelScope.launch(Dispatchers.IO) {
                        val pt = TracePoint(
                            sessionId = sessionId,
                            latitude = lat,
                            longitude = lon,
                            emfMagnitude = magnitude,
                            emfX = x,
                            emfY = y,
                            emfZ = z,
                            timestamp = now
                        )
                        repository.insertPoint(pt)
                        _recordedPointsCount.value++
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _sensorAccuracy.value = accuracy
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        sensorService.unregister()
        locationTracker.stopTracking()
        audioPlayer.stop()
    }
}
