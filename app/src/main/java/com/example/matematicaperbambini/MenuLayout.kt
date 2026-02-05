package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                header()
            }
            MenuLogoArea(logoPainter = logoPainter, height = logoAreaHeight)
            content(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun MenuLogoArea(
    logoPainter: Painter?,
    height: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        if (logoPainter != null) {
            Image(
                painter = logoPainter,
                contentDescription = "Math Kids",
                modifier = Modifier
                    .fillMaxWidth(1.02f)
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
