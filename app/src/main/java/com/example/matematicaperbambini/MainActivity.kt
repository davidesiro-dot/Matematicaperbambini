package com.example.matematicaperbambini

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit


// -----------------------------
// LEADERBOARD (persistente)
// -----------------------------
data class ScoreEntry(val name: String, val value: Long)

private const val PREFS_NAME = "math_kids_prefs"
const val GLOBAL_STARS_LEADERBOARD_ID = "global_stars_score"
const val GLOBAL_BALLOONS_LEADERBOARD_ID = "global_balloons_time"
private fun encodeName(name: String) = URLEncoder.encode(name, "UTF-8")
private fun decodeName(name: String) = URLDecoder.decode(name, "UTF-8")

fun loadEntries(context: Context, boardId: String): MutableList<ScoreEntry> {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = sp.getString("leaderboard_$boardId", "") ?: ""
    if (raw.isBlank()) return mutableListOf()
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        return runCatching {
            val jsonArray = org.json.JSONArray(trimmed)
            val entries = mutableListOf<ScoreEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val name = obj.optString("name", "Giocatore")
                val value = obj.optLong("value", 0L)
                entries += ScoreEntry(name, value)
            }
            entries
        }.getOrElse { mutableListOf() }
    }
    return raw.split(",").mapNotNull { token ->
        val parts = token.split("|", limit = 2)
        if (parts.size != 2) null else {
            val t = parts[0].toLongOrNull() ?: return@mapNotNull null
            val n = runCatching { decodeName(parts[1]) }.getOrNull() ?: "Giocatore"
            ScoreEntry(n, t)
        }
    }.toMutableList()
}

fun saveEntries(context: Context, boardId: String, entries: List<ScoreEntry>) {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonArray = org.json.JSONArray()
    entries.forEach { entry ->
        val obj = org.json.JSONObject()
        obj.put("name", entry.name)
        obj.put("value", entry.value)
        jsonArray.put(obj)
    }
    sp.edit().putString("leaderboard_$boardId", jsonArray.toString()).apply()
}

fun addEntry(context: Context, boardId: String, entry: ScoreEntry) {
    val list = loadEntries(context, boardId)
    list.add(entry)
    val top10 = list.sortedBy { it.value }.take(10)
    saveEntries(context, boardId, top10)
}

fun addTimeEntry(context: Context, boardId: String, entry: ScoreEntry) {
    val list = loadEntries(context, boardId)
    list.add(entry)
    val sorted = list.sortedBy { it.value }.take(50)
    saveEntries(context, boardId, sorted)
}

fun addScoreEntry(context: Context, boardId: String, entry: ScoreEntry) {
    val list = loadEntries(context, boardId)
    list.add(entry)
    val sorted = list.sortedByDescending { it.value }.take(50)
    saveEntries(context, boardId, sorted)
}

fun clearLeaderboard(context: Context, boardId: String) {
    saveEntries(context, boardId, emptyList())
}

fun computeRankTime(entries: List<ScoreEntry>, myValue: Long): Int {
    val sorted = entries.sortedBy { it.value }
    val index = sorted.indexOfFirst { it.value == myValue }
    return if (index >= 0) index + 1 else sorted.size + 1
}

fun computeRankScore(entries: List<ScoreEntry>, myValue: Long): Int {
    val sorted = entries.sortedByDescending { it.value }
    val index = sorted.indexOfFirst { it.value == myValue }
    return if (index >= 0) index + 1 else sorted.size + 1
}

fun bestTime(context: Context, boardId: String): Long? =
    loadEntries(context, boardId).minByOrNull { it.value }?.value

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val cent = (ms % 1000) / 10
    return if (min > 0) "%d:%02d.%02d".format(min, sec, cent) else "%d.%02d".format(sec, cent)
}

// -----------------------------
// MODI DI GIOCO
// -----------------------------
enum class GameMode(val title: String) {
    ADD("Addizioni"),
    SUB("Sottrazioni"),
    MULT("Tabelline"),
    DIV("Divisioni"),
    MONEY("Conta i soldi"),
    MULT_HARD("Moltiplicazioni difficili")
}

