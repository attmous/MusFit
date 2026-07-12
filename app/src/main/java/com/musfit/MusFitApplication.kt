package com.musfit

import android.app.Application
import android.util.Log
import com.musfit.data.transfer.PendingDataImport
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusFitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { PendingDataImport.applyIfPending(this) }
            .onFailure { error ->
                Log.e("MusFitDataTransfer", "Pending data restore failed; previous data retained", error)
                PendingDataImport.recordFailure(this, error)
            }
    }
}
