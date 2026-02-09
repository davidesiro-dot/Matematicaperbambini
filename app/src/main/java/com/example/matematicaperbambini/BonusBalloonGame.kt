package com.example.matematicaperbambini

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
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
    val vy: Float,
    val popped: Boolean = false,
    val particles: List<BalloonParticle> = emptyList()
)

private data class BalloonParticle(
    val dx: Float,
    val dy: Float,
    val radiusPx: Float
)

@Composable
fun BonusRewardHost(
    correctCount: Int,
    rewardsEarned: Int,
    rewardEvery: Int = BONUS_TARGET,
    soundEnabled: Boolean,
    fx: SoundFx,
    onOpenLeaderboard: (LeaderboardTab) -> Unit,
    onRewardEarned: () -> Unit,
    onRewardSkipped: () -> Unit,
    onBonusPromptAction: () -> Unit = {}
) {
    val normalizedRewardEvery = rewardEvery.coerceAtLeast(1)
    val nextRewardAt = (rewardsEarned + 1) * normalizedRewardEvery
    var showPrompt by remember { mutableStateOf(false) }
    var showGame by remember { mutableStateOf(false) }
    var pickedGame by remember { mutableStateOf<BonusGame?>(null) }

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
                        + "\nScegli il gioco bonus!"
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            onBonusPromptAction()
                            pickedGame = BonusGame.Balloons
                            showPrompt = false
                            showGame = true
                            if (soundEnabled) fx.bonus()
                        }
                    ) { Text("Palloncini 🎈") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onBonusPromptAction()
                            pickedGame = BonusGame.Stars
                            showPrompt = false
                            showGame = true
                            if (soundEnabled) fx.bonus()
                        }
                    ) { Text("Stelle ⭐") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onBonusPromptAction()
                        showPrompt = false
                        pickedGame = null
                        onRewardSkipped()
                    }
                ) { Text("Torna agli esercizi") }
            }
        )
    }

    if (showGame) {
        when (pickedGame) {
            BonusGame.Stars -> FallingStarsGame(
                soundEnabled = soundEnabled,
                fx = fx,
                onFinish = {
                    showGame = false
                    pickedGame = null
                    onRewardEarned()
                    onOpenLeaderboard(LeaderboardTab.STARS)
                }
            )
            else -> BonusBalloonGame(
                onFinish = {
                    showGame = false
                    pickedGame = null
                    onRewardEarned()
                    onOpenLeaderboard(LeaderboardTab.BALLOONS)
                }
            )
        }
    }
}

private enum class BonusGame {
    Balloons,
    Stars
}