enum class LeaderboardTab {
    BALLOONS,
    STARS
}

// -----------------------------
// NAV
// -----------------------------
private enum class Screen {
    HOME,
    DIGITS_PICKER,     // Add/Sub
    GAME,              // Add/Sub/Money (e altro se vuoi)
    MULT_PICKER,       // scegli tabellina 1..10
    MULT_GAME,         // tabellina 10 caselle
    MULT_HARD_GAME,    // moltiplicazioni difficili (2 cifre in colonna)
    DIV_STEP_GAME,     // ✅ Divisioni passo-passo con resto
    LEADERBOARD
}

private enum class NavAnim { SLIDE, EXPAND }

// -----------------------------
// APP
// -----------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0EA5E9),
                    secondary = Color(0xFF22C55E),
                    tertiary = Color(0xFFF59E0B),
                    surface = Color.White,
                    background = Color(0xFFF6F7FB),
                    onSurface = Color(0xFF111827),
                    onBackground = Color(0xFF111827),
                ),
                typography = Typography(
                    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    bodyLarge = TextStyle(fontSize = 16.sp),
                    bodyMedium = TextStyle(fontSize = 14.sp)
                )
            ) { AppBackground { AppShell() } }
        }
    }
}

// -----------------------------
// BACKGROUND (SEA)
// -----------------------------
@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_sea),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        content()
    }
}

// -----------------------------
// GLASS PANEL
// -----------------------------
@Composable
fun SeaGlassPanel(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(26.dp))
            .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            content()
        }
    }
}

@Composable
fun SmallCircleButton(
    text: String,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    fontSize: TextUnit = 18.sp,
    onClick: () -> Unit
) {
    val isBack = text == "⬅"

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.78f))
            .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF111827),
                modifier = Modifier.size(iconSize)
            )
        } else {
            Text(
                text = text,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = Color(0xFF111827)
            )
        }
    }
}



@Composable
fun InfoPanel(
    title: String,
    text: String,
    ui: UiSizing? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    SeaGlassPanel(title = title) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (ui?.font ?: 18).sp
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BonusBar(correctCount: Int, ui: UiSizing? = null) {
    val rewardProgress = correctCount % 5
    val label = if (rewardProgress == 0) "Bonus: 5/5 🎈" else "Bonus: $rewardProgress/5"
    val p = (rewardProgress / 5f).coerceIn(0f, 1f)
    val fontSize = (ui?.font ?: 18).sp
    val barHeight = if (ui?.isCompact == true) 8.dp else 10.dp

    SeaGlassPanel(title = "Progresso") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Obiettivo",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                fontSize = fontSize
            )
            Text(
                label,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = fontSize
            )
        }
        LinearProgressIndicator(
            progress = { p },
            modifier = Modifier.fillMaxWidth().height(barHeight).clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = Color.White.copy(alpha = 0.60f)
        )
    }
}

@Composable
fun GameHeader(
    title: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onLeaderboard: () -> Unit,
    ui: UiSizing? = null
) {
    val isCompact = ui?.isCompact == true
    val titleSize = (ui?.title ?: 18).sp
    val subtitleSize = if (isCompact) 10.sp else 12.sp
    val buttonSize = if (isCompact) 34.dp else 40.dp
    val iconSize = if (isCompact) 18.dp else 22.dp
    val buttonFont = if (isCompact) 16.sp else 18.sp
    val spacing = if (isCompact) 6.dp else 10.dp

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing)) {
            SmallCircleButton(
                "⬅",
                onClick = onBack,
                size = buttonSize,
                iconSize = iconSize,
                fontSize = buttonFont
            )
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleSize,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Fai 5 giuste per il BONUS 🎈",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = subtitleSize,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            SmallCircleButton(
                if (soundEnabled) "🔊" else "🔇",
                onClick = onToggleSound,
                size = buttonSize,
                iconSize = iconSize,
                fontSize = buttonFont
            )
            SmallCircleButton(
                "🏆",
                onClick = onLeaderboard,
                size = buttonSize,
                iconSize = iconSize,
                fontSize = buttonFont
            )
        }
    }
}

