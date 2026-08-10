package com.example.util

import com.example.data.TracePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TraceAnalyticsCalculatorTest {

    @Test
    fun testHaversineDistance() {
        // Known coordinates (~111 km per degree of latitude)
        val p1 = TracePoint(sessionId = 1, latitude = 37.7749, longitude = -122.4194, emfMagnitude = 40f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 1000L)
        val p2 = TracePoint(sessionId = 1, latitude = 37.7759, longitude = -122.4194, emfMagnitude = 45f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 2000L)

        val distance = TraceAnalyticsCalculator.calculateDistanceMeters(listOf(p1, p2))
        // Expect ~111 meters for 0.001 degree latitude delta
        assertEquals(111.0, distance, 5.0)
    }

    @Test
    fun testDurationCalculation() {
        val p1 = TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 30f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 10000L)
        val p2 = TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 50f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 25000L)

        val duration = TraceAnalyticsCalculator.calculateDurationMs(listOf(p1, p2))
        assertEquals(15000L, duration)
    }

    @Test
    fun testAnomalyPercentageAndPeak() {
        val points = listOf(
            TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 30f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 1000L),
            TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 85f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 2000L), // Exceeds 80uT
            TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 40f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 3000L),
            TracePoint(sessionId = 1, latitude = 37.0, longitude = -122.0, emfMagnitude = 90f, emfX = 0f, emfY = 0f, emfZ = 0f, timestamp = 4000L)  // Exceeds 80uT (Peak)
        )

        val pct = TraceAnalyticsCalculator.calculateAnomalyPercentage(points, 80f)
        assertEquals(50.0f, pct, 0.1f)

        val peak = TraceAnalyticsCalculator.getPeakPoint(points)
        assertNotNull(peak)
        assertEquals(90f, peak!!.emfMagnitude, 0.01f)

        val risk = TraceAnalyticsCalculator.getRiskRating(peak.emfMagnitude, pct)
        assertEquals("CRITICAL", risk)
    }
}
