package com.example.matematicaperbambini

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

/**
 * Cornice unica stile "Sottrazioni":
 * - Header (back/suono/classifica)
 * - CompactHud (bonus + hint)
 * - Contenuto (slot)
 * - Bottom bar (slot)
 */
@Composable
fun GameScreenFrame(
    title: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    correctCount: Int,
    bonusTarget: Int = BONUS_TARGET,
    hintText: String,
    ui: UiSizing,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    message: String? = null
) {
    val isCompact = ui.isCompact
    val buttonSize = if (isCompact) 34.dp else 40.dp
    val iconSize = if (isCompact) 18.dp else 22.dp
    val buttonFont = if (isCompact) 16.sp else 18.sp

    val contentSpacing = maxOf(ui.spacing, 10.dp)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(ui.pad),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            item {
                SeaGlassPanel {
                    GameHeader(
                        title = title,
                        soundEnabled = soundEnabled,
                        onToggleSound = onToggleSound,
                        onBack = onBack,
                        onLeaderboard = onOpenLeaderboard,
                        ui = ui,
                        bonusTarget = bonusTarget,
                        showBack = false
                    )
                }
            }

            item {
                SeaGlassPanel {
                    CompactHud(
                        correctCount = correctCount,
                        bonusTarget = bonusTarget,
                        hintText = hintText,
                        ui = ui
                    )
                }
            }

            item { content() }

            item {
                AnimatedVisibility(visible = !message.isNullOrBlank()) {
                    SeaGlassPanel {
                        Text(
                            text = message.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (bottomBar != null) {
                item {
                    SeaGlassPanel {
                        bottomBar()
                    }
                }
            }

            item { Spacer(Modifier.height(contentSpacing)) }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = ui.pad, top = ui.pad)
        ) {
            SmallCircleButton(
                "⬅",
                onClick = onBack,
                size = buttonSize,
                iconSize = iconSize,
                fontSize = buttonFont
            )
        }
    }
}

@Composable
private fun CompactHud(
    correctCount: Int,
    bonusTarget: Int,
    hintText: String,
    ui: UiSizing
) {
    val safeTarget = bonusTarget.coerceAtLeast(1)
    val rewardProgress = correctCount % safeTarget
    val label = if (rewardProgress == 0) {
        "Bonus: $safeTarget/$safeTarget 🎈"
    } else {
        "Bonus: $rewardProgress/$safeTarget 🎈"
    }
    val progress = (rewardProgress / safeTarget.toFloat()).coerceIn(0f, 1f)
    val isCompact = ui.isCompact
    val fontSize = if (isCompact) 12.sp else 14.sp
    val progressHeight = if (isCompact) 6.dp else 8.dp
    var showHintDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(progressHeight)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showHintDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("?", fontSize = fontSize)
            }
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("Chiudi")
                }
            },
            title = { Text("Cosa fare") },
            text = { Text(hintText) }
        )
    }
}

/**
 * Riga bottoni standard (riutilizzabile)
 */
@Composable
fun GameBottomActions(
    leftText: String,
    onLeft: () -> Unit,
    rightText: String,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
    center: (@Composable () -> Unit)? = null
) {
    val longLabelLimit = 8
    val baseFont = 16.sp
    val compactFont = 15.sp
    val leftFont = if (leftText.length > longLabelLimit) compactFont else baseFont
    val rightFont = if (rightText.length > longLabelLimit) compactFont else baseFont

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onLeft,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                leftText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontSize = leftFont
            )
        }

        if (center != null) {
            Box(Modifier.weight(1f)) { center() }
        }

        Button(
            onClick = onRight,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                rightText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontSize = rightFont
            )
        }
    }
}

@Composable
fun SuccessDialog(
    show: Boolean,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
    resultText: String? = null
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bravissimo! 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Operazione corretta!")
                if (!resultText.isNullOrBlank()) {
                    Text("Risultato: $resultText")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nuova operazione")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}
