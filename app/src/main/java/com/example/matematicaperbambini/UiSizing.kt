package com.example.matematicaperbambini

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class UiSizing(
    val isCompact: Boolean,
    val spacing: Dp,
    val pad: Dp,
    val cell: Dp,
    val cellSmall: Dp,
    val font: Int,
    val title: Int
)

@Composable
@ReadOnlyComposable
fun rememberUiSizing(): UiSizing {
    val windowType = LocalWindowType.current
    val uiScale = LocalUiScale.current
    return remember(windowType, uiScale) {
        UiSizing(
            isCompact = windowType == WindowType.Compact,
            spacing = uiScale.spacing,
            pad = (uiScale.spacing + 2.dp),
            cell = uiScale.digitSize,
            cellSmall = uiScale.digitSmallSize,
            font = uiScale.bodyFont.value.toInt(),
            title = uiScale.titleFont.value.toInt()
        )
    }
}