// -----------------------------
// APP SHELL + NAV
// -----------------------------
@Composable
private fun AppShell() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var navAnim by remember { mutableStateOf(NavAnim.SLIDE) }
    var returnScreenAfterLeaderboard by remember { mutableStateOf<Screen?>(null) }
    var leaderboardTab by remember { mutableStateOf(LeaderboardTab.STARS) }

    var soundEnabled by remember { mutableStateOf(true) }
    val fx = remember { SoundFx() }

    var mode by remember { mutableStateOf(GameMode.ADD) }
    var digits by remember { mutableStateOf(2) }

    var pendingDigitsMode by remember { mutableStateOf<GameMode?>(null) }

    // tabelline
    var selectedTable by remember { mutableStateOf(2) }

    fun openGame(m: GameMode, d: Int = digits) {
        mode = m
        digits = d
        navAnim = NavAnim.EXPAND
        screen = Screen.GAME
    }

    fun openLb(tab: LeaderboardTab = leaderboardTab) {
        leaderboardTab = tab
        returnScreenAfterLeaderboard = screen
        navAnim = NavAnim.SLIDE
        screen = Screen.LEADERBOARD
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (navAnim == NavAnim.EXPAND) {
                (scaleIn(initialScale = 0.90f, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(200)))
                    .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 1.04f, animationSpec = tween(220)))
            } else {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "nav"
    ) { s ->
        when (s) {
            Screen.HOME -> HomeMenuKids(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onPickDigitsFor = { m ->
                    pendingDigitsMode = m
                    navAnim = NavAnim.SLIDE
                    screen = Screen.DIGITS_PICKER
                },
                onPlayDirect = { m ->
                    when (m) {
                        GameMode.MULT -> { navAnim = NavAnim.SLIDE; screen = Screen.MULT_PICKER }
                        GameMode.MULT_HARD -> { navAnim = NavAnim.SLIDE; screen = Screen.MULT_HARD_GAME }
                        GameMode.DIV -> { navAnim = NavAnim.SLIDE; screen = Screen.DIV_STEP_GAME } // ✅
                        else -> openGame(m, digits)
                    }
                }
            )

            Screen.DIGITS_PICKER -> {
                val m = pendingDigitsMode ?: GameMode.ADD
                DigitsPickerScreen(
                    mode = m,
                    soundEnabled = soundEnabled,
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                    onOpenLeaderboard = { openLb() },
                    onStart = { chosenDigits -> openGame(m, chosenDigits) }
                )
            }

            Screen.MULT_PICKER -> MultTablePickerScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onPickTable = { table ->
                    selectedTable = table
                    navAnim = NavAnim.SLIDE
                    screen = Screen.MULT_GAME
                }
            )

            Screen.MULT_GAME -> MultiplicationTableGame(
                table = selectedTable,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.MULT_PICKER },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_HARD_GAME -> HardMultiplication2x2Game(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            // ✅ NUOVA SCHERMATA: DIVISIONI PASSO-PASSO
            Screen.DIV_STEP_GAME -> DivisionStepGame(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenLeaderboard = { openLb() }
            )

            Screen.GAME -> GameRouter(
                mode = mode,
                digits = digits,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.EXPAND; screen = Screen.HOME },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.LEADERBOARD -> LeaderboardScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                selectedTab = leaderboardTab,
                onTabChange = { leaderboardTab = it },
                onBack = {
                    navAnim = NavAnim.SLIDE
                    screen = returnScreenAfterLeaderboard ?: Screen.HOME
                    returnScreenAfterLeaderboard = null
                }
            )
        }
    }
}

