package com.example.luxhotel

import android.app.Application
import androidx.work.*
import com.example.luxhotel.network.SyncWorker

class LuxHotelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        enqueueSyncWork()
    }

    private fun enqueueSyncWork() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "luxhotel_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
