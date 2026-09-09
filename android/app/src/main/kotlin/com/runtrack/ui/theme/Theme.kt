package com.runtrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun RunTrackTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // TODO: 컬러 스킴 정의 및 테마 구성
    MaterialTheme(
        content = content
    )
}
