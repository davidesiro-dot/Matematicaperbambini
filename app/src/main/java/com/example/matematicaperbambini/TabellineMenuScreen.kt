package com.example.matematicaperbambini

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabellineMenuScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onSelectClassicManual: () -> Unit,
    onSelectMode: (TabellineMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallCircleButton("⬅") { onBack() }
                Column {
                    Text("Tabelline", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    Text("Scegli la modalità", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
            }
            SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
        }

        SeaGlassPanel(title = "Modalità Tabelline") {
            Text(
                "Seleziona una modalità di gioco:",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeMenuButton(
                    title = "Inserimento manuale",
                    subtitle = "Scegli la tabellina da completare",
                    baseColor = Color(0xFF22C55E),
                    onClick = onSelectClassicManual
                )
                ModeMenuButton(
                    title = "Tabelline miste",
                    subtitle = "Moltiplicazioni casuali 1×1..10×10",
                    baseColor = Color(0xFFF59E0B),
                    onClick = { onSelectMode(TabellineMode.MIXED) }
                )
                ModeMenuButton(
                    title = "Buchi nella tabellina",
                    subtitle = "Completa solo i risultati mancanti",
                    baseColor = Color(0xFF3B82F6),
                    onClick = { onSelectMode(TabellineMode.GAPS) }
                )
                ModeMenuButton(
                    title = "Tabellina al contrario",
                    subtitle = "Trova l’operazione corretta",
                    baseColor = Color(0xFF8B5CF6),
                    onClick = { onSelectMode(TabellineMode.REVERSE) }
                )
                ModeMenuButton(
                    title = "Scelta multipla",
                    subtitle = "Scegli il risultato giusto",
                    baseColor = Color(0xFFEF4444),
                    onClick = { onSelectMode(TabellineMode.MULTIPLE_CHOICE) }
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ModeMenuButton(
    title: String,
    subtitle: String,
    baseColor: Color,
    onClick: () -> Unit
) {
    val dark = baseColor
    val light = androidx.compose.ui.graphics.lerp(baseColor, Color.White, 0.35f)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label = "tabellineModeBtnScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(colors = listOf(light, dark)))
            .border(2.dp, dark.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.45f)
                )
            ) { onClick() }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.90f), Color.Transparent)
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.01f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
