package com.runtrack

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.runtrack.ui.auth.RunTrackEntry
import com.runtrack.ui.theme.RunTrackColors
import com.runtrack.ui.theme.RunTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = RunTrackColors.Background.toArgb()
        window.navigationBarColor = (if (Build.VERSION.SDK_INT >= 26) RunTrackColors.Surface else RunTrackColors.Text).toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { RunTrackTheme { RunTrackEntry() } }
    }
}
