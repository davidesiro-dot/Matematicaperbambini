package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.random.Random

data class StarState(
    val id: Int,
    val x: Float,
    val y: Float,
    val vy: Float,
    val sizePx: Float,
    val isGolden: Boolean
)

@Composable
fun FallingStarsGame(
    starCount: Int = 10,
    durationMs: Long = 30_000,
    boardId: String,
    soundEnabled: Boolean,
    fx: SoundFx,
    onBackToMath: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rng = remember { Random(System.currentTimeMillis()) }
    val stars = remember { mutableStateListOf<StarState>() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var score by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(true) }
    var playerName by remember { mutableStateOf("Bimbo") }
    var saved by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }
    var startTimeNs by remember { mutableStateOf<Long?>(null) }
    var lastFrameNs by remember { mutableStateOf(0L) }

    val widthPx = containerSize.width.toFloat()
    val heightPx = containerSize.height.toFloat()
    val paused = showNameDialog || showLeaderboard || finished
    val starsBoardId = starsBoardId(boardId)

    fun createStar(id: Int, startInView: Boolean): StarState {
        val sizePx = with(density) { rng.nextInt(36, 56).dp.toPx() }
        val x = rng.nextFloat() * (widthPx - sizePx).coerceAtLeast(0f)
        val y = if (startInView) {
            rng.nextFloat() * (heightPx - sizePx).coerceAtLeast(0f)
        } else {
            -sizePx - rng.nextFloat() * heightPx
        }
        val vy = with(density) { rng.nextInt(60, 140).dp.toPx() }
        val isGolden = rng.nextInt(6) == 0
        return StarState(id = id, x = x, y = y, vy = vy, sizePx = sizePx, isGolden = isGolden)
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Nome del bimbo") },
            text = {
                Column {
                    Text("Scrivi il tuo nome per iniziare!")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { newValue ->
                            val filtered = newValue.trimStart().trimEnd().take(12)
                            playerName = filtered
                        },
                        singleLine = true,
                        label = { Text("Nome") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = playerName.trim()
                        playerName = if (trimmed.isNotEmpty()) trimmed else "Bimbo"
                        showNameDialog = false
                        started = true
                    }
                ) {
                    Text("Inizia")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                )
            )
            .zIndex(2f)
            .padding(8.dp)
            .onSizeChanged { containerSize = it }
    ) {
        if (started && stars.isEmpty() && widthPx > 0f && heightPx > 0f) {
            repeat(starCount) { index ->
                stars += createStar(index, startInView = true)
            }
        }

        LaunchedEffect(widthPx, heightPx, finished, started, paused) {
            if (!started || finished || widthPx <= 0f || heightPx <= 0f) return@LaunchedEffect
            if (paused) {
                startTimeNs = null
                lastFrameNs = 0L
                return@LaunchedEffect
            }

            while (isActive && !finished) {
                withFrameNanos { now ->
                    if (startTimeNs == null) startTimeNs = now - (elapsedMs * 1_000_000)
                    if (lastFrameNs == 0L) lastFrameNs = now
                    val dt = (now - lastFrameNs) / 1_000_000_000f
                    lastFrameNs = now

                    val elapsed = ((now - (startTimeNs ?: now)) / 1_000_000)
                    elapsedMs = elapsed
                    if (elapsedMs >= durationMs) {
                        elapsedMs = durationMs
                        finished = true
                        return@withFrameNanos
                    }

                    for (i in stars.indices) {
                        val star = stars[i]
                        val ny = star.y + star.vy * dt
                        if (ny > heightPx + star.sizePx) {
                            stars[i] = createStar(star.id, startInView = false)
                        } else {
                            stars[i] = star.copy(y = ny)
                        }
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.85f)
                ) {
                    Text(
                        "Punti: $score",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.85f)
                ) {
                    val remainingMs = (durationMs - elapsedMs).coerceAtLeast(0L)
                    val remainingSec = (remainingMs / 1000).toInt()
                    val min = remainingSec / 60
                    val sec = remainingSec % 60
                    Text(
                        "Tempo: %02d:%02d".format(min, sec),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                SmallCircleButton("🏆") { showLeaderboard = true }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Tocca le stelle!",
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Bold
            )
        }

        for (i in stars.indices) {
            val star = stars[i]
            Box(
                modifier = Modifier
                    .offset { IntOffset(star.x.roundToInt(), star.y.roundToInt()) }
                    .size(with(density) { star.sizePx.toDp() })
                    .clickable {
                        if (!finished) {
                            val gain = if (star.isGolden) 3 else 1
                            score += gain
                            if (soundEnabled) {
                                if (star.isGolden) fx.bonus() else fx.correct()
                            }
                            stars[i] = createStar(star.id, startInView = false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (star.isGolden) "🌟" else "⭐",
                    fontSize = with(density) { (star.sizePx * 0.7f).toSp() }
                )
            }
        }

        if (finished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Finito! Punti: $score",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (!saved) {
                                    saved = true
                                    addScoreEntry(context, starsBoardId, ScoreEntry(playerName, score.toLong()))
                                    refreshToken++
                                    onBackToMath()
                                }
                            }
                        ) {
                            Text("Continua")
                        }
                    }
                }
            }
        }

        if (showLeaderboard) {
            val entries = remember(refreshToken) {
                loadEntries(context, starsBoardId).sortedByDescending { it.value }
            }
            LeaderboardDialog(
                title = "Classifica Stelline",
                entries = entries,
                valueFormatter = { it.toString() },
                onDismiss = { showLeaderboard = false }
            )
        }
    }
}
