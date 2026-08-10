package com.example.util

import com.example.data.TracePoint
import com.example.data.TraceSession
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporter utility for converting session data and sensor points into CSV and JSON formats.
 */
object CsvJsonExporter {

    /**
     * Exports trace points to standard CSV format.
     */
    fun exportToCsv(session: TraceSession, points: List<TracePoint>): String {
        val sb = java.lang.StringBuilder()
        sb.append("Timestamp_ISO,Timestamp_MS,Latitude,Longitude,EMF_Magnitude_uT,EMF_X_uT,EMF_Y_uT,EMF_Z_uT\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

        for (pt in points) {
            val dateStr = sdf.format(Date(pt.timestamp))
            sb.append(
                String.format(
                    Locale.US,
                    "%s,%d,%.7f,%.7f,%.2f,%.2f,%.2f,%.2f\n",
                    dateStr,
                    pt.timestamp,
                    pt.latitude,
                    pt.longitude,
                    pt.emfMagnitude,
                    pt.emfX,
                    pt.emfY,
                    pt.emfZ
                )
            )
        }
        return sb.toString()
    }

    /**
     * Exports session and trace points to structured JSON format.
     */
    fun exportToJson(session: TraceSession, points: List<TracePoint>): String {
        val root = JSONObject()
        val meta = JSONObject().apply {
            put("sessionId", session.id)
            put("sessionName", session.name)
            put("notes", session.notes)
            put("timestamp", session.timestamp)
            put("maxEmf", session.maxEmf)
            put("avgEmf", session.avgEmf)
            put("totalPoints", points.size)
        }
        root.put("metadata", meta)

        val pointsArray = JSONArray()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        for (pt in points) {
            val ptObj = JSONObject().apply {
                put("timestamp", pt.timestamp)
                put("isoTime", sdf.format(Date(pt.timestamp)))
                put("latitude", pt.latitude)
                put("longitude", pt.longitude)
                put("emfMagnitude", pt.emfMagnitude)
                put("emfX", pt.emfX)
                put("emfY", pt.emfY)
                put("emfZ", pt.emfZ)
            }
            pointsArray.put(ptObj)
        }
        root.put("points", pointsArray)

        return root.toString(2)
    }
}
