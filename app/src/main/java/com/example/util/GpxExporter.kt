package com.example.util

import com.example.data.TracePoint
import com.example.data.TraceSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {
    fun exportToGpx(session: TraceSession, points: List<TracePoint>): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sessionTime = isoFormat.format(Date(session.timestamp))

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"FieldTrace\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(session.name)).append("</name>\n")
        sb.append("    <desc>").append(escapeXml(session.notes)).append("</desc>\n")
        sb.append("    <time>").append(sessionTime).append("</time>\n")
        sb.append("  </metadata>\n")
        
        if (points.isNotEmpty()) {
            sb.append("  <trk>\n")
            sb.append("    <name>EMF Magnetic Track</name>\n")
            sb.append("    <desc>Max EMF: ").append(String.format(Locale.US, "%.1f", session.maxEmf))
              .append(" uT, Avg EMF: ").append(String.format(Locale.US, "%.1f", session.avgEmf)).append(" uT</desc>\n")
            sb.append("    <trkseg>\n")
            
            for (pt in points) {
                val ptTime = isoFormat.format(Date(pt.timestamp))
                sb.append(String.format(Locale.US, "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", pt.latitude, pt.longitude))
                sb.append("        <time>").append(ptTime).append("</time>\n")
                sb.append(String.format(Locale.US, "        <desc>EMF: %.1f uT (X:%.1f, Y:%.1f, Z:%.1f)</desc>\n", 
                    pt.emfMagnitude, pt.emfX, pt.emfY, pt.emfZ))
                sb.append("        <extensions>\n")
                sb.append(String.format(Locale.US, "          <emf_magnitude>%.2f</emf_magnitude>\n", pt.emfMagnitude))
                sb.append(String.format(Locale.US, "          <emf_x>%.2f</emf_x>\n", pt.emfX))
                sb.append(String.format(Locale.US, "          <emf_y>%.2f</emf_y>\n", pt.emfY))
                sb.append(String.format(Locale.US, "          <emf_z>%.2f</emf_z>\n", pt.emfZ))
                sb.append("        </extensions>\n")
                sb.append("      </trkpt>\n")
            }
            
            sb.append("    </trkseg>\n")
            sb.append("  </trk>\n")
        }
        
        sb.append("</gpx>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
