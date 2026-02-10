package com.example.matematicaperbambini

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer

/**
 * Cornice unica stile "Sottrazioni":
 * - Header (back/suono/aiuto/classifica)
 * - CompactHud (bonus)
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
    ui: UiSizing,
    modifier: Modifier = Modifier,
    bonusLabelOverride: String? = null,
    bonusProgressOverride: Float? = null,
    content: @Composable () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    message: String? = null
) {
    val contentSpacing = maxOf(ui.spacing, 10.dp)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            SeaGlassPanel(modifier = Modifier.padding(horizontal = ui.pad, vertical = ui.pad)) {
                GameHeader(
                    title = title,
                    soundEnabled = soundEnabled,
                    onToggleSound = onToggleSound,
                    onBack = onBack,
                    onLeaderboard = onOpenLeaderboard,
                    ui = ui,
                    bonusTarget = bonusTarget,
                    showBack = true
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(ui.pad),
                verticalArrangement = Arrangement.spacedBy(contentSpacing)
            ) {
                item {
                    SeaGlassPanel {
                        CompactHud(
                            correctCount = correctCount,
                            bonusTarget = bonusTarget,
                            ui = ui,
                            bonusLabelOverride = bonusLabelOverride,
                            bonusProgressOverride = bonusProgressOverride
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
        }

    }
}

@Composable
private fun CompactHud(
    correctCount: Int,
    bonusTarget: Int,
    ui: UiSizing,
    bonusLabelOverride: String? = null,
    bonusProgressOverride: Float? = null
) {
    val safeTarget = bonusTarget.coerceAtLeast(1)
    val rewardProgress = correctCount % safeTarget
    val defaultLabel = if (rewardProgress == 0) {
        "Bonus: $safeTarget/$safeTarget 🎈"
    } else {
        "Bonus: $rewardProgress/$safeTarget 🎈"
    }
    val label = bonusLabelOverride ?: defaultLabel
    val progress = bonusProgressOverride?.coerceIn(0f, 1f)
        ?: (rewardProgress / safeTarget.toFloat()).coerceIn(0f, 1f)
    val isCompact = ui.isCompact
    val fontSize = if (isCompact) 12.sp else 14.sp
    val progressHeight = if (isCompact) 6.dp else 8.dp

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
        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = onLeft,
                modifier = Modifier.fillMaxWidth(),
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
        }

        if (center != null) {
            Box(Modifier.weight(1f)) { center() }
        }

        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = onRight,
                modifier = Modifier.fillMaxWidth(),
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
}

@Composable
fun SuccessDialog(
    show: Boolean,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
    resultText: String? = null,
    confirmText: String = "Nuova operazione",
    extraActionText: String? = null,
    onExtraAction: (() -> Unit)? = null
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(confirmText)
                }

                if (!extraActionText.isNullOrBlank() && onExtraAction != null) {
                    Button(
                        onClick = onExtraAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(extraActionText)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chiudi")
                }
            }
        }
    )
}
