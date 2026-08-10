package com.example.util

import com.example.data.TracePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility calculator for computing spatial path distance, duration,
 * anomaly rates, and risk classification across trace session points.
 */
object TraceAnalyticsCalculator {

    /**
     * Calculates the total path distance traversed across sequential trace points in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(points: List<TracePoint>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            totalDistance += haversineDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }
        return totalDistance
    }

    /**
     * Calculates session duration in milliseconds from the first to the last recorded node.
     */
    fun calculateDurationMs(points: List<TracePoint>): Long {
        if (points.isEmpty()) return 0L
        val start = points.first().timestamp
        val end = points.last().timestamp
        return (end - start).coerceAtLeast(0L)
    }

    /**
     * Calculates the percentage of recorded nodes that exceed the specified warning threshold.
     */
    fun calculateAnomalyPercentage(points: List<TracePoint>, threshold: Float): Float {
        if (points.isEmpty()) return 0f
        val anomalousCount = points.count { it.emfMagnitude >= threshold }
        return (anomalousCount.toFloat() / points.size.toFloat()) * 100f
    }

    /**
     * Retrieves the trace point with the highest EMF magnitude.
     */
    fun getPeakPoint(points: List<TracePoint>): TracePoint? {
        return points.maxByOrNull { it.emfMagnitude }
    }

    /**
     * Determines the risk assessment classification rating string ("SAFE", "ELEVATED", "CRITICAL").
     */
    fun getRiskRating(maxEmf: Float, anomalyPct: Float): String {
        return when {
            maxEmf >= 100f || anomalyPct >= 35f -> "CRITICAL"
            maxEmf >= 60f || anomalyPct >= 15f -> "ELEVATED"
            else -> "SAFE"
        }
    }

    /**
     * Standard Haversine distance algorithm returning distance in meters between two lat/lon coordinates.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }
}
