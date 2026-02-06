package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun MenuHeaderLogoLayout(
    logoPainter: Painter?,
    logoAreaHeight: Dp,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val logoTopPadding = headerHeightDp + 5.dp
    val contentTopPadding = logoTopPadding + logoAreaHeight + 10.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            header()
        }

        MenuLogoArea(
            logoPainter = logoPainter,
            height = logoAreaHeight,
            modifier = Modifier
                .padding(top = logoTopPadding)
                .align(Alignment.TopCenter)
        )

        content(
            Modifier
                .fillMaxWidth()
                .padding(top = contentTopPadding)
        )
    }
}

@Composable
fun MenuLogoArea(
    logoPainter: Painter?,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        if (logoPainter != null) {
            Image(
                painter = logoPainter,
                contentDescription = "Math Kids",
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .heightIn(max = height),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "Logo mancante",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
