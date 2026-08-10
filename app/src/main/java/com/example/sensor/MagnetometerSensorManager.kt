package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * SensorManager service responsible for accessing device magnetometer (and accelerometer) sensors,
 * normalizing raw readings, tracking sensor accuracy, and providing figure-eight calibration routines.
 */
class MagnetometerSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Raw Sensor Streams
    private val _rawX = MutableStateFlow(0f)
    val rawX: StateFlow<Float> = _rawX.asStateFlow()

    private val _rawY = MutableStateFlow(0f)
    val rawY: StateFlow<Float> = _rawY.asStateFlow()

    private val _rawZ = MutableStateFlow(0f)
    val rawZ: StateFlow<Float> = _rawZ.asStateFlow()

    private val _rawMagnitude = MutableStateFlow(0f)
    val rawMagnitude: StateFlow<Float> = _rawMagnitude.asStateFlow()

    // Normalized Calibrated Sensor Streams
    private val _normX = MutableStateFlow(0f)
    val normX: StateFlow<Float> = _normX.asStateFlow()

    private val _normY = MutableStateFlow(0f)
    val normY: StateFlow<Float> = _normY.asStateFlow()

    private val _normZ = MutableStateFlow(0f)
    val normZ: StateFlow<Float> = _normZ.asStateFlow()

    private val _normMagnitude = MutableStateFlow(0f)
    val normMagnitude: StateFlow<Float> = _normMagnitude.asStateFlow()

    // Sensor Hardware Accuracy
    private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val accuracy: StateFlow<Int> = _accuracy.asStateFlow()

    // Orientation / Motion tracking for interactive calibration figure-8 visualizer
    private val _pitch = MutableStateFlow(0f) // degrees
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f) // degrees
    val roll: StateFlow<Float> = _roll.asStateFlow()

    // Calibration Mode & Stats
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f) // 0.0 to 1.0 (0% to 100%)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private val _xCoverage = MutableStateFlow(0f)
    val xCoverage: StateFlow<Float> = _xCoverage.asStateFlow()

    private val _yCoverage = MutableStateFlow(0f)
    val yCoverage: StateFlow<Float> = _yCoverage.asStateFlow()

    private val _zCoverage = MutableStateFlow(0f)
    val zCoverage: StateFlow<Float> = _zCoverage.asStateFlow()

    private val _biasX = MutableStateFlow(0f)
    val biasX: StateFlow<Float> = _biasX.asStateFlow()

    private val _biasY = MutableStateFlow(0f)
    val biasY: StateFlow<Float> = _biasY.asStateFlow()

    private val _biasZ = MutableStateFlow(0f)
    val biasZ: StateFlow<Float> = _biasZ.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()

    // Calibration bounds during figure-8 motion
    private var minXVal = Float.MAX_VALUE
    private var maxXVal = -Float.MAX_VALUE
    private var minYVal = Float.MAX_VALUE
    private var maxYVal = -Float.MAX_VALUE
    private var minZVal = Float.MAX_VALUE
    private var maxZVal = -Float.MAX_VALUE

    // Target sweep span per axis in uT to reach 100% calibration
    private val targetSpanX = 25f
    private val targetSpanY = 25f
    private val targetSpanZ = 20f

    fun register() {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    fun startCalibration() {
        minXVal = Float.MAX_VALUE
        maxXVal = -Float.MAX_VALUE
        minYVal = Float.MAX_VALUE
        maxYVal = -Float.MAX_VALUE
        minZVal = Float.MAX_VALUE
        maxZVal = -Float.MAX_VALUE

        _calibrationProgress.value = 0f
        _xCoverage.value = 0f
        _yCoverage.value = 0f
        _zCoverage.value = 0f
        _isCalibrating.value = true
    }

    fun stopCalibration() {
        _isCalibrating.value = false
        if (_calibrationProgress.value >= 0.3f) {
            // Apply hard-iron offset bias calculations
            if (minXVal != Float.MAX_VALUE && maxXVal != -Float.MAX_VALUE) {
                _biasX.value = (minXVal + maxXVal) / 2f
            }
            if (minYVal != Float.MAX_VALUE && maxYVal != -Float.MAX_VALUE) {
                _biasY.value = (minYVal + maxYVal) / 2f
            }
            if (minZVal != Float.MAX_VALUE && maxZVal != -Float.MAX_VALUE) {
                _biasZ.value = (minZVal + maxZVal) / 2f
            }
            _isCalibrated.value = true
            _needsCalibration.value = false
        }
    }

    fun resetCalibration() {
        _biasX.value = 0f
        _biasY.value = 0f
        _biasZ.value = 0f
        _isCalibrated.value = false
        _calibrationProgress.value = 0f
        _xCoverage.value = 0f
        _yCoverage.value = 0f
        _zCoverage.value = 0f
        _isCalibrating.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val rx = event.values[0]
                val ry = event.values[1]
                val rz = event.values[2]
                val rMag = sqrt(rx * rx + ry * ry + rz * rz)

                _rawX.value = rx
                _rawY.value = ry
                _rawZ.value = rz
                _rawMagnitude.value = rMag

                // Compute normalized readings applying calibration offsets
                val nx = rx - _biasX.value
                val ny = ry - _biasY.value
                val nz = rz - _biasZ.value
                val nMag = sqrt(nx * nx + ny * ny + nz * nz)

                _normX.value = nx
                _normY.value = ny
                _normZ.value = nz
                _normMagnitude.value = nMag

                // Process calibration progress if currently in figure-8 routine
                if (_isCalibrating.value) {
                    minXVal = min(minXVal, rx)
                    maxXVal = max(maxXVal, rx)
                    minYVal = min(minYVal, ry)
                    maxYVal = max(maxYVal, ry)
                    minZVal = min(minZVal, rz)
                    maxZVal = max(maxZVal, rz)

                    val spanX = max(0f, maxXVal - minXVal)
                    val spanY = max(0f, maxYVal - minYVal)
                    val spanZ = max(0f, maxZVal - minZVal)

                    val cX = (spanX / targetSpanX).coerceIn(0f, 1f)
                    val cY = (spanY / targetSpanY).coerceIn(0f, 1f)
                    val cZ = (spanZ / targetSpanZ).coerceIn(0f, 1f)

                    _xCoverage.value = cX
                    _yCoverage.value = cY
                    _zCoverage.value = cZ

                    val totalProgress = (cX * 0.4f + cY * 0.4f + cZ * 0.2f).coerceIn(0f, 1f)
                    _calibrationProgress.value = totalProgress

                    if (totalProgress >= 1f && _isCalibrating.value) {
                        stopCalibration()
                    }
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // Simple Pitch and Roll calculations in degrees
                val pitchRad = atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))
                val rollRad = atan2(-ax.toDouble(), az.toDouble())

                _pitch.value = Math.toDegrees(pitchRad).toFloat()
                _roll.value = Math.toDegrees(rollRad).toFloat()
            }
        }
    }

    // Smart Calibration Notification Flows
    private val _needsCalibration = MutableStateFlow(false)
    val needsCalibration: StateFlow<Boolean> = _needsCalibration.asStateFlow()

    private val _calibrationReason = MutableStateFlow<String?>(null)
    val calibrationReason: StateFlow<String?> = _calibrationReason.asStateFlow()

    fun dismissCalibrationReminder() {
        _needsCalibration.value = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            _accuracy.value = accuracy
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                _needsCalibration.value = true
                _calibrationReason.value = "Sensor accuracy is low. Recalibrate baseline."
            }
        }
    }
}
