package com.example.matematicaperbambini

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WindowType { Compact, Medium, Expanded }

data class UiScale(
    val digitSize: Dp,
    val digitSmallSize: Dp,
    val spacing: Dp,
    val titleFont: TextUnit,
    val bodyFont: TextUnit
)

val LocalWindowType = staticCompositionLocalOf { WindowType.Compact }
val LocalUiScale = staticCompositionLocalOf {
    UiScale(
        digitSize = 44.dp,
        digitSmallSize = 38.dp,
        spacing = 8.dp,
        titleFont = 20.sp,
        bodyFont = 14.sp
    )
}

@Composable
fun calculateWindowType(): WindowType {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.coerceAtLeast(1)
    return when {
        widthDp < 600 -> WindowType.Compact
        widthDp < 840 -> WindowType.Medium
        else -> WindowType.Expanded
    }
}

@Composable
fun rememberUiScale(windowType: WindowType): UiScale {
    return when (windowType) {
        WindowType.Compact -> UiScale(
            digitSize = 44.dp,
            digitSmallSize = 38.dp,
            spacing = 8.dp,
            titleFont = 20.sp,
            bodyFont = 14.sp
        )

        WindowType.Medium -> UiScale(
            digitSize = 56.dp,
            digitSmallSize = 48.dp,
            spacing = 12.dp,
            titleFont = 24.sp,
            bodyFont = 16.sp
        )

        WindowType.Expanded -> UiScale(
            digitSize = 68.dp,
            digitSmallSize = 58.dp,
            spacing = 16.dp,
            titleFont = 28.sp,
            bodyFont = 18.sp
        )
    }
}
