package com.example.data

import kotlinx.coroutines.flow.Flow

class TraceRepository(private val traceDao: TraceDao) {
    val allSessions: Flow<List<TraceSession>> = traceDao.getAllSessions()

    fun getSessionById(id: Int): Flow<TraceSession?> = traceDao.getSessionById(id)

    fun getPointsForSession(sessionId: Int): Flow<List<TracePoint>> = traceDao.getPointsForSession(sessionId)

    suspend fun insertSession(session: TraceSession): Long = traceDao.insertSession(session)

    suspend fun updateSession(session: TraceSession) = traceDao.updateSession(session)

    suspend fun deleteSessionById(id: Int) {
        traceDao.deletePointsForSession(id)
        traceDao.deleteSessionById(id)
    }

    suspend fun insertPoint(point: TracePoint) = traceDao.insertPoint(point)

    suspend fun insertPoints(points: List<TracePoint>) = traceDao.insertPoints(points)
}
