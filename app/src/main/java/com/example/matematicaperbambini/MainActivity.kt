package com.example.matematicaperbambini

import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.ExperimentalFoundationApi




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
    IMPARA_MENU,
    LEARN_MENU,
    GUIDED_PATH,
    GAME_MENU,
    HOMEWORK_MENU,
    HOMEWORK_CODES,
    DIGITS_PICKER,     // Add/Sub
    OPERATION_START_MENU,
    TABELLINE_MENU,
    GUIDED_TABLE_PICKER,
    GUIDED_TABLE_GAME,
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

private enum class BonusHomeGame {
    Balloons,
    Stars
}

private enum class LearnFilter {
    ALL,
    ADDITIONS,
    SUBTRACTIONS,
    MULTIPLICATIONS,
    DIVISIONS,
    TIMES_TABLES
}


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
    wrapContentHeight: Boolean = true,
    content: @Composable () -> Unit
) {
    val heightModifier = if (wrapContentHeight) {
        Modifier.wrapContentHeight()
    } else {
        Modifier.fillMaxHeight()
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
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
    showBack: Boolean = true,
    useStatusBarsPadding: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isCompact = ui?.isCompact == true
    val titleSize = (ui?.title ?: 18).sp
    val subtitleSize = if (isCompact) 10.sp else 12.sp
    val buttonSize = if (isCompact) 34.dp else 40.dp
    val iconSize = if (isCompact) 18.dp else 22.dp
    val buttonFont = if (isCompact) 16.sp else 18.sp
    val spacing = if (isCompact) 6.dp else 10.dp

    val headerModifier = modifier
        .fillMaxWidth()
        .headerOffsetFromStatusBar(includeStatusBarPadding = useStatusBarsPadding)

    Row(
        headerModifier,
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
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var navAnim by rememberSaveable { mutableStateOf(NavAnim.SLIDE) }
    var returnScreenAfterLeaderboard by rememberSaveable { mutableStateOf<Screen?>(null) }
    var leaderboardTab by rememberSaveable { mutableStateOf(LeaderboardTab.STARS) }

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
    var isLearnFlow by remember { mutableStateOf(false) }

    var pendingDigitsMode by remember { mutableStateOf<GameMode?>(null) }
    var pendingStartMenuMode by remember { mutableStateOf<GameMode?>(null) }

    // tabelline
    var selectedTable by remember { mutableStateOf(2) }
    var selectedGapsTable by remember { mutableStateOf(2) }
    var guidedTable by remember { mutableStateOf(2) }
    var homeworkQueue by remember { mutableStateOf<List<HomeworkExerciseEntry>>(emptyList()) }
    var lastHomeworkResults by remember { mutableStateOf<List<ExerciseResult>>(emptyList()) }
    var homeworkReports by remember { mutableStateOf<List<HomeworkReport>>(emptyList()) }
    var savedHomeworks by remember { mutableStateOf<List<SavedHomework>>(emptyList()) }
    var homeworkCodes by remember { mutableStateOf<List<HomeworkCodeEntry>>(emptyList()) }
    var homeworkReturnScreen by remember { mutableStateOf(Screen.HOMEWORK_BUILDER) }
    var runningHomeworkId by remember { mutableStateOf<String?>(null) }
    var runningHomeworkFromCode by remember { mutableStateOf(false) }
    var learnCategoryFilter by remember { mutableStateOf(LearnFilter.ALL) }
    var gradeFilter by remember { mutableStateOf<GradeLevel?>(null) }
    val completedGuidedLessonIds = remember { mutableStateListOf<String>() }
    var activeGuidedLessonId by remember { mutableStateOf<String?>(null) }
    val guidedCatalog = remember { buildBaseCatalog() }
    val reportStorage = remember(context) { HomeworkReportStorage(context) }
    val reportScope = rememberCoroutineScope()
    val savedHomeworkRepository = remember(context) { SavedHomeworkRepository(context) }
    val savedHomeworkScope = rememberCoroutineScope()
    val homeworkCodeRepository = remember(context) { HomeworkCodeRepository(context) }
    val homeworkCodeScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        homeworkReports = reportStorage.loadReports()
        savedHomeworks = savedHomeworkRepository.getAll()
        homeworkCodes = homeworkCodeRepository.getAll()
    }

    fun openGame(m: GameMode, d: Int = digits, startModeValue: StartMode = startMode) {
        mode = m
        digits = d
        startMode = startModeValue
        sessionHelpSettings = helpPreset.toHelpSettings()
        navAnim = NavAnim.EXPAND
        screen = Screen.GAME
    }

    fun openGuidedSession() {
        isLearnFlow = true
        helpPreset = HelpPreset.GUIDED
        sessionHelpSettings = HelpPreset.GUIDED.toHelpSettings()
        startMode = StartMode.RANDOM
    }

    fun openStartMenu(m: GameMode) {
        pendingStartMenuMode = m
        if (!isLearnFlow) {
            helpPreset = HelpPreset.TRAINING
        }
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

    val windowType = calculateWindowType()
    val uiScale = rememberUiScale(windowType)

    CompositionLocalProvider(
        LocalWindowType provides windowType,
        LocalUiScale provides uiScale
    ) {
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
        AdaptiveRootLayout(
            windowType = windowType,
            screen = s,
            onNavigate = { target ->
                navAnim = NavAnim.SLIDE
                screen = target
            }
        ) {
        when (s) {
            Screen.HOME -> HomeMenuKids(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onOpenLeaderboardFromBonus = { tab -> openLb(tab) },
                onOpenGameMenu = { isLearnFlow = false; navAnim = NavAnim.SLIDE; screen = Screen.GAME_MENU },
                onOpenLearnMenu = { isLearnFlow = true; navAnim = NavAnim.SLIDE; screen = Screen.LEARN_MENU },
                onOpenHomeworkMenu = { isLearnFlow = false; navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_MENU },
                onOpenReports = { isLearnFlow = false; navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_REPORTS },
                savedHomeworks = savedHomeworks,
                fx = fx,
            )

            Screen.LEARN_MENU -> LearnMenuScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onBack = { isLearnFlow = false; navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenGuidedPath = { navAnim = NavAnim.SLIDE; screen = Screen.GUIDED_PATH },
                onOpenAutonomousLearn = { navAnim = NavAnim.SLIDE; screen = Screen.IMPARA_MENU }
            )

            Screen.GUIDED_PATH -> {
                GuidedPathScreen(
                    soundEnabled = soundEnabled,
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onOpenLeaderboard = { openLb() },
                    onBack = { navAnim = NavAnim.SLIDE; screen = Screen.LEARN_MENU },
                    selectedFilter = learnCategoryFilter,
                    onFilterChange = { learnCategoryFilter = it },
                    gradeFilter = gradeFilter,
                    onGradeFilterChange = { gradeFilter = it },
                    lessons = guidedCatalog,
                    onStartLesson = { lesson ->
                        val generated = try {
                            when {
                                lesson.fixedExercises.isNotEmpty() -> lesson.fixedExercises
                                lesson.generator != null -> lesson.generator.invoke(kotlin.random.Random(System.currentTimeMillis()))
                                else -> emptyList()
                            }
                        } catch (e: Throwable) {
                            DefaultLogger.warn("GUIDED_PATH", "generator failed for ${lesson.id}: ${e.message}")
                            Toast.makeText(context, "Lezione non configurata", Toast.LENGTH_SHORT).show()
                            emptyList()
                        }
                        if (generated.isEmpty()) {
                            Toast.makeText(context, "Lezione non configurata", Toast.LENGTH_SHORT).show()
                        } else {
                            openGuidedSession()
                            activeGuidedLessonId = lesson.id
                            homeworkQueue = generated.map { instance ->
                                HomeworkExerciseEntry(instance = instance, helps = lesson.helpPreset ?: HelpPreset.GUIDED.toHelpSettings())
                            }
                            homeworkReturnScreen = Screen.GUIDED_PATH
                            runningHomeworkId = null
                            runningHomeworkFromCode = false
                            navAnim = NavAnim.SLIDE
                            screen = Screen.HOMEWORK_RUNNER
                        }
                    },
                    completedLessonIds = completedGuidedLessonIds.toSet()
                )
            }

            Screen.IMPARA_MENU -> LearnMenuKids(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.LEARN_MENU },
                onStartAddition = {
                    openGuidedSession()
                    openStartMenu(GameMode.ADD)
                },
                onStartSubtraction = {
                    openGuidedSession()
                    openStartMenu(GameMode.SUB)
                },
                onStartMultiplication = {
                    openGuidedSession()
                    openStartMenu(GameMode.MULT_HARD)
                },
                onStartDivision = {
                    openGuidedSession()
                    openStartMenu(GameMode.DIV)
                },
                onStartGuidedTables = {
                    openGuidedSession()
                    navAnim = NavAnim.SLIDE
                    screen = Screen.GUIDED_TABLE_PICKER
                }
            )

            Screen.GAME_MENU -> GameMenuKids(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onPickDigitsFor = { m ->
                    isLearnFlow = false
                    openStartMenu(m)
                },
                onPlayDirect = { m ->
                    isLearnFlow = false
                    when (m) {
                        GameMode.MULT -> openTabellineMenu()
                        GameMode.MULT_HARD -> openStartMenu(m)
                        GameMode.DIV -> openStartMenu(m) // ✅
                        GameMode.MONEY -> openGame(m, digits, startMode)
                        else -> openGame(m, digits)
                    }
                }
            )

            Screen.HOMEWORK_MENU -> HomeworkMenu(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                onOpenHomeworkBuilder = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_BUILDER },
                onOpenAssignedHomeworks = { navAnim = NavAnim.SLIDE; screen = Screen.ASSIGNED_HOMEWORKS },
                onOpenHomeworkCodes = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_CODES }
            )

            Screen.HOMEWORK_CODES -> HomeworkCodesScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onOpenLeaderboard = { openLb() },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_MENU },
                codes = homeworkCodes,
                onDeleteCode = { entry ->
                    homeworkCodeScope.launch {
                        homeworkCodeRepository.delete(entry.id)
                        homeworkCodes = homeworkCodeRepository.getAll()
                    }
                },
                onShareCode = { entry -> shareHomeworkCode(context, entry) },
                onStartHomeworkFromCode = { payload ->
                    homeworkQueue = buildExerciseQueue(payload.tasks)
                    homeworkReturnScreen = Screen.HOMEWORK_CODES
                    runningHomeworkId = null
                    runningHomeworkFromCode = true
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
                }
            )

            Screen.OPERATION_START_MENU -> {
                val startMenuMode = pendingStartMenuMode ?: GameMode.ADD
                OperationStartMenuScreen(
                    gameMode = startMenuMode,
                    soundEnabled = soundEnabled,
                    onToggleSound = { soundEnabled = !soundEnabled },
                    onBack = {
                        navAnim = NavAnim.SLIDE
                        screen = if (isLearnFlow) Screen.LEARN_MENU else Screen.GAME_MENU
                    },
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
                    onSelectHelpPreset = { helpPreset = it },
                    availableHelpPresets = if (isLearnFlow) {
                        listOf(HelpPreset.GUIDED)
                    } else {
                        listOf(HelpPreset.TRAINING, HelpPreset.CHALLENGE)
                    }
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
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.GAME_MENU },
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

            Screen.GUIDED_TABLE_PICKER -> MultTablePickerScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.IMPARA_MENU },
                onPickTable = { table ->
                    guidedTable = table
                    navAnim = NavAnim.SLIDE
                    screen = Screen.GUIDED_TABLE_GAME
                }
            )

            Screen.GUIDED_TABLE_GAME -> TabellineGuidateScreen(
                table = guidedTable,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.IMPARA_MENU },
                onRepeat = {},
                onPickAnother = {
                    navAnim = NavAnim.SLIDE
                    screen = Screen.GUIDED_TABLE_PICKER
                },
                onExitToLearnMenu = { navAnim = NavAnim.SLIDE; screen = Screen.IMPARA_MENU }
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
                onBack = {
                    navAnim = NavAnim.SLIDE
                    screen = if (isLearnFlow) Screen.LEARN_MENU else Screen.GAME_MENU
                },
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
                onBack = {
                    navAnim = NavAnim.SLIDE
                    screen = if (isLearnFlow) Screen.LEARN_MENU else Screen.GAME_MENU
                },
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
                onBack = {
                    if (isLearnFlow) {
                        navAnim = NavAnim.SLIDE
                        screen = Screen.LEARN_MENU
                    } else {
                        navAnim = NavAnim.EXPAND
                        screen = Screen.GAME_MENU
                    }
                },
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
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_MENU },
                lastResults = lastHomeworkResults,
                onStartHomework = { configs ->
                    homeworkQueue = buildExerciseQueue(configs)
                    homeworkReturnScreen = Screen.HOMEWORK_BUILDER
                    runningHomeworkId = null
                    runningHomeworkFromCode = false
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
                },
                onSaveHomework = { savedHomework ->
                    savedHomeworkScope.launch {
                        savedHomeworkRepository.save(savedHomework)
                        savedHomeworks = savedHomeworkRepository.getAll()
                    }
                },
                onSaveHomeworkCode = { entry ->
                    homeworkCodeScope.launch {
                        homeworkCodeRepository.save(entry)
                        homeworkCodes = homeworkCodeRepository.getAll()
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
                    activeGuidedLessonId = null
                    runningHomeworkId = null
                    runningHomeworkFromCode = false
                    navAnim = NavAnim.SLIDE
                    screen = homeworkReturnScreen
                },
                onSaveReport = { report ->
                    reportScope.launch {
                        reportStorage.saveReport(report)
                        homeworkReports = listOf(report) + homeworkReports
                    }
                },
                onFinishHomework = { results ->
                    lastHomeworkResults = results
                    if (homeworkReturnScreen == Screen.GUIDED_PATH) {
                        activeGuidedLessonId?.let { lessonId ->
                            if (!completedGuidedLessonIds.contains(lessonId)) {
                                completedGuidedLessonIds += lessonId
                            }
                        }
                    }
                    activeGuidedLessonId = null
                    runningHomeworkId = null
                    runningHomeworkFromCode = false
                    homeworkQueue = emptyList()
                    navAnim = NavAnim.SLIDE
                    screen = if (homeworkReturnScreen == Screen.GUIDED_PATH) {
                        Screen.GUIDED_PATH
                    } else {
                        Screen.HOME
                    }
                },
                homeworkId = runningHomeworkId,
                onHomeworkCompleted = { homeworkId ->
                    savedHomeworks = savedHomeworks.filterNot { it.id == homeworkId }
                    savedHomeworkScope.launch {
                        savedHomeworkRepository.delete(homeworkId)
                        savedHomeworks = savedHomeworkRepository.getAll()
                    }
                },
                startedFromCode = runningHomeworkFromCode
            )

            Screen.HOMEWORK_REPORTS -> HomeworkReportsScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOME },
                reports = homeworkReports,
                onDeleteReports = { reportsToDelete ->
                    reportScope.launch {
                        reportStorage.deleteReports(reportsToDelete)
                        homeworkReports = reportStorage.loadReports()
                    }
                }
            )

            Screen.ASSIGNED_HOMEWORKS -> AssignedHomeworksScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onBack = { navAnim = NavAnim.SLIDE; screen = Screen.HOMEWORK_MENU },
                savedHomeworks = savedHomeworks,
                homeworkCodes = homeworkCodes,
                onStartHomework = { savedHomework ->
                    homeworkQueue = buildExerciseQueue(savedHomework.tasks)
                    homeworkReturnScreen = Screen.ASSIGNED_HOMEWORKS
                    runningHomeworkId = savedHomework.id
                    runningHomeworkFromCode = false
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
                },
                onStartHomeworkFromCode = { payload ->
                    homeworkQueue = buildExerciseQueue(payload.tasks)
                    homeworkReturnScreen = Screen.ASSIGNED_HOMEWORKS
                    runningHomeworkId = null
                    runningHomeworkFromCode = true
                    navAnim = NavAnim.SLIDE
                    screen = Screen.HOMEWORK_RUNNER
                }
            )
        }
        }
    }
    }
}

