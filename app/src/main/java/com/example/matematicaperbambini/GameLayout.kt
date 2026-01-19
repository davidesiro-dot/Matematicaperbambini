package com.example.matematicaperbambini

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cornice unica stile "Sottrazioni":
 * - Header (back/suono/classifica)
 * - BonusBar
 * - InfoPanel con istruzioni/hint
 * - Pannello contenuto (slot)
 * - Riga bottoni (slot)
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
    bottomBar: @Composable (() -> Unit)? = null,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ui.pad),
        verticalArrangement = Arrangement.spacedBy(ui.spacing)
    ) {
        GameHeader(
            title = title,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = onOpenLeaderboard,
            ui = ui
        )

        BonusBar(correctCount = correctCount, ui = ui)

        InfoPanel(
            title = "Cosa fare",
            text = hintText,
            ui = ui,
            maxLines = if (ui.isCompact) 1 else 2
        )

        content()

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        bottomBar?.invoke()

        Spacer(Modifier.height(ui.spacing))
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
