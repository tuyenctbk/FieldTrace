package com.example.util

import com.example.data.TracePoint
import com.example.data.TraceSession
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvJsonExporterTest {

    @Test
    fun testCsvExportFormat() {
        val session = TraceSession(id = 1, name = "Test Session", notes = "Test Note", maxEmf = 85f, avgEmf = 45f, timestamp = 100000L)
        val points = listOf(
            TracePoint(sessionId = 1, latitude = 37.7749, longitude = -122.4194, emfMagnitude = 50f, emfX = 10f, emfY = 20f, emfZ = 30f, timestamp = 100000L),
            TracePoint(sessionId = 1, latitude = 37.7750, longitude = -122.4195, emfMagnitude = 85f, emfX = 15f, emfY = 25f, emfZ = 35f, timestamp = 101500L)
        )

        val csv = CsvJsonExporter.exportToCsv(session, points)
        assertTrue(csv.contains("Timestamp_ISO,Timestamp_MS,Latitude,Longitude,EMF_Magnitude_uT"))
        assertTrue(csv.contains("37.7749"))
        assertTrue(csv.contains("85.00"))
    }

    @Test
    fun testJsonExportFormat() {
        val session = TraceSession(id = 1, name = "JSON Test Session", notes = "JSON Note", maxEmf = 90f, avgEmf = 40f, timestamp = 200000L)
        val points = listOf(
            TracePoint(sessionId = 1, latitude = 37.7749, longitude = -122.4194, emfMagnitude = 90f, emfX = 10f, emfY = 20f, emfZ = 30f, timestamp = 200000L)
        )

        val json = CsvJsonExporter.exportToJson(session, points)
        assertTrue(json.contains("\"sessionName\": \"JSON Test Session\""))
        assertTrue(json.contains("\"emfMagnitude\": 90"))
    }
}
