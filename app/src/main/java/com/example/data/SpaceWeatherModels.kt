package com.example.data

data class SpaceWeatherData(
    val kpIndex: Float,          // 0.0 to 9.0 (e.g. 2.33)
    val stormCategory: String,  // e.g. "G0 - Quiet", "G1 - Minor Storm", "G3 - Strong Storm"
    val solarWindSpeedKmS: Int,  // e.g. 420 km/s
    val solarFlareClass: String, // e.g. "C1.2", "M2.4", "X1.0"
    val bzTiltedNt: Float,       // Interplanetary Magnetic Field Bz component in nanoTesla (e.g. -3.5 nT)
    val planetaryEmfImpact: String, // e.g. "Low Background Shift (+0.05 µT)"
    val lastUpdatedText: String,
    val isLiveNoaaData: Boolean
)
