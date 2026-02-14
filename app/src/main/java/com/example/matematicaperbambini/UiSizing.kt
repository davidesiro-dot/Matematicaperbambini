package com.example.matematicaperbambini

import androidx.compose.runtime.Composable
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
fun rememberUiSizing(): UiSizing {
    val windowType = calculateWindowType()
    val scale = rememberUiScale(windowType)
    return UiSizing(
        isCompact = windowType == WindowType.Compact,
        spacing = scale.spacing,
        pad = (scale.spacing + 4.dp),
        cell = scale.digitSize,
        cellSmall = scale.digitSmallSize,
        font = scale.bodyFont.value.toInt(),
        title = scale.titleFont.value.toInt()
    )
}