// -----------------------------
// HOME MENU (kids glossy)
// -----------------------------
@Composable
private fun HomeMenuKids(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onPickDigitsFor: (GameMode) -> Unit, // ADD/SUB
    onPlayDirect: (GameMode) -> Unit     // MULT/DIV/MONEY/MULT_HARD
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Image(
            painter = painterResource(R.drawable.math_kids_logo),
            contentDescription = "Math Kids",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .fillMaxWidth(0.90f),
            contentScale = ContentScale.Fit
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
            SmallCircleButton("🏆") { onOpenLeaderboard() }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 200.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KidsMenuButton(
                title = "Addizioni",
                baseColor = Color(0xFFE74C3C),
                icon = { Text("＋", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) },
                onClick = { onPickDigitsFor(GameMode.ADD) }
            )
            KidsMenuButton(
                title = "Sottrazioni",
                baseColor = Color(0xFF2ECC71),
                icon = { Text("−", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) },
                onClick = { onPickDigitsFor(GameMode.SUB) }
            )
            KidsMenuButton(
                title = "Tabelline",
                baseColor = Color(0xFFF39C12),
                icon = { Text("×", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) },
                onClick = { onPlayDirect(GameMode.MULT) }
            )
            KidsMenuButton(
                title = "Moltiplicazioni difficili",
                baseColor = Color(0xFF8B5CF6),
                icon = { Text("××", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black) },
                onClick = { onPlayDirect(GameMode.MULT_HARD) }
            )
            KidsMenuButton(
                title = "Divisioni",
                baseColor = Color(0xFF3498DB),
                icon = { Text("÷", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) },
                onClick = { onPlayDirect(GameMode.DIV) } // ✅ ora apre DivisionStepGame
            )
            KidsMenuButton(
                title = "Conta i soldi (Euro)",
                baseColor = Color(0xFFF1C40F),
                icon = { Text("€", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) },
                onClick = { onPlayDirect(GameMode.MONEY) }
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Completa gli esercizi e vinci il BONUS! 🎈",
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun KidsMenuButton(
    title: String,
    baseColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = baseColor
    val light = lerp(baseColor, Color.White, 0.35f)

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.985f else 1f, label = "kidsBtnScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(10.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(colors = listOf(light, dark)))
            .border(3.dp, dark.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 18.dp),
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
                        colors = listOf(Color.White.copy(alpha = 0.40f), Color.Transparent)
                    )
                )
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { icon() }

            Text(
                title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(70)
            pressed = false
        }
    }
}

