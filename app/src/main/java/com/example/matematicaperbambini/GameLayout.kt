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
    hintText: String,
    ui: UiSizing,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    message: String? = null
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(ui.pad),
        verticalArrangement = Arrangement.spacedBy(ui.spacing)
    ) {
        item {
            SeaGlassPanel {
                GameHeader(
                    title = title,
                    soundEnabled = soundEnabled,
                    onToggleSound = onToggleSound,
                    onBack = onBack,
                    onLeaderboard = onOpenLeaderboard,
                    ui = ui
                )
            }
        }

        item {
            SeaGlassPanel {
                CompactHud(
                    correctCount = correctCount,
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

        item { Spacer(Modifier.height(ui.spacing)) }
    }
}

@Composable
private fun CompactHud(
    correctCount: Int,
    hintText: String,
    ui: UiSizing
) {
    val rewardProgress = correctCount % 5
    val label = if (rewardProgress == 0) "Bonus: 5/5 🎈" else "Bonus: $rewardProgress/5 🎈"
    val progress = (rewardProgress / 5f).coerceIn(0f, 1f)
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = onLeft, modifier = Modifier.weight(1f)) {
            Text(leftText)
        }

        if (center != null) {
            Box(Modifier.weight(1f)) { center() }
        }

        Button(onClick = onRight, modifier = Modifier.weight(1f)) {
            Text(rightText)
        }
    }
}
