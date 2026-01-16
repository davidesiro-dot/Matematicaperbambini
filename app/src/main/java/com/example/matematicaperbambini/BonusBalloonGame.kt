package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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

private data class BalloonState(
    val id: Int,
    val color: Color,
    val sizePx: Float,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float
)

@Composable
fun BonusRewardHost(
    correctCount: Int,
    rewardsEarned: Int,
    boardId: String,
    soundEnabled: Boolean,
    fx: SoundFx,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    val nextRewardAt = (rewardsEarned + 1) * 5
    var showPrompt by remember { mutableStateOf(false) }
    var showGame by remember { mutableStateOf(false) }

    LaunchedEffect(correctCount, rewardsEarned) {
        if (correctCount >= nextRewardAt) {
            showPrompt = true
        }
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Complimenti! 🎉") },
            text = {
                Text(
                    "Hai fatto $nextRewardAt operazioni corrette."
                            + "\nÈ ora del Bonus Round con i palloncini!"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPrompt = false
                        showGame = true
                        if (soundEnabled) fx.bonus()
                    }
                ) { Text("Gioca") }
            }
        )
    }

    if (showGame) {
        BonusBalloonGame(
            onFinished = { timeMs ->
                addEntry(context, boardId, ScoreEntry("Bimbo", timeMs))
                showGame = false
                onRewardEarned()
            }
        )
    }
}

@Composable
fun BonusBalloonGame(
    balloonCount: Int = 8,
    onFinished: (Long) -> Unit
) {
    val density = LocalDensity.current
    val rng = remember { Random(System.currentTimeMillis()) }
    val balloons = remember { mutableStateListOf<BalloonState>() }

    var elapsedMs by remember { mutableStateOf(0L) }
    var finished by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    // ✅ size reale del Box (in px) misurata con onSizeChanged
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val colors = listOf(
        Color(0xFF60A5FA),
        Color(0xFFF87171),
        Color(0xFF34D399),
        Color(0xFFFBBF24),
        Color(0xFFC084FC)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE))
                )
            )
            .zIndex(2f)
    ) {
        val widthPx = boxSize.width.toFloat()
        val heightPx = boxSize.height.toFloat()
        val balloonSizePx = with(density) { 64.dp.toPx() }

        if (!started && balloons.isEmpty() && widthPx > 0 && heightPx > 0) {
            repeat(balloonCount) { index ->
                val x = rng.nextFloat() * (widthPx - balloonSizePx).coerceAtLeast(0f)
                val y = rng.nextFloat() * (heightPx - balloonSizePx).coerceAtLeast(0f)
                val speed = with(density) { rng.nextInt(70, 130).dp.toPx() }
                val angle = rng.nextFloat() * 360f
                val vx = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * speed
                val vy = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * speed
                balloons += BalloonState(
                    id = index,
                    color = colors[index % colors.size],
                    sizePx = balloonSizePx,
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy
                )
            }
            started = true
        }

        LaunchedEffect(widthPx, heightPx, finished, started) {
            if (finished || !started) return@LaunchedEffect
            var startTimeNs: Long? = null
            var lastFrameNs = 0L

            while (isActive && !finished) {
                withFrameNanos { now ->
                    if (startTimeNs == null) startTimeNs = now
                    if (lastFrameNs == 0L) lastFrameNs = now
                    val dt = (now - lastFrameNs) / 1_000_000_000f
                    lastFrameNs = now

                    if (balloons.isNotEmpty()) {
                        elapsedMs = ((now - (startTimeNs ?: now)) / 1_000_000)
                        for (i in balloons.indices) {
                            val b = balloons[i]
                            var nx = b.x + b.vx * dt
                            var ny = b.y + b.vy * dt
                            var nvx = b.vx
                            var nvy = b.vy

                            if (nx <= 0f || nx >= widthPx - b.sizePx) {
                                nx = nx.coerceIn(0f, (widthPx - b.sizePx).coerceAtLeast(0f))
                                nvx = -nvx
                            }
                            if (ny <= 0f || ny >= heightPx - b.sizePx) {
                                ny = ny.coerceIn(0f, (heightPx - b.sizePx).coerceAtLeast(0f))
                                nvy = -nvy
                            }

                            balloons[i] = b.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                        }
                    } else {
                        finished = true
                        onFinished(elapsedMs)
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bonus Round 🎈", fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(16.dp))
                    Text("Tempo: ${formatMs(elapsedMs)}", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Scoppia tutti i palloncini!",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        balloons.forEach { balloon ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(balloon.x.roundToInt(), balloon.y.roundToInt()) }
                    .size(with(density) { balloon.sizePx.toDp() })
                    .background(balloon.color, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable { balloons.remove(balloon) },
                contentAlignment = Alignment.Center
            ) {
                Text("🎈", fontSize = MaterialTheme.typography.titleLarge.fontSize)
            }
        }
    }
}
