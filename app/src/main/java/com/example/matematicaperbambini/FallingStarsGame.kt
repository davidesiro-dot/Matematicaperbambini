package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.compose.foundation.layout.Spacer

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
    soundEnabled: Boolean,
    fx: SoundFx,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rng = remember { Random(System.currentTimeMillis()) }

    val stars = remember { mutableStateListOf<StarState>() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var elapsedMs by remember { mutableLongStateOf(0L) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var showNameEntry by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    var startTimeNs by remember { mutableStateOf<Long?>(null) }
    var lastFrameNs by remember { mutableLongStateOf(0L) }

    val widthPx = containerSize.width.toFloat()
    val heightPx = containerSize.height.toFloat()

    // ✅ terreno
    val groundHeightDp = 20.dp
    val groundHeightPx = with(density) { groundHeightDp.toPx() }

    LaunchedEffect(finished) {
        if (finished && !saved) showNameEntry = true
    }

    fun createStar(id: Int, startInView: Boolean): StarState {
        val sizePx = with(density) { rng.nextInt(32, 52).dp.toPx() }

        val safeWidth = (widthPx - sizePx).coerceAtLeast(1f)
        val x = rng.nextFloat() * safeWidth

        val safeHeight = (heightPx - groundHeightPx - sizePx).coerceAtLeast(1f)
        val y = if (startInView) {
            rng.nextFloat() * safeHeight
        } else {
            -sizePx - rng.nextFloat() * (heightPx.coerceAtLeast(1f))
        }

        val vy = with(density) { rng.nextInt(160, 260).dp.toPx() }
        val isGolden = rng.nextInt(6) == 0
        return StarState(id, x, y, vy, sizePx, isGolden)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {

        // ✅ BACKGROUND IMMAGINE
        Image(
            painter = painterResource(id = R.drawable.stars_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // (opzionale) overlay leggero per leggere meglio testi
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x33000000),
                            Color(0x77000000)
                        )
                    )
                )
        )

        // --- header ---
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.85f)) {
                    Text(
                        "Punti: $score",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.85f)) {
                    val remaining = ((durationMs - elapsedMs) / 1000).coerceAtLeast(0)
                    Text(
                        "Tempo: %02d:%02d".format(remaining / 60, remaining % 60),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Tocca le stelle!", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // --- spawn iniziale ---
        if (stars.isEmpty() && widthPx > 0f && heightPx > 0f) {
            repeat(starCount) { i -> stars += createStar(i, true) }
        }

        // --- loop animazione ---
        LaunchedEffect(widthPx, heightPx, finished) {
            if (widthPx <= 0f || heightPx <= 0f || finished) return@LaunchedEffect

            while (isActive && !finished) {
                withFrameNanos { now ->
                    if (startTimeNs == null) startTimeNs = now
                    if (lastFrameNs == 0L) lastFrameNs = now

                    val dt = (now - lastFrameNs) / 1_000_000_000f
                    lastFrameNs = now

                    elapsedMs = (now - (startTimeNs ?: now)) / 1_000_000
                    if (elapsedMs >= durationMs) {
                        finished = true
                        return@withFrameNanos
                    }

                    val bottomLimit = heightPx - groundHeightPx

                    for (i in stars.indices) {
                        val s = stars[i]
                        val ny = s.y + s.vy * dt
                        stars[i] =
                            if (ny > bottomLimit) createStar(s.id, false)
                            else s.copy(y = ny)
                    }
                }
            }
        }

        // --- stelle (tap compatibile) ---
        stars.forEachIndexed { i, star ->
            val hitExtraPx = with(density) { 24.dp.toPx() }
            val hitSizePx = star.sizePx + hitExtraPx
            val offsetX = (star.x - hitExtraPx / 2f).roundToInt()
            val offsetY = (star.y - hitExtraPx / 2f).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(with(density) { hitSizePx.toDp() })
                    .pointerInput(finished, soundEnabled, star.id, star.isGolden) {
                        detectTapGestures(
                            onTap = {
                                if (!finished) {
                                    val gain = if (star.isGolden) 3 else 1
                                    score += gain

                                    if (soundEnabled) {
                                        if (star.isGolden) fx.bonus() else fx.correct()
                                    }

                                    stars[i] = createStar(star.id, false)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (star.isGolden) "🌟" else "⭐",
                    fontSize = with(density) { (star.sizePx * 0.7f).toSp() }
                )
            }
        }

        // --- terreno (blocca tap in basso) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(groundHeightDp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF14532D), Color(0xFF16A34A))
                    )
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                    }
                }
                .zIndex(10f)
        )

        // --- salvataggio nome ---
        if (showNameEntry) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0F172A)),
                contentAlignment = Alignment.Center
            ) {
                SeaGlassPanel(title = "Salva i tuoi punti") {
                    Text("Scrivi il tuo nome (max 12 caratteri)")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it.take(12) },
                        singleLine = true,
                        label = { Text("Nome") }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (saved) return@Button
                            saved = true
                            val name = playerName.ifBlank { "Bimbo" }

                            addScoreEntry(
                                context,
                                GLOBAL_STARS_LEADERBOARD_ID,
                                ScoreEntry(name, score.toLong())
                            )

                            showNameEntry = false
                            onFinish()
                        }
                    ) {
                        Text("Salva e vai alla classifica")
                    }
                }
            }
        }
    }
}
