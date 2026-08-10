package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceDao {
    @Query("SELECT * FROM trace_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<TraceSession>>

    @Query("SELECT * FROM trace_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: Int): Flow<TraceSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TraceSession): Long

    @Update
    suspend fun updateSession(session: TraceSession)

    @Delete
    suspend fun deleteSession(session: TraceSession)

    @Query("DELETE FROM trace_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("SELECT * FROM trace_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(sessionId: Int): Flow<List<TracePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TracePoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TracePoint>)

    @Query("DELETE FROM trace_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Int)
}
