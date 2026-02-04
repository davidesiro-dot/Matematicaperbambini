package com.example.matematicaperbambini

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer



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
    MULT_HARD("Moltiplicazioni")
}

enum class TabellineMode(val title: String) {
    CLASSIC("Tabelline classiche"),
    MIXED("Tabelline miste"),
    GAPS("Buchi nella tabellina"),
    REVERSE("Tabellina al contrario"),
    MULTIPLE_CHOICE("Scelta multipla")
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
    OPERATION_START_MENU,
    TABELLINE_MENU,
    GAME,              // Add/Sub/Money (e altro se vuoi)
    MULT_PICKER,       // scegli tabellina 1..10
    MULT_GAME,         // tabellina 10 caselle
    MULT_GAPS_PICKER,
    MULT_MIXED_GAME,
    MULT_GAPS_GAME,
    MULT_REVERSE_GAME,
    MULT_CHOICE_GAME,
    MULT_HARD_GAME,    // moltiplicazioni difficili (2 cifre in colonna)
    DIV_STEP_GAME,     // ✅ Divisioni passo-passo con resto
    LEADERBOARD,
    HOMEWORK_BUILDER,
    HOMEWORK_RUNNER,
    HOMEWORK_REPORTS,
    ASSIGNED_HOMEWORKS
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