private fun Screen.supportsExpandedRail(): Boolean = when (this) {
    Screen.HOME,
    Screen.IMPARA_MENU,
    Screen.GUIDED_PATH,
    Screen.ASSIGNED_HOMEWORKS,
    Screen.HOMEWORK_REPORTS -> true
    else -> false
}

@Composable
private fun AdaptiveRootLayout(
    windowType: WindowType,
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    if (windowType == WindowType.Expanded && screen.supportsExpandedRail()) {
        Row(Modifier.fillMaxSize()) {
            NavigationRailPanel(current = screen, onNavigate = onNavigate)
            Divider(Modifier.fillMaxHeight().width(1.dp))
            Box(Modifier.weight(1f)) { content() }
        }
    } else {
        content()
    }
}

@Composable
private fun NavigationRailPanel(
    current: Screen,
    onNavigate: (Screen) -> Unit
) {
    val items = listOf(
        Screen.HOME to "Home",
        Screen.IMPARA_MENU to "Impara",
        Screen.GUIDED_PATH to "Percorso",
        Screen.ASSIGNED_HOMEWORKS to "Archivio",
        Screen.HOMEWORK_REPORTS to "Report"
    )
    NavigationRail {
        Spacer(Modifier.height(12.dp))
        items.forEach { (screen, label) ->
            NavigationRailItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Text(label.take(1), fontWeight = FontWeight.Bold) },
                label = { Text(label) }
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
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    onOpenGameMenu: () -> Unit,
    onOpenLearnMenu: () -> Unit,
    onOpenHomeworkMenu: () -> Unit,
    onOpenReports: () -> Unit,
    savedHomeworks: List<SavedHomework>,
    fx: SoundFx
) {
    val bonusTapWindowMs = 600L
    val bonusTapTarget = 10
    var bonusTapCount by remember { mutableStateOf(0) }
    var lastBonusTapMs by remember { mutableStateOf(0L) }
    var showBonusMenu by remember { mutableStateOf(false) }
    var activeBonusGame by remember { mutableStateOf<BonusHomeGame?>(null) }

    fun onLogoTapped() {
        val now = System.currentTimeMillis()
        bonusTapCount = if (now - lastBonusTapMs <= bonusTapWindowMs) {
            bonusTapCount + 1
        } else {
            1
        }
        lastBonusTapMs = now

        if (bonusTapCount >= bonusTapTarget) {
            bonusTapCount = 0
            lastBonusTapMs = 0L
            showBonusMenu = true
        }
    }

    if (activeBonusGame == null) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenH = maxHeight
            val screenW = maxWidth
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val sizing = menuSizing(maxHeight = screenH, maxWidth = screenW)
                val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()
                val buttons = listOf(
                    MenuButtonData(
                        title = "Gioco libero",
                        baseColor = Color(0xFF8B5CF6),
                        iconText = "🎮",
                        onClick = onOpenGameMenu
                    ),
                    MenuButtonData(
                        title = "Impara",
                        baseColor = Color(0xFFF59E0B),
                        iconText = "📖",
                        onClick = onOpenLearnMenu
                    ),
                    MenuButtonData(
                        title = "Compiti",
                        baseColor = Color(0xFF2ECC71),
                        iconText = "📘",
                        onClick = onOpenHomeworkMenu
                    ),
                    MenuButtonData(
                        title = "Report",
                        baseColor = Color(0xFF3498DB),
                        iconText = "📊",
                        onClick = onOpenReports
                    )
                )

                val animationsReady = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { animationsReady.value = true }

                val density = LocalDensity.current
                var headerHeightPx by remember { mutableStateOf(0) }
                var logoHeightPx by remember { mutableStateOf(0) }

                val headerLogoSpacing = 20.dp
                val logoButtonSpacing = 20.dp
                val headerLogoSpacingPx = with(density) { headerLogoSpacing.toPx() }
                val logoButtonSpacingPx = with(density) { logoButtonSpacing.toPx() }
                val logoTopPx = headerHeightPx + headerLogoSpacingPx
                val firstButtonTopPx = logoTopPx + logoHeightPx + logoButtonSpacingPx
                val headerHeightDp = with(density) { headerHeightPx.toDp() }
                val buttonCount = buttons.size
                val totalButtonsHeight =
                    (sizing.buttonHeight * buttonCount.toFloat()) +
                        (sizing.buttonSpacing * (buttonCount - 1).coerceAtLeast(0).toFloat())
                val isLandscape = screenW > screenH
                val logoMinHeight = if (isLandscape) 96.dp else 0.dp
                val logoWidthFraction = if (isLandscape) 0.55f else 1.00f
                val logoMaxHeight =
                    (screenH - headerHeightDp - headerLogoSpacing - logoButtonSpacing - totalButtonsHeight)
                        .coerceAtLeast(logoMinHeight)

                val firstButtonOffset = with(density) { firstButtonTopPx.toDp() }
                val logoOffset = with(density) { logoTopPx.toDp() }
                val offsetPx = with(density) { sizing.offset.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .onSizeChanged { headerHeightPx = it.height }
                    ) {
                        MenuHeader(
                            soundEnabled = soundEnabled,
                            onToggleSound = onToggleSound,
                            onOpenLeaderboard = onOpenLeaderboard
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = logoOffset)
                            .onSizeChanged { logoHeightPx = it.height }
                            .pointerInput(Unit) {
                                detectTapGestures { onLogoTapped() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoPainter != null) {
                            Image(
                                painter = logoPainter,
                                contentDescription = "Math Kids",
                                modifier = Modifier
                                    .fillMaxWidth(logoWidthFraction)
                                    .heightIn(min = logoMinHeight, max = logoMaxHeight),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Logo mancante",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = firstButtonOffset),
                        verticalArrangement = Arrangement.spacedBy(sizing.buttonSpacing)
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

                            Box {
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
                                    height = sizing.buttonHeight,
                                    textSize = sizing.buttonTextSize,
                                    modifier = Modifier.graphicsLayer {
                                        this.alpha = alpha
                                        translationY = offsetY
                                    }
                                )

                                if (data.title == "Compiti") {
                                    HomeworkBadge(
                                        count = savedHomeworks.size,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-6).dp, y = (-6).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showBonusMenu) {
            AlertDialog(
                onDismissRequest = { showBonusMenu = false },
                properties = DialogProperties(dismissOnClickOutside = false),
                title = { Text("Modalità bonus") },
                text = { Text("Scegli il gioco bonus") },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showBonusMenu = false
                                    activeBonusGame = BonusHomeGame.Balloons
                                }
                            ) { Text("Palloncini 🎈") }
                            Button(
                                onClick = {
                                    showBonusMenu = false
                                    activeBonusGame = BonusHomeGame.Stars
                                }
                            ) { Text("Stelle ⭐") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showBonusMenu = false }) { Text("Annulla") }
                    }
                }
            )
        }
    } else {
        when (activeBonusGame) {
            BonusHomeGame.Balloons -> BonusBalloonGame(
                onScoreSaved = { onOpenLeaderboardFromBonus(LeaderboardTab.BALLOONS) },
                onFinish = { activeBonusGame = null }
            )
            BonusHomeGame.Stars -> FallingStarsGame(
                soundEnabled = soundEnabled,
                fx = fx,
                onScoreSaved = { onOpenLeaderboardFromBonus(LeaderboardTab.STARS) },
                onFinish = { activeBonusGame = null }
            )
            null -> Unit
        }
    }
}

