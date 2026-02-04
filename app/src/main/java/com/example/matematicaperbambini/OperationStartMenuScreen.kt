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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OperationStartMenuScreen(
    gameMode: GameMode,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onSelectStartMode: (StartMode) -> Unit,
    selectedHelpPreset: HelpPreset,
    onSelectHelpPreset: (HelpPreset) -> Unit
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
                    Text(gameMode.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    Text("Scegli come iniziare", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
            }
            SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
        }

        SeaGlassPanel(title = "Modalità di avvio") {
            Text(
                "Seleziona una modalità:",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StartModeButton(
                    title = "Operazioni casuali",
                    subtitle = "Numeri già pronti",
                    baseColor = Color(0xFF0EA5E9),
                    onClick = { onSelectStartMode(StartMode.RANDOM) }
                )
                StartModeButton(
                    title = "Inserimento manuale",
                    subtitle = "Decidi tu i numeri",
                    baseColor = Color(0xFF22C55E),
                    onClick = { onSelectStartMode(StartMode.MANUAL) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Modalità di gioco",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HelpPreset.values().forEach { preset ->
                    Box(modifier = Modifier.weight(1f)) {
                        ModeSegmentedButton(
                            label = when (preset) {
                                HelpPreset.GUIDED -> "Guidato"
                                HelpPreset.TRAINING -> "Allenamento"
                                HelpPreset.CHALLENGE -> "Sfida"
                            },
                            selected = preset == selectedHelpPreset,
                            onClick = { onSelectHelpPreset(preset) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            AiutiAttiviInfo(selectedHelpPreset)
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun StartModeButton(
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
        label = "startModeBtnScale"
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
                Text(
                    "▶",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(y = 1.dp)
                )
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

@Composable
private fun ModeSegmentedButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ModeButtonText(label)
        }
    }
}

@Composable
private fun ModeButtonText(
    text: String
) {
    val textSizeState = remember(text) { mutableStateOf(16.sp) }

    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        fontSize = textSizeState.value,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && textSizeState.value > 12.sp) {
                textSizeState.value = (textSizeState.value.value - 1).sp
            }
        }
    )
}

@Composable
private fun AiutiAttiviInfo(selectedHelpPreset: HelpPreset) {
    val helps = selectedHelpPreset.toHelpSettings()
    val activeHelps = buildList {
        if (helps.hintsEnabled) add("Suggerimenti")
        if (helps.highlightsEnabled) add("Evidenziazioni")
        if (helps.allowSolution) add("Soluzione disponibile")
        if (helps.autoCheck) add("Controllo automatico")
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Aiuti attivi",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        if (activeHelps.isEmpty()) {
            Text(
                "Nessun aiuto attivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            activeHelps.forEach { help ->
                Text(
                    text = "• $help",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