        // ✅ RIMOSSO: overlay rettangolare semi-trasparente che “sporca” il layout
        // Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))

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
            .wrapContentHeight()
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
    val backgroundColor = if (isBack) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.31f)
    }
    val borderColor = if (isBack) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Text(
                text = text,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = Color(0xFF111827),
                lineHeight = fontSize
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
fun BonusBar(correctCount: Int, bonusTarget: Int = BONUS_TARGET, ui: UiSizing? = null) {
    val safeTarget = bonusTarget.coerceAtLeast(1)
    val rewardProgress = correctCount % safeTarget
    val label = if (rewardProgress == 0) "Bonus: $safeTarget/$safeTarget 🎈" else "Bonus: $rewardProgress/$safeTarget"
    val p = (rewardProgress / safeTarget.toFloat()).coerceIn(0f, 1f)
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
    ui: UiSizing? = null,
    bonusTarget: Int = BONUS_TARGET,
    showBack: Boolean = true
) {
    val isCompact = ui?.isCompact == true
    val titleSize = (ui?.title ?: 18).sp
    val subtitleSize = if (isCompact) 10.sp else 12.sp
    val buttonSize = if (isCompact) 34.dp else 40.dp
    val iconSize = if (isCompact) 18.dp else 22.dp
    val buttonFont = if (isCompact) 16.sp else 18.sp
    val spacing = if (isCompact) 6.dp else 10.dp

    Row(
        Modifier.fillMaxWidth().statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing)) {
            if (showBack) {
                SmallCircleButton(
                    "⬅",
                    onClick = onBack,
                    size = buttonSize,
                    iconSize = iconSize,
                    fontSize = buttonFont
                )
            } else {
                Spacer(Modifier.size(buttonSize))
            }
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleSize,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Fai $bonusTarget giuste per il BONUS 🎈",
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val fx = remember(context) { SoundFx(context) }
    val isGameScreen = when (screen) {
        Screen.GAME,
        Screen.MULT_GAME,
        Screen.MULT_MIXED_GAME,
        Screen.MULT_GAPS_GAME,
        Screen.MULT_REVERSE_GAME,
        Screen.MULT_CHOICE_GAME,
        Screen.MULT_HARD_GAME,
        Screen.DIV_STEP_GAME,
        Screen.HOMEWORK_RUNNER -> true
        else -> false
    }

    LaunchedEffect(soundEnabled, screen) {
        if (soundEnabled && !isGameScreen) {
            fx.playIntro()
        } else {
            fx.stopIntro()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            fx.release()
        }
    }



    var mode by remember { mutableStateOf(GameMode.ADD) }
    var digits by remember { mutableStateOf(2) }
    var startMode by remember { mutableStateOf(StartMode.RANDOM) }
    var helpPreset by remember { mutableStateOf(HelpPreset.GUIDED) }
    var sessionHelpSettings by remember { mutableStateOf(helpPreset.toHelpSettings()) }

    var pendingDigitsMode by remember { mutableStateOf<GameMode?>(null) }
    var pendingStartMenuMode by remember { mutableStateOf<GameMode?>(null) }

    // tabelline
    var selectedTable by remember { mutableStateOf(2) }
    var selectedGapsTable by remember { mutableStateOf(2) }
    var homeworkQueue by remember { mutableStateOf<List<HomeworkExerciseEntry>>(emptyList()) }
    var lastHomeworkResults by remember { mutableStateOf<List<ExerciseResult>>(emptyList()) }
    var homeworkReports by remember { mutableStateOf<List<HomeworkReport>>(emptyList()) }
    var savedHomeworks by remember { mutableStateOf<List<SavedHomework>>(emptyList()) }
    var homeworkReturnScreen by remember { mutableStateOf(Screen.HOMEWORK_BUILDER) }
    var runningHomeworkId by remember { mutableStateOf<String?>(null) }
    val reportStorage = remember(context) { HomeworkReportStorage(context) }
    val reportScope = rememberCoroutineScope()
    val savedHomeworkRepository = remember(context) { SavedHomeworkRepository(context) }
    val savedHomeworkScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        homeworkReports = reportStorage.loadReports()
        savedHomeworks = savedHomeworkRepository.getAll()
    }

    fun openGame(m: GameMode, d: Int = digits, startModeValue: StartMode = startMode) {
        mode = m
        digits = d
        startMode = startModeValue
        sessionHelpSettings = helpPreset.toHelpSettings()
        navAnim = NavAnim.EXPAND
        screen = Screen.GAME
    }

    fun openStartMenu(m: GameMode) {
        pendingStartMenuMode = m
        navAnim = NavAnim.SLIDE
        screen = Screen.OPERATION_START_MENU
    }

    fun openTabellineMenu() {
        navAnim = NavAnim.SLIDE
        screen = Screen.TABELLINE_MENU
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
                onOpenHomework = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_BUILDER },
                onOpenAssignedHomeworks = { navAnim = NavAnim.SLIDE; screen = Screen.ASSIGNED_HOMEWORKS },
                onOpenReports = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_REPORTS },
                savedHomeworks = savedHomeworks,
                onPickDigitsFor = { m ->
                    openStartMenu(m)
                },
                onPlayDirect = { m ->
                    when (m) {
                        GameMode.MULT -> openTabellineMenu()
                        GameMode.MULT_HARD -> openStartMenu(m)
                        GameMode.DIV -> openStartMenu(m) // ✅
                        GameMode.MONEY -> openGame(m, digits, startMode)
                        else -> openGame(m, digits)
                    }
                }
            )

            Screen.OPERATION_START_MENU -> {
                val startMenuMode = pendingStartMenuMode ?: GameMode.ADD
                OperationStartMenuScreen(
                    gameMode = startMenuMode,
                    soundEnabled = soundEnabled,
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                    onSelectStartMode = { chosenMode ->
                        startMode = chosenMode
                        sessionHelpSettings = helpPreset.toHelpSettings()
                        when (startMenuMode) {
                            GameMode.ADD, GameMode.SUB -> {
                                if (chosenMode == StartMode.MANUAL) {
                                    openGame(startMenuMode, digits, startMode)
                                } else {
                                    pendingDigitsMode = startMenuMode
                                    navAnim = NavAnim.SLIDE
                                    screen = Screen.DIGITS_PICKER
                                }
                            }
                            GameMode.MULT -> { navAnim = NavAnim.SLIDE; screen = Screen.MULT_PICKER }
                            GameMode.MULT_HARD -> { navAnim = NavAnim.SLIDE; screen = Screen.MULT_HARD_GAME }
                            GameMode.DIV -> { navAnim = NavAnim.SLIDE; screen = Screen.DIV_STEP_GAME }
                            GameMode.MONEY -> openGame(startMenuMode, digits, startMode)
                        }
                    },
                    selectedHelpPreset = helpPreset,
                    onSelectHelpPreset = { helpPreset = it }
                )
            }

            Screen.DIGITS_PICKER -> {
                val m = pendingDigitsMode ?: GameMode.ADD
                DigitsPickerScreen(
                    mode = m,
                    soundEnabled = soundEnabled,
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onBack = { navAnim = NavAnim.SLIDE; screen = Screen.OPERATION_START_MENU },
                    onOpenLeaderboard = { openLb() },
                    onStart = { chosenDigits -> openGame(m, chosenDigits, startMode) }
                )
            }

            Screen.TABELLINE_MENU -> TabellineMenuScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onSelectClassicManual = {
                    startMode = StartMode.MANUAL
                    navAnim = NavAnim.SLIDE
                    screen = Screen.MULT_PICKER
                },
                onSelectMode = { mode ->
                    navAnim = NavAnim.SLIDE
                    screen = when (mode) {
                        TabellineMode.MIXED -> Screen.MULT_MIXED_GAME
                        TabellineMode.GAPS -> Screen.MULT_GAPS_PICKER
                        TabellineMode.REVERSE -> Screen.MULT_REVERSE_GAME
                        TabellineMode.MULTIPLE_CHOICE -> Screen.MULT_CHOICE_GAME
                        TabellineMode.CLASSIC -> Screen.MULT_PICKER
                    }
                }
            )

            Screen.MULT_PICKER -> MultTablePickerScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.TABELLINE_MENU },
                onPickTable = { table ->
                    selectedTable = table
                    navAnim = NavAnim.SLIDE
                    screen = Screen.MULT_GAME
                }
            )

            Screen.MULT_GAPS_PICKER -> MultTablePickerScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.TABELLINE_MENU },
                onPickTable = { table ->
                    selectedGapsTable = table
                    navAnim = NavAnim.SLIDE
                    screen = Screen.MULT_GAPS_GAME
                }
            )

            Screen.MULT_GAME -> MultiplicationTableGame(
                table = selectedTable,
                startMode = startMode,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.MULT_PICKER },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_MIXED_GAME -> TabellineMixedGame(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.TABELLINE_MENU },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_GAPS_GAME -> TabellineGapsGame(
                table = selectedGapsTable,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.MULT_GAPS_PICKER },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_REVERSE_GAME -> TabellinaReverseGame(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.TABELLINE_MENU },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_CHOICE_GAME -> TabellineMultipleChoiceGame(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.TABELLINE_MENU },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) }
            )

            Screen.MULT_HARD_GAME -> HardMultiplication2x2Game(
                startMode = startMode,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) },
                helps = sessionHelpSettings
            )

            // ✅ NUOVA SCHERMATA: DIVISIONI PASSO-PASSO
            Screen.DIV_STEP_GAME -> DivisionStepGame(
                startMode = startMode,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) },
                helps = sessionHelpSettings
            )

            Screen.GAME -> GameRouter(
                mode = mode,
                digits = digits,
                startMode = startMode,
                helps = sessionHelpSettings,
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

            Screen.HOMEWORK_BUILDER -> HomeworkBuilderScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                lastResults = lastHomeworkResults,
                onStartHomework = { configs ->
                    homeworkQueue = buildExerciseQueue(configs)
                    homeworkReturnScreen = Screen.HOMEWORK_BUILDER
                    runningHomeworkId = null
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
                },
                onSaveHomework = { savedHomework ->
                    savedHomeworkScope.launch {
                        savedHomeworkRepository.save(savedHomework)
                        savedHomeworks = savedHomeworkRepository.getAll()
                    }
                }
            )

            Screen.HOMEWORK_RUNNER -> HomeworkRunnerScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                fx = fx,
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) },
                queue = homeworkQueue,
                previousReports = homeworkReports,
                onExit = { results ->
                    lastHomeworkResults = results
                    runningHomeworkId = null
                    navAnim = NavAnim.SLIDE
                    screen = homeworkReturnScreen
                },
                onSaveReport = { report ->
                    reportScope.launch {
                        reportStorage.saveReport(report)
                        homeworkReports = listOf(report) + homeworkReports
                    }
                },
                homeworkId = runningHomeworkId,
                onHomeworkCompleted = { homeworkId ->
                    savedHomeworks = savedHomeworks.filterNot { it.id == homeworkId }
                    savedHomeworkScope.launch {
                        savedHomeworkRepository.delete(homeworkId)
                        savedHomeworks = savedHomeworkRepository.getAll()
                    }
                }
            )

            Screen.HOMEWORK_REPORTS -> HomeworkReportsScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                reports = homeworkReports
            )

            Screen.ASSIGNED_HOMEWORKS -> AssignedHomeworksScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                savedHomeworks = savedHomeworks,
                onStartHomework = { savedHomework ->
                    homeworkQueue = buildExerciseQueue(savedHomework.tasks)
                    homeworkReturnScreen = Screen.ASSIGNED_HOMEWORKS
                    runningHomeworkId = savedHomework.id
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
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
    onOpenHomework: () -> Unit,
    onOpenAssignedHomeworks: () -> Unit,
    onOpenReports: () -> Unit,
    savedHomeworks: List<SavedHomework>,
    onPickDigitsFor: (GameMode) -> Unit, // ADD/SUB
    onPlayDirect: (GameMode) -> Unit     // MULT/DIV/MONEY/MULT_HARD
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val sizeProfile = when {
            maxHeight < 620.dp -> MenuSizeProfile.Small
            maxHeight < 760.dp -> MenuSizeProfile.Normal
            else -> MenuSizeProfile.Large
        }

        val baseButtonHeight = when (sizeProfile) {
        MenuSizeProfile.Small -> 56.dp
        MenuSizeProfile.Normal -> 64.dp
        MenuSizeProfile.Large -> 72.dp
    }
        val buttonHeight = baseButtonHeight * 0.75f

        val buttonSpacing = when (sizeProfile) {
            MenuSizeProfile.Small -> 10.dp
            MenuSizeProfile.Normal -> 12.dp
            MenuSizeProfile.Large -> 16.dp
        }
        val cardPadding = when (sizeProfile) {
            MenuSizeProfile.Small -> 14.dp
            MenuSizeProfile.Normal -> 18.dp
            MenuSizeProfile.Large -> 22.dp
        }
        val buttonTextSize = when (sizeProfile) {
            MenuSizeProfile.Small -> 16.sp
            MenuSizeProfile.Normal -> 18.sp
            MenuSizeProfile.Large -> 20.sp
        }
        val bonusTextSize = when (sizeProfile) {
            MenuSizeProfile.Small -> 11.sp
            MenuSizeProfile.Normal -> 12.sp
            MenuSizeProfile.Large -> 13.sp
        }

        val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()


        val buttons = listOf(
            MenuButtonData(
                title = "Addizioni",
                baseColor = Color(0xFFE74C3C),
                iconText = "＋",
                onClick = { onPickDigitsFor(GameMode.ADD) }
            ),
            MenuButtonData(
                title = "Sottrazioni",
                baseColor = Color(0xFF2ECC71),
                iconText = "−",
                onClick = { onPickDigitsFor(GameMode.SUB) }
            ),
            MenuButtonData(
                title = "Moltiplicazioni",
                baseColor = Color(0xFF8B5CF6),
                iconText = "××",
                onClick = { onPlayDirect(GameMode.MULT_HARD) }
            ),
            MenuButtonData(
                title = "Divisioni",
                baseColor = Color(0xFF3498DB),
                iconText = "÷",
                onClick = { onPlayDirect(GameMode.DIV) } // ✅ ora apre DivisionStepGame
            ),
            MenuButtonData(
                title = "Tabelline",
                baseColor = Color(0xFFF39C12),
                iconText = "×",
                onClick = { onPlayDirect(GameMode.MULT) }
            ),
            MenuButtonData(
                title = "Conta i soldi",
                baseColor = Color(0xFFF1C40F),
                iconText = "€",
                onClick = { onPlayDirect(GameMode.MONEY) }
            )
        )

        val animationsReady = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { animationsReady.value = true }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                TopActionsPill(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp)
                ) {
                    SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
                    SmallCircleButton("📋") { onOpenHomework() }
                    SmallCircleButton("🗂️") { onOpenReports() }
                    SmallCircleButton("🏆") { onOpenLeaderboard() }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (logoPainter != null) {
                    Image(
                        painter = logoPainter,
                        contentDescription = "Math Kids",
                        modifier = Modifier.fillMaxWidth(1.02f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Logo mancante",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val assignedCount = savedHomeworks.size
            if (assignedCount > 0) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AssignedHomeworkBanner(count = assignedCount, onOpen = onOpenAssignedHomeworks)
                }
            }

            // ✅ BOTTONI SENZA PANNELLO "GLASSCARD" + alzati di 20dp
            val offsetPx = with(LocalDensity.current) {
                when (sizeProfile) {
                    MenuSizeProfile.Small -> 12.dp.toPx()
                    MenuSizeProfile.Normal -> 16.dp.toPx()
                    MenuSizeProfile.Large -> 18.dp.toPx()
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                buttons.forEachIndexed { index, data ->
                    val alpha by animateFloatAsState(
                        targetValue = if (animationsReady.value) 1f else 0f,
                        animationSpec = tween(durationMillis = 220, delayMillis = index * 60),
                        label = "menuAlpha$index"
                    )
                    val offsetY by animateFloatAsState(
                        targetValue = if (animationsReady.value) 0f else offsetPx,
                        animationSpec = tween(durationMillis = 220, delayMillis = index * 60),
                        label = "menuOffset$index"
                    )

                    KidsMenuButton(
                        title = data.title,
                        baseColor = data.baseColor,
                        icon = {
                            Text(
                                data.iconText,
                                color = Color.White,
                                fontSize = if (data.iconText.length > 1) 20.sp else 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        },
                        onClick = data.onClick,
                        height = buttonHeight,
                        textSize = buttonTextSize,
                        modifier = Modifier.graphicsLayer {
                            this.alpha = alpha
                            translationY = offsetY
                        }
                    )
                }

                BonusPill(
                    text = "Completa gli esercizi e vinci il BONUS! 🎈",
                    fontSize = bonusTextSize
                )
            }



                }
            }
        }


@Composable
fun AssignedHomeworkBanner(count: Int, onOpen: () -> Unit) {
    if (count <= 0) return

    val transition = rememberInfiniteTransition(label = "assignedHomeworkPulse")
    val badgeScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assignedHomeworkBadgeScale"
    )

    SeaGlassPanel(modifier = Modifier.clickable { onOpen() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎒",
                fontSize = 24.sp
            )
            Text(
                text = "Hai $count compiti assegnati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(badgeScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(0.dp)
                )
            }
            TextButton(onClick = onOpen) {
                Text("Apri")
            }
        }
    }
}