@Composable
private fun LearnMenuKids(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    onStartAddition: () -> Unit,
    onStartSubtraction: () -> Unit,
    onStartMultiplication: () -> Unit,
    onStartDivision: () -> Unit,
    onStartGuidedTables: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val screenW = maxWidth
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val sizing = menuSizing(maxHeight = screenH, maxWidth = screenW)
            val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()
            val buttons = listOf(
                MenuButtonData(
                    title = "Addizioni guidate",
                    baseColor = Color(0xFFE74C3C),
                    iconText = "＋",
                    onClick = onStartAddition
                ),
                MenuButtonData(
                    title = "Sottrazioni guidate",
                    baseColor = Color(0xFF2ECC71),
                    iconText = "−",
                    onClick = onStartSubtraction
                ),
                MenuButtonData(
                    title = "Moltiplicazioni guidate",
                    baseColor = Color(0xFF8B5CF6),
                    iconText = "××",
                    onClick = onStartMultiplication
                ),
                MenuButtonData(
                    title = "Divisioni guidate",
                    baseColor = Color(0xFF3498DB),
                    iconText = "÷",
                    onClick = onStartDivision
                ),
                MenuButtonData(
                    title = "Tabelline guidate",
                    baseColor = Color(0xFFF59E0B),
                    iconText = "×",
                    onClick = onStartGuidedTables
                )
            )

            val animationsReady = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animationsReady.value = true }

            val density = LocalDensity.current
            var headerHeightPx by remember { mutableStateOf(0) }
            var logoHeightPx by remember { mutableStateOf(0) }

            val headerLogoSpacing = 20.dp
            val logoButtonSpacing = 20.dp
            val headerLogoSpacingPx = with(density) { headerLogoSpacing.toPx() }
            val logoButtonSpacingPx = with(density) { logoButtonSpacing.toPx() }
            val logoTopPx = headerHeightPx + headerLogoSpacingPx
            val firstButtonTopPx = logoTopPx + logoHeightPx + logoButtonSpacingPx
            val headerHeightDp = with(density) { headerHeightPx.toDp() }
            val buttonCount = buttons.size
            val totalButtonsHeight =
                (sizing.buttonHeight * buttonCount.toFloat()) +
                    (sizing.buttonSpacing * (buttonCount - 1).coerceAtLeast(0).toFloat())
            val isLandscape = screenW > screenH
            val logoMinHeight = if (isLandscape) 96.dp else 0.dp
            val logoWidthFraction = if (isLandscape) 0.55f else 1.00f
            val logoMaxHeight =
                (screenH - headerHeightDp - headerLogoSpacing - logoButtonSpacing - totalButtonsHeight)
                    .coerceAtLeast(logoMinHeight)

            val firstButtonOffset = with(density) { firstButtonTopPx.toDp() }
            val logoOffset = with(density) { logoTopPx.toDp() }
            val offsetPx = with(density) { sizing.offset.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .onSizeChanged { headerHeightPx = it.height }
                ) {
                    MenuHeader(
                        soundEnabled = soundEnabled,
                        onToggleSound = onToggleSound,
                        onOpenLeaderboard = onOpenLeaderboard,
                        onBack = onBack
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = logoOffset)
                        .onSizeChanged { logoHeightPx = it.height },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "Math Kids",
                            modifier = Modifier
                                .fillMaxWidth(logoWidthFraction)
                                .heightIn(min = logoMinHeight, max = logoMaxHeight),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Logo mancante",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = firstButtonOffset),
                    verticalArrangement = Arrangement.spacedBy(sizing.buttonSpacing)
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
                            height = sizing.buttonHeight,
                            textSize = sizing.buttonTextSize,
                            modifier = Modifier.graphicsLayer {
                                this.alpha = alpha
                                translationY = offsetY
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameMenuKids(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    onPickDigitsFor: (GameMode) -> Unit, // ADD/SUB
    onPlayDirect: (GameMode) -> Unit     // MULT/DIV/MONEY/MULT_HARD
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val screenW = maxWidth
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val sizing = menuSizing(maxHeight = screenH, maxWidth = screenW)
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

            val density = LocalDensity.current
            var headerHeightPx by remember { mutableStateOf(0) }
            var logoHeightPx by remember { mutableStateOf(0) }

            val headerLogoSpacing = 20.dp
            val logoButtonSpacing = 20.dp
            val headerLogoSpacingPx = with(density) { headerLogoSpacing.toPx() }
            val logoButtonSpacingPx = with(density) { logoButtonSpacing.toPx() }
            val logoTopPx = headerHeightPx + headerLogoSpacingPx
            val firstButtonTopPx = logoTopPx + logoHeightPx + logoButtonSpacingPx
            val headerHeightDp = with(density) { headerHeightPx.toDp() }
            val buttonCount = buttons.size
            val totalButtonsHeight =
                (sizing.buttonHeight * buttonCount.toFloat()) +
                    (sizing.buttonSpacing * (buttonCount - 1).coerceAtLeast(0).toFloat())
            val isLandscape = screenW > screenH
            val logoMinHeight = if (isLandscape) 96.dp else 0.dp
            val logoWidthFraction = if (isLandscape) 0.55f else 1.00f
            val logoMaxHeight =
                (screenH - headerHeightDp - headerLogoSpacing - logoButtonSpacing - totalButtonsHeight)
                    .coerceAtLeast(logoMinHeight)

            val firstButtonOffset = with(density) { firstButtonTopPx.toDp() }
            val logoOffset = with(density) { logoTopPx.toDp() }
            val offsetPx = with(density) { sizing.offset.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .onSizeChanged { headerHeightPx = it.height }
                ) {
                    MenuHeader(
                        soundEnabled = soundEnabled,
                        onToggleSound = onToggleSound,
                        onOpenLeaderboard = onOpenLeaderboard,
                        onBack = onBack
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = logoOffset)
                        .onSizeChanged { logoHeightPx = it.height },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "Math Kids",
                            modifier = Modifier
                                .fillMaxWidth(logoWidthFraction)
                                .heightIn(min = logoMinHeight, max = logoMaxHeight),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Logo mancante",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = firstButtonOffset),
                    verticalArrangement = Arrangement.spacedBy(sizing.buttonSpacing)
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
                            height = sizing.buttonHeight,
                            textSize = sizing.buttonTextSize,
                            modifier = Modifier.graphicsLayer {
                                this.alpha = alpha
                                translationY = offsetY
                            }
                        )
                    }

                    BonusPill(
                        text = "Completa gli esercizi e vinci il BONUS! 🎈",
                        fontSize = sizing.bonusTextSize
                    )
                }
            }
        }
    }
}

