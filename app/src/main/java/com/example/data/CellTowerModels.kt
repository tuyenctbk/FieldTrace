package com.example.data

data class CellTowerInfo(
    val id: String,
    val radioType: String,      // e.g. "5G NR", "LTE", "UMTS", "GSM"
    val carrierName: String,     // e.g. "Verizon Wireless", "T-Mobile 5G", "AT&T LTE", "Vodafone"
    val frequencyBand: String,   // e.g. "n78 (3.5 GHz)", "B3 (1800 MHz)", "B13 (700 MHz)"
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val signalStrengthDbm: Int,  // e.g. -65 dBm
    val rfPowerDensityMwM2: Float, // e.g. 1.25 mW/m²
    val estimatedEmfContributionuT: Float // e.g. 0.12 µT
)

data class RfInfrastructureSummary(
    val nearbyTowers: List<CellTowerInfo>,
    val nearestTowerDistanceM: Int,
    val totalRfPowerDensityMwM2: Float,
    val rfDensityStatus: String, // "LOW RF EXPOSURE", "MODERATE CELL DENSITY", "HIGH RF TOWER PROXIMITY"
    val isLiveOpenCellData: Boolean
)