@Composable
fun BonusBalloonGame(
    balloonCount: Int = 8,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rng = remember { Random(System.currentTimeMillis()) }
    val balloons = remember { mutableStateListOf<BalloonState>() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var finished by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(true) }
    var showNameEntry by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var startTimeNs by remember { mutableStateOf<Long?>(null) }
    var lastFrameNs by remember { mutableStateOf(0L) }

    val widthPx = containerSize.width.toFloat()
    val heightPx = containerSize.height.toFloat()
    val paused = showNameEntry || finished

    val colors = listOf(
        Color(0xFF60A5FA),
        Color(0xFFF87171),
        Color(0xFF34D399),
        Color(0xFFFBBF24),
        Color(0xFFC084FC)
    )

    fun createBalloon(id: Int, sizePx: Float): BalloonState {
        val x = rng.nextFloat() * (widthPx - sizePx).coerceAtLeast(0f)
        val y = rng.nextFloat() * (heightPx - sizePx).coerceAtLeast(0f)
        val speed = with(density) { rng.nextInt(70, 130).dp.toPx() }
        val angle = rng.nextFloat() * 360f
        val vx = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * speed
        val vy = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * speed
        return BalloonState(
            id = id,
            color = colors[id % colors.size],
            sizePx = sizePx,
            x = x,
            y = y,
            vx = vx,
            vy = vy
        )
    }

    LaunchedEffect(finished) {
        if (finished && !saved) {
            showNameEntry = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE))
                )
            )
            .zIndex(2f)
            .padding(8.dp)
            .onSizeChanged { containerSize = it }
    ) {
        val ui = rememberUiSizing()
        val headerPad = ui.pad
        val headerSpacing = if (ui.isCompact) 8.dp else 16.dp
        val balloonSizeDp = if (ui.isCompact) 58.dp else 72.dp
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cloudColor = Color.White.copy(alpha = 0.7f)
            val cloudRadius = size.minDimension * 0.08f
            drawCircle(cloudColor, cloudRadius, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.18f))
            drawCircle(cloudColor, cloudRadius * 0.8f, center = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.16f))
            drawCircle(cloudColor, cloudRadius * 0.9f, center = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.2f))

            drawCircle(cloudColor, cloudRadius * 1.1f, center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.12f))
            drawCircle(cloudColor, cloudRadius, center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.1f))
            drawCircle(cloudColor, cloudRadius * 0.85f, center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.14f))

            drawCircle(cloudColor, cloudRadius * 0.9f, center = androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.3f))
            drawCircle(cloudColor, cloudRadius * 0.7f, center = androidx.compose.ui.geometry.Offset(size.width * 0.56f, size.height * 0.28f))
            drawCircle(cloudColor, cloudRadius * 0.8f, center = androidx.compose.ui.geometry.Offset(size.width * 0.41f, size.height * 0.32f))
        }

        val balloonSizePx = with(density) { balloonSizeDp.toPx() }

        if (started && balloons.isEmpty() && widthPx > 0 && heightPx > 0) {
            repeat(balloonCount) { index ->
                balloons += createBalloon(index, balloonSizePx)
            }
        }

        LaunchedEffect(widthPx, heightPx, finished, started, paused) {
            if (finished || !started || widthPx <= 0f || heightPx <= 0f) return@LaunchedEffect
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

                    if (balloons.isNotEmpty()) {
                        elapsedMs = ((now - (startTimeNs ?: now)) / 1_000_000)
                        for (i in balloons.indices) {
                            val b = balloons[i]
                            if (b.popped) continue
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
                    } else if (!finished) {
                        finished = true
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(headerPad)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = headerSpacing, vertical = if (ui.isCompact) 6.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bonus Round 🎈", fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(headerSpacing))
                        Text("Tempo: ${formatMs(elapsedMs)}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(if (ui.isCompact) 8.dp else 12.dp))

            Text(
                "Scoppia tutti i palloncini!",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        balloons.forEach { balloon ->
            val popProgress by animateFloatAsState(
                targetValue = if (balloon.popped) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "balloonPop"
            )
            val scale = 1f - (0.6f * popProgress)
            val alpha = 1f - popProgress

            if (balloon.popped) {
                LaunchedEffect(balloon.id, balloon.popped) {
                    delay(320)
                    balloons.removeAll { it.id == balloon.id }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(balloon.x.roundToInt(), balloon.y.roundToInt()) }
                    .size(with(density) { balloon.sizePx.toDp() })
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !balloon.popped) {
                        val index = balloons.indexOfFirst { it.id == balloon.id }
                        if (index >= 0) {
                            val particles = List(6) {
                                val angle = rng.nextFloat() * 360f
                                val distance = with(density) { rng.nextInt(16, 30).dp.toPx() }
                                BalloonParticle(
                                    dx = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance,
                                    dy = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance,
                                    radiusPx = with(density) { rng.nextInt(3, 6).dp.toPx() }
                                )
                            }
                            balloons[index] = balloon.copy(popped = true, particles = particles)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bodyHeight = size.height * 0.78f
                    val bodyRect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, bodyHeight)
                    val balloonBrush = Brush.radialGradient(
                        colors = listOf(balloon.color.copy(alpha = 0.95f), balloon.color.copy(alpha = 0.75f)),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.25f),
                        radius = size.minDimension * 0.7f
                    )
                    drawOval(brush = balloonBrush, topLeft = bodyRect.topLeft, size = bodyRect.size)
                    drawOval(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.12f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.22f, bodyHeight * 0.25f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.18f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.12f, bodyHeight * 0.14f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = bodyRect.topLeft,
                        size = bodyRect.size,
                        style = Stroke(width = 3f)
                    )
                    val stringStart = androidx.compose.ui.geometry.Offset(size.width / 2f, bodyHeight)
                    val stringEnd = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height)
                    drawLine(
                        color = Color(0xFF9CA3AF),
                        start = stringStart,
                        end = stringEnd,
                        strokeWidth = 2f
                    )

                    if (balloon.popped) {
                        val particleProgress = popProgress
                        balloon.particles.forEach { particle ->
                            drawCircle(
                                color = Color.White.copy(alpha = (1f - particleProgress).coerceAtLeast(0f)),
                                radius = particle.radiusPx * (0.6f + particleProgress),
                                center = androidx.compose.ui.geometry.Offset(
                                    x = size.width / 2f + particle.dx * particleProgress,
                                    y = size.height / 2f + particle.dy * particleProgress
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showNameEntry) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0F172A)),
                contentAlignment = Alignment.Center
            ) {
                SeaGlassPanel(title = "Salva il tuo tempo") {
                    Text("Scrivi il tuo nome (max 12 caratteri)")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { newValue ->
                            playerName = newValue.trimStart().trimEnd().take(12)
                        },
                        singleLine = true,
                        label = { Text("Nome") }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (saved) return@Button
                            val trimmed = playerName.trim()
                            val finalName = if (trimmed.isNotEmpty()) trimmed else "Bimbo"
                            saved = true
                            addTimeEntry(
                                context,
                                GLOBAL_BALLOONS_LEADERBOARD_ID,
                                ScoreEntry(finalName, elapsedMs)
                            )
                            showNameEntry = false
                            onFinish()
                        }
                    ) { Text("Salva e vai alla classifica") }
                }
            }
        }
    }
}
