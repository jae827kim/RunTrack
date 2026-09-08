package com.runtrack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RunTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Timber 로깅 초기화
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("RunTrackApp initialized")
    }
}
