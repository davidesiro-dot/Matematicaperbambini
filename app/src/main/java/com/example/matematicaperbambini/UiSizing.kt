package com.example.matematicaperbambini

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class UiSizing(
    val windowType: WindowType,
    val spacing: Dp,
    val pad: Dp,
    val cell: Dp,
    val cellSmall: Dp,
    val font: Int,
    val title: Int
) {
    val isCompact: Boolean get() = windowType == WindowType.Compact
    val isExpanded: Boolean get() = windowType == WindowType.Expanded
}

@Composable
fun rememberUiSizing(): UiSizing {
    val windowType = calculateWindowType()
    val scale = rememberUiScale(windowType)

    val safeSpacing = scale.spacing.coerceAtLeast(6.dp)
    val safeCell = scale.digitSize.coerceAtLeast(36.dp)
    val safeSmallCell = scale.digitSmallSize.coerceAtLeast(32.dp)

    val padding = when (windowType) {
        WindowType.Compact -> 10.dp
        WindowType.Medium -> 14.dp
        WindowType.Expanded -> 18.dp
    }

    return UiSizing(
        windowType = windowType,
        spacing = safeSpacing,
        pad = padding,
        cell = safeCell,
        cellSmall = safeSmallCell,
        font = scale.bodyFont.value.toInt().coerceAtLeast(12),
        title = scale.titleFont.value.toInt().coerceAtLeast(16)
    )
}