// -----------------------------
// DIGITS PICKER (Add/Sub)
// -----------------------------
@Composable
private fun DigitsPickerScreen(
    mode: GameMode,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onStart: (digits: Int) -> Unit
) {
    val title = if (mode == GameMode.ADD) "Addizioni" else "Sottrazioni"
    val accent = if (mode == GameMode.ADD) Color(0xFFE74C3C) else Color(0xFF2ECC71)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallCircleButton("⬅") { onBack() }
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    Text("Scegli quante cifre", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
                SmallCircleButton("🏆") { onOpenLeaderboard() }
            }
        }

        SeaGlassPanel(title = "Difficoltà") {
            Text("Vuoi operazioni con:", fontWeight = FontWeight.Bold, color = Color(0xFF374151))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DigitsPill(text = "2 cifre", baseColor = accent, onClick = { onStart(2) }, modifier = Modifier.weight(1f))
                DigitsPill(text = "3 cifre", baseColor = accent, onClick = { onStart(3) }, modifier = Modifier.weight(1f))
            }

            Text(
                "Suggerimento: 2 cifre per iniziare 🙂",
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun DigitsPill(
    text: String,
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = baseColor
    val light = lerp(baseColor, Color.White, 0.35f)

    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(light, dark)))
            .border(3.dp, dark.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

// -----------------------------
// TABELLINE PICKER 1..10
// -----------------------------
@Composable
fun MultTablePickerScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onPickTable: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GameHeader(
            title = "Scegli la tabellina",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        SeaGlassPanel(title = "Tabelline") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 4) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0 until 3) {
                            val n = row * 3 + col + 1
                            if (n <= 10) {
                                Button(
                                    onClick = { onPickTable(n) },
                                    modifier = Modifier.weight(1f).height(64.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("× $n", fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

// -----------------------------
// GAME ROUTER
// -----------------------------
@Composable
fun GameRouter(
    mode: GameMode,
    digits: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    when (mode) {
        GameMode.ADD -> LongAdditionGame(
            digits = digits,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus
        )


        GameMode.SUB -> LongSubtractionGame(
            digits = digits,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus
        )

        // ✅ anche se dal menu vai alla schermata dedicata, qui lo gestiamo uguale
        GameMode.DIV -> DivisionStepGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard
        )

        GameMode.MONEY -> MoneyCountGame(
            soundEnabled,
            onToggleSound,
            fx,
            onBack,
            onOpenLeaderboard,
            onOpenLeaderboardFromBonus
        )

        else -> {
            // MULT e MULT_HARD hanno schermate dedicate in AppShell
            InfoPanel("Info", "Modalità non disponibile qui.")
        }
    }
}

// -----------------------------
// LEADERBOARD
// -----------------------------
@Composable
fun LeaderboardScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    selectedTab: LeaderboardTab,
    onTabChange: (LeaderboardTab) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val balloonsId = GLOBAL_BALLOONS_LEADERBOARD_ID
    val starsId = GLOBAL_STARS_LEADERBOARD_ID

    var refreshToken by remember { mutableStateOf(0) }
    val balloonEntries = remember(refreshToken) {
        loadEntries(context, balloonsId).sortedBy { it.value }
    }
    val starEntries = remember(refreshToken) {
        loadEntries(context, starsId).sortedByDescending { it.value }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallCircleButton("⬅") { onBack() }
                Column {
                    Text("Classifica 🏆", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    Text("Bonus Round", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
            }
            SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
        }

        SeaGlassPanel(title = "Classifiche Bonus") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTabPill("🎈 Palloncini", selected = selectedTab == LeaderboardTab.BALLOONS) {
                    onTabChange(LeaderboardTab.BALLOONS)
                }
                ModeTabPill("⭐ Stelline", selected = selectedTab == LeaderboardTab.STARS) {
                    onTabChange(LeaderboardTab.STARS)
                }
            }
        }

        when (selectedTab) {
            LeaderboardTab.BALLOONS -> SeaGlassPanel(title = "Palloncini (Tempo)") {
                if (balloonEntries.isEmpty()) {
                    Text(
                        "Nessun record ancora.\nCompleta 5 risposte giuste e gioca coi palloncini!",
                        color = Color(0xFF6B7280)
                    )
                } else {
                    balloonEntries.take(10).forEachIndexed { i, e ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${i + 1}  ${e.name}", fontWeight = FontWeight.Bold)
                            Text(
                                formatMs(e.value),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { clearLeaderboard(context, balloonsId); refreshToken++ },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Svuota classifica palloncini") }
            }

            LeaderboardTab.STARS -> SeaGlassPanel(title = "Stelline (Punti)") {
                if (starEntries.isEmpty()) {
                    Text(
                        "Nessun record ancora.\nCompleta 5 risposte giuste e gioca con le stelline!",
                        color = Color(0xFF6B7280)
                    )
                } else {
                    starEntries.take(10).forEachIndexed { i, e ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${i + 1}  ${e.name}", fontWeight = FontWeight.Bold)
                            Text(
                                "${e.value}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { clearLeaderboard(context, starsId); refreshToken++ },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Svuota classifica stelline") }
            }
        }
    }
}

@Composable
fun LeaderboardDialog(
    title: String,
    entries: List<ScoreEntry>,
    valueFormatter: (Long) -> String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entries.isEmpty()) {
                    Text("Nessun record ancora.")
                } else {
                    entries.take(10).forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${index + 1}  ${entry.name}", fontWeight = FontWeight.Bold)
                            Text(
                                valueFormatter(entry.value),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
private fun ModeTabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.70f)
    val fg = if (selected) Color.White else Color(0xFF111827)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) { Text(text, color = fg, fontWeight = FontWeight.Bold) }
}
