package com.example.matematicaperbambini

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    )

    val heightDp = with(density) { size.height.toDp() }
    val widthDp = with(density) { size.width.toDp() }
    val isCompact = heightDp < 700.dp || widthDp < 360.dp

    return if (isCompact) {
        UiSizing(
            isCompact = true,
            spacing = 8.dp,
            pad = 10.dp,
            cell = 46.dp,
            cellSmall = 38.dp,
            font = 16,
            title = 16
        )
    } else {
        UiSizing(
            isCompact = false,
            spacing = 12.dp,
            pad = 16.dp,
            cell = 56.dp,
            cellSmall = 48.dp,
            font = 18,
            title = 18
        )
    }
}
