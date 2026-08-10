package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.TraceRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class FirestoreSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting WorkManager auto-sync to Firestore...")

        val userId = FirebaseAuthManager.userId.value
        return if (userId == null) {
            Log.w(TAG, "No authenticated user. Skipping auto-sync.")
            Result.success()
        } else {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = TraceRepository(database.traceDao())

                val localSessions = repository.allSessions.first()
                Log.d(TAG, "Found ${localSessions.size} local sessions to sync.")

                for (session in localSessions) {
                    val points = repository.getPointsForSession(session.id).first()
                    FirestoreSyncService.syncSessionToFirestore(session, points)
                }

                Log.d(TAG, "Auto-sync Work completed successfully.")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error in auto-sync worker: ${e.message}", e)
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "FirestoreSyncWorker"
        private const val UNIQUE_WORK_NAME = "FirestoreSessionAutoSyncWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Run every 1 hour to sync data automatically
            val syncRequest = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Enqueued Firestore sync worker (Periodic, 1h, Connected network constraint).")
        }
    }
}
