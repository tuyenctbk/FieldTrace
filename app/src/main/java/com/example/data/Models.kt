package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trace_sessions")
data class TraceSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val maxEmf: Float = 0f,
    val avgEmf: Float = 0f
)

@Entity(tableName = "trace_points")
data class TracePoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val latitude: Double,
    val longitude: Double,
    val emfMagnitude: Float,
    val emfX: Float,
    val emfY: Float,
    val emfZ: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String? = null
)
