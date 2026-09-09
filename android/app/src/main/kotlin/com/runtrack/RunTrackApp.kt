package com.runtrack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RunTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: 앱 초기화 로직
    }
}