@Composable
fun AssignedHomeworkOverlay(
    count: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldPulse = count > 0
    val pulseScale = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "assignedHomeworkPulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "assignedHomeworkOverlayScale"
        )
        scale
    } else {
        1f
    }
    val pulseAlpha = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "assignedHomeworkPulseAlpha")
        val alpha by transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "assignedHomeworkOverlayAlpha"
        )
        alpha
    } else {
        1f
    }

    Surface(
        modifier = modifier
            .height(28.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                alpha = pulseAlpha
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpen() },
        color = if (count > 0) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        },
        contentColor = if (count > 0) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Compiti",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            if (count > 0) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = count.coerceAtMost(99).toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkMenu(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    onOpenHomeworkBuilder: () -> Unit,
    onOpenAssignedHomeworks: () -> Unit,
    onOpenHomeworkCodes: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val screenW = maxWidth
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val sizing = menuSizing(maxHeight = screenH, maxWidth = screenW)
            val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()
            val buttons = listOf(
                MenuButtonData(
                    title = "Fai i compiti",
                    baseColor = Color(0xFF22C55E),
                    iconText = "✅",
                    onClick = onOpenAssignedHomeworks
                ),
                MenuButtonData(
                    title = "Genera compiti",
                    baseColor = Color(0xFFE74C3C),
                    iconText = "📝",
                    onClick = onOpenHomeworkBuilder
                ),
                MenuButtonData(
                    title = "Lista codici compito (BETA)",
                    baseColor = Color(0xFF6366F1),
                    iconText = "🔑",
                    onClick = onOpenHomeworkCodes
                )
            )

            val animationsReady = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animationsReady.value = true }

            val density = LocalDensity.current
            var headerHeightPx by remember { mutableStateOf(0) }
            var logoHeightPx by remember { mutableStateOf(0) }

            val headerLogoSpacing = 20.dp
            val logoButtonSpacing = 20.dp
            val headerLogoSpacingPx = with(density) { headerLogoSpacing.toPx() }
            val logoButtonSpacingPx = with(density) { logoButtonSpacing.toPx() }
            val logoTopPx = headerHeightPx + headerLogoSpacingPx
            val firstButtonTopPx = logoTopPx + logoHeightPx + logoButtonSpacingPx
            val headerHeightDp = with(density) { headerHeightPx.toDp() }
            val buttonCount = buttons.size
            val totalButtonsHeight =
                (sizing.buttonHeight * buttonCount.toFloat()) +
                    (sizing.buttonSpacing * (buttonCount - 1).coerceAtLeast(0).toFloat())
            val isLandscape = screenW > screenH
            val logoMinHeight = if (isLandscape) 96.dp else 0.dp
            val logoWidthFraction = if (isLandscape) 0.55f else 1.00f
            val logoMaxHeight =
                (screenH - headerHeightDp - headerLogoSpacing - logoButtonSpacing - totalButtonsHeight)
                    .coerceAtLeast(logoMinHeight)

            val firstButtonOffset = with(density) { firstButtonTopPx.toDp() }
            val logoOffset = with(density) { logoTopPx.toDp() }
            val offsetPx = with(density) { sizing.offset.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.00f))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .onSizeChanged { headerHeightPx = it.height }
                ) {
                    MenuHeader(
                        soundEnabled = soundEnabled,
                        onToggleSound = onToggleSound,
                        onOpenLeaderboard = onOpenLeaderboard,
                        onBack = onBack
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = logoOffset)
                        .onSizeChanged { logoHeightPx = it.height },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "Math Kids",
                            modifier = Modifier
                                .fillMaxWidth(logoWidthFraction)
                                .heightIn(min = logoMinHeight, max = logoMaxHeight),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Logo mancante",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = firstButtonOffset),
                    verticalArrangement = Arrangement.spacedBy(sizing.buttonSpacing)
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
                            height = sizing.buttonHeight,
                            textSize = sizing.buttonTextSize,
                            modifier = Modifier.graphicsLayer {
                                this.alpha = alpha
                                translationY = offsetY
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LearnMenuScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    onOpenGuidedPath: () -> Unit,
    onOpenAutonomousLearn: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sizing = menuSizing(maxHeight = screenHeight, maxWidth = LocalConfiguration.current.screenWidthDp.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MenuHeader(
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onOpenLeaderboard = onOpenLeaderboard,
                onBack = onBack
            )

            SeaGlassPanel(title = stringResource(R.string.learn_title)) {
                KidsMenuButton(
                    title = stringResource(R.string.learn_guided_path),
                    baseColor = Color(0xFF2563EB),
                    icon = {
                        Text(
                            "🧭",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    },
                    onClick = onOpenGuidedPath,
                    height = sizing.buttonHeight,
                    textSize = sizing.buttonTextSize
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "segui un percorso passo dopo passo, dal facile al difficile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF374151)
                )

                Spacer(Modifier.height(14.dp))

                KidsMenuButton(
                    title = stringResource(R.string.learn_autonomous),
                    baseColor = Color(0xFF0EA5E9),
                    icon = {
                        Text(
                            "🎯",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    },
                    onClick = onOpenAutonomousLearn,
                    height = sizing.buttonHeight,
                    textSize = sizing.buttonTextSize
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "scegli liberamente cosa allenare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF374151)
                )
            }
        }
    }
}

@Composable
private fun GuidedPathScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    selectedFilter: LearnFilter,
    onFilterChange: (LearnFilter) -> Unit,
    gradeFilter: GradeLevel?,
    onGradeFilterChange: (GradeLevel?) -> Unit,
    lessons: List<LessonSpec>,
    onStartLesson: (LessonSpec) -> Unit,
    completedLessonIds: Set<String>
) {
    var selectedLessonDetails by remember { mutableStateOf<LessonSpec?>(null) }
    val windowType = LocalWindowType.current

    val opFilterButtons = listOf(
        LearnFilter.ALL to R.string.learn_filter_all,
        LearnFilter.ADDITIONS to R.string.learn_filter_additions,
        LearnFilter.SUBTRACTIONS to R.string.learn_filter_subtractions,
        LearnFilter.MULTIPLICATIONS to R.string.learn_filter_multiplications,
        LearnFilter.DIVISIONS to R.string.learn_filter_divisions,
        LearnFilter.TIMES_TABLES to R.string.learn_filter_times_tables
    )
    val grouped = lessons
        .filter { lesson ->
            val operationMatch = when (selectedFilter) {
                LearnFilter.ALL -> true
                LearnFilter.ADDITIONS -> lesson.operation == OperationType.ADD
                LearnFilter.SUBTRACTIONS -> lesson.operation == OperationType.SUB
                LearnFilter.MULTIPLICATIONS -> lesson.operation == OperationType.MUL
                LearnFilter.DIVISIONS -> lesson.operation == OperationType.DIV
                LearnFilter.TIMES_TABLES -> false
            }
            val gradeMatch = gradeFilter == null || lesson.grade == gradeFilter
            operationMatch && gradeMatch && lesson.kind == LessonKind.BASE
        }
        .groupBy { it.grade to it.operation }
        .toList()
        .sortedWith(compareBy({ it.first.first.ordinal }, { it.first.second.ordinal }))

    Scaffold(
        topBar = {
            GameHeader(
                title = stringResource(R.string.learn_guided_path),
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = onOpenLeaderboard,
                useStatusBarsPadding = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = gradeFilter == null, onClick = { onGradeFilterChange(null) }, label = { Text("Tutte") })
                GradeLevel.entries.forEach { grade ->
                    FilterChip(
                        selected = gradeFilter == grade,
                        onClick = { onGradeFilterChange(grade) },
                        label = { Text(grade.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opFilterButtons.forEach { (filter, labelRes) ->
                    FilterChip(selected = selectedFilter == filter, onClick = { onFilterChange(filter) }, label = { Text(stringResource(labelRes)) })
                }
            }

            if (grouped.isEmpty()) {
                SeaGlassPanel { Text("Lezione non configurata") }
            } else if (windowType == WindowType.Compact) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(grouped, key = { "${it.first.first}-${it.first.second}" }) { (key, groupLessons) ->
                        val accent = gradeAccent(key.first)
                        LessonGroupCard(
                            grade = key.first,
                            operation = key.second,
                            accent = accent,
                            lessons = groupLessons.sortedBy { it.levelIndex },
                            onStartLesson = onStartLesson,
                            onViewLesson = { selectedLessonDetails = it },
                            completedLessonIds = completedLessonIds
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(grouped.size) { idx ->
                        val (key, groupLessons) = grouped[idx]
                        val accent = gradeAccent(key.first)
                        LessonGroupCard(
                            grade = key.first,
                            operation = key.second,
                            accent = accent,
                            lessons = groupLessons.sortedBy { it.levelIndex },
                            onStartLesson = onStartLesson,
                            onViewLesson = { selectedLessonDetails = it },
                            completedLessonIds = completedLessonIds
                        )
                    }
                }
            }
        }
    }

    selectedLessonDetails?.let { lesson ->
        AlertDialog(
            onDismissRequest = { selectedLessonDetails = null },
            title = { Text(lesson.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lesson.description)
                    Text("Totale esercizi: ${lesson.fixedExercises.size.takeIf { it > 0 } ?: 10}")
                    Text("Classe: ${lesson.grade.name} • ${operationLabel(lesson.operation)} • Livello ${lesson.levelIndex}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLessonDetails = null }) { Text("Chiudi") }
            }
        )
    }
}

@Composable
fun LessonGroupCard(
    grade: GradeLevel,
    operation: OperationType,
    accent: Color,
    lessons: List<LessonSpec>,
    onStartLesson: (LessonSpec) -> Unit,
    onViewLesson: (LessonSpec) -> Unit,
    completedLessonIds: Set<String>
) {
    val ordered = lessons.sortedBy { it.levelIndex }
    SeaGlassPanel(modifier = Modifier.border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(26.dp))) {
        Text("Classe ${grade.name} – ${operationLabel(operation)} (Percorso base)", fontWeight = FontWeight.Bold)
        Text("5 lezioni in ordine – completa la 1 per sbloccare la 2 (unlock verrà poi)")
        Row(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(48.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(5) { idx ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accent.copy(alpha = 0.18f), CircleShape)
                            .border(1.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                    if (idx < 4) {
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(44.dp)
                                .background(accent.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                ordered.forEachIndexed { idx, lesson ->
                    LessonRow(lesson, idx, accent, isCompleted = completedLessonIds.contains(lesson.id), onStart = { onStartLesson(lesson) }, onView = { onViewLesson(lesson) })
                    if (idx < ordered.lastIndex) HorizontalDivider(color = accent.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
fun LessonRow(
    lesson: LessonSpec,
    indexInGroup: Int,
    accent: Color,
    isCompleted: Boolean,
    onStart: () -> Unit,
    onView: () -> Unit
) {
    val activeHelps = lesson.helpPreset.activeHelpLabels()
    val shape = when (indexInGroup) {
        0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        4 -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Surface(color = accent.copy(alpha = 0.08f), shape = shape, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(lesson.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (isCompleted) {
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            "Completata",
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(lesson.description, style = MaterialTheme.typography.bodySmall)
            if (activeHelps.isNotEmpty()) {
                Text(
                    text = "Aiuti attivi: ${activeHelps.joinToString(separator = " • ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Avvia") }
                OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) { Text("Dettagli") }
            }
        }
    }
}

private fun operationLabel(operation: OperationType): String = when (operation) {
    OperationType.ADD -> "Addizioni"
    OperationType.SUB -> "Sottrazioni"
    OperationType.MUL -> "Moltiplicazioni"
    OperationType.DIV -> "Divisioni"
}

private fun HelpSettings?.activeHelpLabels(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        if (hintsEnabled) add("Suggerimenti")
        if (highlightsEnabled) add("Evidenziazioni")
        if (allowSolution) add("Soluzione disponibile")
        if (autoCheck) add("Controllo automatico")
    }
}

private fun gradeAccent(grade: GradeLevel): Color = when (grade) {
    GradeLevel.I -> Color(0xFF16A34A)
    GradeLevel.II -> Color(0xFF0EA5E9)
    GradeLevel.III -> Color(0xFFF59E0B)
    GradeLevel.IV -> Color(0xFF7C3AED)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeworkCodesScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    codes: List<HomeworkCodeEntry>,
    onDeleteCode: (HomeworkCodeEntry) -> Unit,
    onShareCode: (HomeworkCodeEntry) -> Unit,
    onStartHomeworkFromCode: (HomeworkCodePayload) -> Unit
) {
    val formatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val sortedCodes = remember(codes) { codes.sortedByDescending { it.createdAt } }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selectedCode = sortedCodes.firstOrNull { it.id == selectedId }

    Scaffold(
        topBar = {
            GameHeader(
                title = "Codici compito",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = onOpenLeaderboard,
                useStatusBarsPadding = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SeaGlassPanel {
                if (sortedCodes.isEmpty()) {
                    Text("Nessun codice salvato.")
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        sortedCodes.forEach { code ->
                            val isSelected = code.id == selectedId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            selectedId = if (isSelected) null else code.id
                                        }
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(code.title, fontWeight = FontWeight.Bold)
                                Text(
                                    formatHomeworkCodePreview(code.code),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                )
                                Text(
                                    buildHomeworkCodeDescription(code.tasks),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Text(
                                    formatter.format(Date(code.createdAt)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selectedCode != null) {
                SeaGlassPanel(title = "Azioni codice") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Codice selezionato: ${selectedCode.title}")
                        Button(
                            onClick = {
                                onStartHomeworkFromCode(
                                    HomeworkCodePayload(
                                        id = selectedCode.id,
                                        createdAt = selectedCode.createdAt,
                                        tasks = selectedCode.tasks
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedCode.tasks.isNotEmpty()
                        ) {
                            Text("Avvio compito")
                        }
                        Button(
                            onClick = { onShareCode(selectedCode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Condividi codice")
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Elimina codice", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && selectedCode != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare questo codice?") },
            text = { Text("Il codice selezionato verrà eliminato definitivamente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val code = selectedCode
                        showDeleteConfirm = false
                        selectedId = null
                        onDeleteCode(code)
                    }
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun HomeworkBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    val transition = rememberInfiniteTransition(label = "homeworkBadgePulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeworkBadgeScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeworkBadgeAlpha"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(Color(0xFFE11D48), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.coerceAtMost(99).toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MenuHeader(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .headerOffsetFromStatusBar(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            SmallCircleButton("⬅") { onBack() }
        } else {
            Spacer(Modifier.size(36.dp))
        }
        TopActionsPill {
            SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
            SmallCircleButton("🏆") { onOpenLeaderboard() }
            SmallCircleButton("❓") { showHelpDialog = true }
        }
    }

    if (showHelpDialog) {
        HelpInfoDialog(onDismiss = { showHelpDialog = false })
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
            .widthIn(max = 560.dp)
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

private data class MenuSizing(
    val buttonHeight: Dp,
    val buttonSpacing: Dp,
    val buttonTextSize: TextUnit,
    val bonusTextSize: TextUnit,
    val offset: Dp,
    val contentTopInset: Dp
)

private fun menuSizing(maxHeight: Dp, maxWidth: Dp): MenuSizing {
    val isLandscape = maxWidth > maxHeight
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
    val buttonHeight = if (isLandscape) baseButtonHeight * 0.68f else baseButtonHeight * 0.75f

    val buttonSpacing = when (sizeProfile) {
        MenuSizeProfile.Small -> 10.dp
        MenuSizeProfile.Normal -> 12.dp
        MenuSizeProfile.Large -> 16.dp
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
    val offset = when (sizeProfile) {
        MenuSizeProfile.Small -> 12.dp
        MenuSizeProfile.Normal -> 16.dp
        MenuSizeProfile.Large -> 18.dp
    }
    val contentTopInset = when (sizeProfile) {
        MenuSizeProfile.Small -> 20.dp
        MenuSizeProfile.Normal -> 22.dp
        MenuSizeProfile.Large -> 24.dp
    }

    return MenuSizing(
        buttonHeight = buttonHeight,
        buttonSpacing = buttonSpacing,
        buttonTextSize = buttonTextSize,
        bonusTextSize = bonusTextSize,
        offset = offset,
        contentTopInset = contentTopInset
    )
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
        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .headerOffsetFromStatusBar(),
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
        Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GameHeader(
            title = "Scegli la tabellina",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {},
            useStatusBarsPadding = false
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

    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .headerOffsetFromStatusBar(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
private fun HelpInfoDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informazioni") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📘 SEZIONE 1 — COME USARE L’APP", fontWeight = FontWeight.Bold)
                Text("L’app Matematica per Bambini è pensata per aiutare i bambini a imparare la matematica in modo graduale, guidato e divertente.")
                Text("La particolarità dell'app che non si trova in altri prodotti è che le operazioni di calcolo sono guidate passo-passo con riporti, prestiti, resti.")
                Text("🎮 Gioco libero", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Il bambino può scegliere il tipo di operazione (addizioni, sottrazioni, moltiplicazioni, divisioni).")
                    Text("Sono disponibili diverse modalità (allenamento, sfida) differenti per la quantità di aiuto disponibile.")
                    Text("Ideale per esercitarsi in autonomia.")
                    Text("Al termine di un certo numero di operazioni corrette si accede ad un gioco bonus con una classifica mostrata nella Leaderboard.")
                }
                Text("📖 Impara (modalità guidata)", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tutti i giochi sono sempre guidati.")
                    Text("L’app spiega passo dopo passo come risolvere le operazioni con una serie di test di aiuto che spiegano le operazioni da compiere.")
                    Text("Gli errori fanno parte dell’apprendimento e non vengono penalizzati.")
                }
                Text("📝 SEZIONE 2 — COMPITI", fontWeight = FontWeight.Bold)
                Text("👨‍👩‍👧‍👦 Genera compiti (genitore/insegnante)", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Il genitore può creare una serie di esercizi personalizzati con numeri random oppure con operazioni determinate, salvarli per farli svolgere in un secondo momento oppure far partire immediatamente il compito da svolgere.")
                    Text("I compiti salvati possono essere descritti per tipologia e tempistica i modo da indicare al bambino quando svolgerli.")
                    Text("Se un compito è presente nell'area compiti verrà visualizzata un'icona rossa sul tasto compiti nella Home.")
                    Text("Insegnanti o genitori possono generare un compito e salvarlo con un  codice alfanumerico per  avviare lo stesso esercizio su un altro dispositivo e/o eventualmente inviarlo a un gruppo condividendo il codice.")
                }
                Text("🧒 Fai i compiti (bambino)", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Il bambino svolge i compiti assegnati.")
                    Text("Al termine deve inserire il proprio nome.")
                    Text("Viene generato un report dettagliato dei risultati.")
                    Text("Al termine si ritorna automaticamente alla schermata principale.")
                }
                Text("🔑 SEZIONE 3 — CODICI COMPITO", fontWeight = FontWeight.Bold)
                Text("🔐 Genera codice compito", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Il codice rappresenta solo la configurazione del compito.")
                    Text("Non contiene nomi, dati personali o informazioni identificative.")
                    Text("Dopo la generazione, è possibile assegnare un titolo al codice.")
                    Text("I codici vengono salvati solo localmente sul dispositivo.")
                }
                Text("📥 Ripristina codice compito", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Il bambino può inserire un codice compito ricevuto.")
                    Text("Prima di iniziare viene mostrata una breve descrizione del compito.")
                    Text("Il compito viene ricostruito esattamente come creato dal genitore/Insegnante.")
                    Text("Al termine viene richiesto il nome del bambino e creato un report.")
                }
                Text("📊 SEZIONE 4 — REPORT", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("I report mostrano i risultati degli esercizi svolti, gli eventuali errori, gli aiuti utilizzati.")
                    Text("Servono per aiutare genitori e insegnanti a capire i progressi del bambino.")
                    Text("I report rimangono solo sul dispositivo.")
                    Text("Possono essere eliminati in qualsiasi momento.")
                }
                Text("🔐 SEZIONE 5 — PRIVACY POLICY / GDPR", fontWeight = FontWeight.Bold)
                Text("Questa applicazione è progettata nel rispetto totale della privacy dei bambini e delle famiglie.")
                Text("✅ Cosa FA l’app", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Funziona completamente offline.")
                    Text("Salva dati solo localmente sul dispositivo.")
                    Text("Consente l’inserimento del nome del bambino solo per visualizzare i report.")
                }
                Text("❌ Cosa NON FA l’app", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NON raccoglie dati personali sensibili.")
                    Text("NON richiede registrazione o login.")
                    Text("NON utilizza internet.")
                    Text("NON invia dati a server esterni.")
                    Text("NON usa pubblicità.")
                    Text("NON effettua tracciamento o profilazione.")
                    Text("NON condivide dati con terze parti.")
                }
                Text("👶 Minori", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("L’app è pensata per l’uso da parte di bambini.")
                    Text("Tutte le funzionalità sono utilizzabili senza creare account.")
                    Text("I dati inseriti possono essere cancellati in qualsiasi momento dall’app.")
                }
                Text("⚖️ Conformità GDPR", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("I dati sono minimizzati e limitati allo stretto necessario.")
                    Text("Nessun dato lascia il dispositivo.")
                    Text("Nessun dato viene utilizzato per fini commerciali.")
                }
                Text("Privacy Policy – sezione “Backup e dati”", fontWeight = FontWeight.Bold)
                Text("Backup dei dati")
                Text("L’app utilizza il sistema di backup automatico di Android per consentire il ripristino dei dati educativi in caso di cambio dispositivo o reinstallazione.")
                Text("Vengono inclusi nel backup esclusivamente:")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("i compiti assegnati")
                    Text("i report dei compiti svolti")
                    Text("i codici compito salvati")
                }
                Text("Non vengono salvati né ripristinati:")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("dati temporanei o di gioco in corso")
                    Text("progressi parziali di una singola attività")
                    Text("cache o informazioni tecniche")
                }
                Text("I dati di backup non sono accessibili a terze parti e rimangono associati all’account Google dell’utente secondo le regole del sistema Android.")
                Text("© COPYRIGHT", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MateMatt")
                    Text("© 2025 – Davide Sironi")
                    Text("Tutti i diritti riservati.")
                    Text("L’app e i suoi contenuti sono protetti da copyright.")
                    Text("È vietata la copia, la distribuzione o l’uso commerciale senza autorizzazione.")
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
