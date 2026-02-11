package com.example.matematicaperbambini

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val HeaderTopOffset = 10.dp

fun Modifier.headerOffsetFromStatusBar(includeStatusBarPadding: Boolean = true): Modifier {
    return if (includeStatusBarPadding) {
        this.statusBarsPadding().padding(top = HeaderTopOffset)
    } else {
        this.padding(top = HeaderTopOffset)
    }
}
