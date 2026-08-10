package com.example.util

import com.example.data.TracePoint
import com.example.data.TraceRepository
import com.example.data.TraceSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Service responsible for bidirectional Room-to-Firestore synchronization
 * of EMF scan history sessions and trace points.
 */
object FirestoreSyncService {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncedSessionsCount = MutableStateFlow(0)
    val syncedSessionsCount: StateFlow<Int> = _syncedSessionsCount.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Ready for cloud sync")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    /**
     * Uploads a single session and its recorded points to Firestore.
     */
    fun syncSessionToFirestore(session: TraceSession, points: List<TracePoint>) {
        val userId = FirebaseAuthManager.userId.value ?: return
        val db = FirebaseFirestore.getInstance()

        val sessionRef = db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(session.id.toString())

        val sessionData = hashMapOf(
            "id" to session.id,
            "name" to session.name,
            "timestamp" to session.timestamp,
            "maxEmf" to session.maxEmf,
            "avgEmf" to session.avgEmf,
            "notes" to session.notes,
            "pointsCount" to points.size,
            "syncedAt" to System.currentTimeMillis()
        )

        sessionRef.set(sessionData, SetOptions.merge())
            .addOnSuccessListener {
                // Batch upload points
                if (points.isNotEmpty()) {
                    val pointsCollection = sessionRef.collection("points")
                    val batch = db.batch()
                    points.take(500).forEach { pt ->
                        val ptRef = pointsCollection.document(pt.id.toString())
                        val ptData = hashMapOf(
                            "id" to pt.id,
                            "sessionId" to pt.sessionId,
                            "timestamp" to pt.timestamp,
                            "emfMagnitude" to pt.emfMagnitude,
                            "emfX" to pt.emfX,
                            "emfY" to pt.emfY,
                            "emfZ" to pt.emfZ,
                            "latitude" to pt.latitude,
                            "longitude" to pt.longitude,
                            "photoPath" to pt.photoPath
                        )
                        batch.set(ptRef, ptData, SetOptions.merge())
                    }
                    batch.commit()
                }
            }
    }

    /**
     * Performs a full sync between Room and Firestore.
     */
    fun performFullSync(repository: TraceRepository) {
        val userId = FirebaseAuthManager.userId.value ?: run {
            _syncStatusText.value = "Cloud Sync Error: User not authenticated"
            return
        }

        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncStatusText.value = "Synchronizing local vault with Firestore..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val localSessions = repository.allSessions.first()

                var synced = 0
                for (session in localSessions) {
                    val points = repository.getPointsForSession(session.id).first()
                    syncSessionToFirestore(session, points)
                    synced++
                }

                _syncedSessionsCount.value = synced
                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatusText.value = "Synced $synced scan sessions to cloud"
            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatusText.value = "Sync Error: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