@Composable
private fun KidsMenuButton(
    title: String,
    baseColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    textSize: TextUnit = 18.sp
) {
    val dark = baseColor
    val light = lerp(baseColor, Color.White, 0.35f)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label = "kidsBtnScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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
        // highlight top gloss
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
                icon()
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private enum class MenuSizeProfile {
    Small,
    Normal,
    Large
}

private data class MenuButtonData(
    val title: String,
    val baseColor: Color,
    val iconText: String,
    val onClick: () -> Unit
)

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White.copy(alpha = 0.14f),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}


@Composable
private fun TitlePill(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color(0xFF1F2937),
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize
        )
    }
}

@Composable
private fun BonusPill(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF111827).copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.96f),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TopActionsPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.29f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
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
                                Box(modifier = Modifier.weight(1f)) {
                                    Button(
                                        onClick = { onPickTable(n) },
                                        modifier = Modifier.fillMaxWidth().height(64.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF59E0B),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("× $n", fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    }
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
    startMode: StartMode,
    helps: HelpSettings,
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
            startMode = startMode,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            helps = helps
        )


        GameMode.SUB -> LongSubtractionGame(
            digits = digits,
            startMode = startMode,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            helps = helps
        )

        // ✅ anche se dal menu vai alla schermata dedicata, qui lo gestiamo uguale
        GameMode.DIV -> DivisionStepGame(
            startMode = startMode,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            helps = helps
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
                        "Nessun record ancora.\nCompleta $BONUS_TARGET risposte giuste e gioca coi palloncini!",
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
                        "Nessun record ancora.\nCompleta $BONUS_TARGET risposte giuste e gioca con le stelline!",
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
