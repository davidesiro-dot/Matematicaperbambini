package com.example.matematicaperbambini

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.FileProvider
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch


@Composable
fun HomeworkBuilderScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    lastResults: List<ExerciseResult>,
    onStartHomework: (List<HomeworkTaskConfig>) -> Unit,
    onSaveHomework: (SavedHomework) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf("") }
    var pendingConfigs by remember { mutableStateOf<List<HomeworkTaskConfig>>(emptyList()) }
    var additionEnabled by remember { mutableStateOf(false) }
    var additionDigitsInput by remember { mutableStateOf("2") }
    var additionExercisesCountInput by remember { mutableStateOf("5") }
    var additionRepeatsInput by remember { mutableStateOf("1") }
    var additionHintsEnabled by remember { mutableStateOf(false) }
    var additionHighlightsEnabled by remember { mutableStateOf(false) }
    var additionAllowSolution by remember { mutableStateOf(false) }
    var additionAutoCheck by remember { mutableStateOf(false) }
    val additionManualOps = remember { mutableStateListOf<ManualOp.AB>() }
    var additionManualSelected by remember { mutableStateOf(false) }
    var additionManualAInput by remember { mutableStateOf("") }
    var additionManualBInput by remember { mutableStateOf("") }
    val additionSource = if (additionManualSelected) {
        ExerciseSourceConfig.Manual(additionManualOps.toList())
    } else {
        ExerciseSourceConfig.Random
    }

    var subtractionEnabled by remember { mutableStateOf(false) }
    var subtractionDigitsInput by remember { mutableStateOf("2") }
    var subtractionExercisesCountInput by remember { mutableStateOf("5") }
    var subtractionRepeatsInput by remember { mutableStateOf("1") }
    var subtractionHintsEnabled by remember { mutableStateOf(false) }
    var subtractionHighlightsEnabled by remember { mutableStateOf(false) }
    var subtractionAllowSolution by remember { mutableStateOf(false) }
    var subtractionAutoCheck by remember { mutableStateOf(false) }
    val subtractionManualOps = remember { mutableStateListOf<ManualOp.AB>() }
    var subtractionManualSelected by remember { mutableStateOf(false) }
    var subtractionManualAInput by remember { mutableStateOf("") }
    var subtractionManualBInput by remember { mutableStateOf("") }
    val subtractionSource = if (subtractionManualSelected) {
        ExerciseSourceConfig.Manual(subtractionManualOps.toList())
    } else {
        ExerciseSourceConfig.Random
    }

    var tableEnabled by remember { mutableStateOf(false) }
    var tableMode by remember { mutableStateOf(TabellineMode.CLASSIC) }
    val tableSelectedTables = remember { mutableStateListOf<Int>() }
    var tableRepeatsInput by remember { mutableStateOf("1") }
    var tableHintsEnabled by remember { mutableStateOf(false) }
    var tableHighlightsEnabled by remember { mutableStateOf(false) }
    var tableAllowSolution by remember { mutableStateOf(false) }
    var tableAutoCheck by remember { mutableStateOf(false) }
    val toggleTable: (Int) -> Unit = { table ->
        if (tableSelectedTables.contains(table)) {
            tableSelectedTables.remove(table)
        } else {
            tableSelectedTables.add(table)
        }
    }

    var divisionEnabled by remember { mutableStateOf(false) }
    var divisionDigitsInput by remember { mutableStateOf("2") }
    var divisionDivisorDigitsInput by remember { mutableStateOf("1") }
    var divisionExercisesCountInput by remember { mutableStateOf("4") }
    var divisionRepeatsInput by remember { mutableStateOf("1") }
    var divisionHintsEnabled by remember { mutableStateOf(false) }
    var divisionHighlightsEnabled by remember { mutableStateOf(false) }
    var divisionAllowSolution by remember { mutableStateOf(false) }
    var divisionAutoCheck by remember { mutableStateOf(false) }
    var divisionShowCellHelper by remember { mutableStateOf(false) }
    val divisionManualOps = remember { mutableStateListOf<ManualOp.AB>() }
    var divisionManualSelected by remember { mutableStateOf(false) }
    var divisionManualDividendInput by remember { mutableStateOf("") }
    var divisionManualDivisorInput by remember { mutableStateOf("") }
    val divisionManualDividendRange = 2..999
    val divisionManualDivisorRange = 2..99
    val divisionSource = if (divisionManualSelected) {
        ExerciseSourceConfig.Manual(divisionManualOps.toList())
    } else {
        ExerciseSourceConfig.Random
    }

    var hardEnabled by remember { mutableStateOf(false) }
    var hardMaxAInput by remember { mutableStateOf("2") }
    var hardMaxBInput by remember { mutableStateOf("1") }
    var hardExercisesCountInput by remember { mutableStateOf("4") }
    var hardRepeatsInput by remember { mutableStateOf("1") }
    var hardHintsEnabled by remember { mutableStateOf(false) }
    var hardHighlightsEnabled by remember { mutableStateOf(false) }
    var hardAllowSolution by remember { mutableStateOf(false) }
    var hardAutoCheck by remember { mutableStateOf(false) }
    val hardManualOps = remember { mutableStateListOf<ManualOp.AB>() }
    var hardManualSelected by remember { mutableStateOf(false) }
    var hardManualAInput by remember { mutableStateOf("") }
    var hardManualBInput by remember { mutableStateOf("") }
    val hardManualARange = 10..999
    val hardManualBRange = 1..99
    val hardSource = if (hardManualSelected) {
        ExerciseSourceConfig.Manual(hardManualOps.toList())
    } else {
        ExerciseSourceConfig.Random
    }

    Scaffold(
        topBar = {
            GameHeader(
                title = "Compiti (genitore)",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (lastResults.isNotEmpty()) {
                item {
                    val perfectCount = lastResults.count { it.outcome() == ExerciseOutcome.PERFECT }
                    val withErrorsCount = lastResults.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
                    SeaGlassPanel(title = "Ultimo report") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Esercizi: ${lastResults.size}", fontWeight = FontWeight.Bold)
                            Text("Corretto: $perfectCount")
                            Text("Completato con errori: $withErrorsCount")
                            Text("Da ripassare: ${lastResults.size - perfectCount - withErrorsCount}")
                        }
                    }
                }
            }

            item {
                SeaGlassPanel(title = "Seleziona giochi") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GameToggleRow(
                            title = "Addizioni",
                            subtitle = "Somme con numeri configurabili",
                            checked = additionEnabled,
                            onCheckedChange = { additionEnabled = it }
                        )
                        GameToggleRow(
                            title = "Sottrazioni",
                            subtitle = "Differenze con numeri configurabili",
                            checked = subtractionEnabled,
                            onCheckedChange = { subtractionEnabled = it }
                        )
                        GameToggleRow(
                            title = "Tabellina",
                            subtitle = "Esercizi su una tabellina specifica",
                            checked = tableEnabled,
                            onCheckedChange = { tableEnabled = it }
                        )
                        GameToggleRow(
                            title = "Divisioni passo-passo",
                            subtitle = "Divisioni con resto",
                            checked = divisionEnabled,
                            onCheckedChange = { divisionEnabled = it }
                        )
                        GameToggleRow(
                            title = "Moltiplicazioni difficili",
                            subtitle = "Moltiplicazioni a due cifre",
                            checked = hardEnabled,
                            onCheckedChange = { hardEnabled = it }
                        )
                    }
                }
            }

        if (additionEnabled) {
            item {
                SeaGlassPanel(title = "Configurazione addizioni") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RandomSourceRow(
                            source = additionSource,
                            onSourceChange = { additionManualSelected = false }
                        )
                        OutlinedTextField(
                            value = additionDigitsInput,
                            onValueChange = {
                                additionDigitsInput = it.filter { char -> char in '1'..'3' }.take(1)
                            },
                            label = { Text("Difficoltà (cifre)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AmountConfigRow(
                            exercisesCountInput = additionExercisesCountInput,
                            repeatsInput = additionRepeatsInput,
                            onExercisesCountChange = { additionExercisesCountInput = it },
                            onRepeatsChange = { additionRepeatsInput = it }
                        )
                        ManualExerciseSection(
                            source = additionSource,
                            onSourceChange = { additionManualSelected = it is ExerciseSourceConfig.Manual },
                            manualOps = additionManualOps,
                            manualAInput = additionManualAInput,
                            manualBInput = additionManualBInput,
                            onManualAChange = { additionManualAInput = it },
                            onManualBChange = { additionManualBInput = it },
                            opLabel = "A",
                            opLabelB = "B",
                            infoText = "Limiti inserimento: 0-999 per A e B (max 3 cifre)",
                            onAddManual = {
                                val a = additionManualAInput.toIntOrNull()
                                val b = additionManualBInput.toIntOrNull()
                                if (a != null && b != null) {
                                    additionManualOps += ManualOp.AB(a, b)
                                    additionManualAInput = ""
                                    additionManualBInput = ""
                                }
                            },
                            onRemoveManual = { index ->
                                additionManualOps.removeAt(index)
                            },
                            manualItemText = { op -> "• ${op.a} + ${op.b}" }
                        )
                        HelpConfigSection(
                            hintsEnabled = additionHintsEnabled,
                            highlightsEnabled = additionHighlightsEnabled,
                            allowSolution = additionAllowSolution,
                            autoCheck = additionAutoCheck,
                            onHintsChange = { additionHintsEnabled = it },
                            onHighlightsChange = { additionHighlightsEnabled = it },
                            onAllowSolutionChange = { additionAllowSolution = it },
                            onAutoCheckChange = { additionAutoCheck = it }
                        )
                    }
                }
            }
        }

        if (subtractionEnabled) {
            item {
                SeaGlassPanel(title = "Configurazione sottrazioni") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RandomSourceRow(
                            source = subtractionSource,
                            onSourceChange = { subtractionManualSelected = false }
                        )
                        OutlinedTextField(
                            value = subtractionDigitsInput,
                            onValueChange = {
                                subtractionDigitsInput = it.filter { char -> char in '1'..'3' }.take(1)
                            },
                            label = { Text("Difficoltà (cifre)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AmountConfigRow(
                            exercisesCountInput = subtractionExercisesCountInput,
                            repeatsInput = subtractionRepeatsInput,
                            onExercisesCountChange = { subtractionExercisesCountInput = it },
                            onRepeatsChange = { subtractionRepeatsInput = it }
                        )
                        ManualExerciseSection(
                            source = subtractionSource,
                            onSourceChange = { subtractionManualSelected = it is ExerciseSourceConfig.Manual },
                            manualOps = subtractionManualOps,
                            manualAInput = subtractionManualAInput,
                            manualBInput = subtractionManualBInput,
                            onManualAChange = { subtractionManualAInput = it },
                            onManualBChange = { subtractionManualBInput = it },
                            opLabel = "A",
                            opLabelB = "B",
                            infoText = "Limiti inserimento: A 1-999, B 1-999 (A > B)",
                            onAddManual = {
                                val a = subtractionManualAInput.toIntOrNull()
                                val b = subtractionManualBInput.toIntOrNull()
                                if (a != null && b != null && a in 1..999 && b in 1..999 && b < a) {
                                    subtractionManualOps += ManualOp.AB(a, b)
                                    subtractionManualAInput = ""
                                    subtractionManualBInput = ""
                                }
                            },
                            onRemoveManual = { index ->
                                subtractionManualOps.removeAt(index)
                            },
                            manualItemText = { op -> "• ${op.a} - ${op.b}" }
                        )
                        HelpConfigSection(
                            hintsEnabled = subtractionHintsEnabled,
                            highlightsEnabled = subtractionHighlightsEnabled,
                            allowSolution = subtractionAllowSolution,
                            autoCheck = subtractionAutoCheck,
                            onHintsChange = { subtractionHintsEnabled = it },
                            onHighlightsChange = { subtractionHighlightsEnabled = it },
                            onAllowSolutionChange = { subtractionAllowSolution = it },
                            onAutoCheckChange = { subtractionAutoCheck = it }
                        )
                    }
                }
            }
        }

        if (tableEnabled) {
            item {
                SeaGlassPanel(title = "Configurazione tabellina") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Seleziona la/le tabellina/e", fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..5).forEach { table ->
                                    val selected = tableSelectedTables.contains(table)
                                    Box(modifier = Modifier.weight(1f)) {
                                        SourceChip(
                                            label = table.toString(),
                                            selected = selected,
                                            onClick = { toggleTable(table) }
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (6..10).forEach { table ->
                                    val selected = tableSelectedTables.contains(table)
                                    Box(modifier = Modifier.weight(1f)) {
                                        SourceChip(
                                            label = table.toString(),
                                            selected = selected,
                                            onClick = { toggleTable(table) }
                                        )
                                    }
                                }
                            }
                            if (tableSelectedTables.isNotEmpty()) {
                                Text(
                                    "Tabelline scelte: ${tableSelectedTables.sorted().joinToString()}",
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    "Nessuna selezionata.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text("Modalità tabelline", fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SourceChip(
                                        label = "Classica",
                                        selected = tableMode == TabellineMode.CLASSIC,
                                        onClick = { tableMode = TabellineMode.CLASSIC }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SourceChip(
                                        label = "Buchi",
                                        selected = tableMode == TabellineMode.GAPS,
                                        onClick = { tableMode = TabellineMode.GAPS }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SourceChip(
                                        label = "Scelta multipla",
                                        selected = tableMode == TabellineMode.MULTIPLE_CHOICE,
                                        onClick = { tableMode = TabellineMode.MULTIPLE_CHOICE }
                                    )
                                }
                            }
                        }
                        HelpConfigSection(
                            hintsEnabled = tableHintsEnabled,
                            highlightsEnabled = tableHighlightsEnabled,
                            allowSolution = tableAllowSolution,
                            autoCheck = tableAutoCheck,
                            onHintsChange = { tableHintsEnabled = it },
                            onHighlightsChange = { tableHighlightsEnabled = it },
                            onAllowSolutionChange = { tableAllowSolution = it },
                            onAutoCheckChange = { tableAutoCheck = it }
                        )
                        OutlinedTextField(
                            value = tableRepeatsInput,
                            onValueChange = { value ->
                                val digits = value.filter(Char::isDigit).take(2)
                                val parsed = digits.toIntOrNull()
                                tableRepeatsInput = when {
                                    parsed == null -> ""
                                    parsed > 20 -> "20"
                                    else -> digits
                                }
                            },
                            label = { Text("Ripetizioni tabelline (max 20)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (divisionEnabled) {
            item {
                SeaGlassPanel(title = "Configurazione divisioni") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RandomSourceRow(
                            source = divisionSource,
                            onSourceChange = { divisionManualSelected = false }
                        )
                        OutlinedTextField(
                            value = divisionDigitsInput,
                            onValueChange = { divisionDigitsInput = it.filter { char -> char in '1'..'3' }.take(1) },
                            label = { Text("Cifre dividendo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = divisionDivisorDigitsInput,
                            onValueChange = {
                                divisionDivisorDigitsInput = it.filter { char -> char in '1'..'3' }.take(1)
                            },
                            label = { Text("Cifre divisore") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AmountConfigRow(
                            exercisesCountInput = divisionExercisesCountInput,
                            repeatsInput = divisionRepeatsInput,
                            onExercisesCountChange = { divisionExercisesCountInput = it },
                            onRepeatsChange = { divisionRepeatsInput = it }
                        )
                        ManualExerciseSection(
                            source = divisionSource,
                            onSourceChange = { divisionManualSelected = it is ExerciseSourceConfig.Manual },
                            manualOps = divisionManualOps,
                            manualAInput = divisionManualDividendInput,
                            manualBInput = divisionManualDivisorInput,
                            onManualAChange = { divisionManualDividendInput = it },
                            onManualBChange = { divisionManualDivisorInput = it },
                            opLabel = "Dividendo",
                            opLabelB = "Divisore",
                            maxDigitsA = divisionManualDividendRange.last.toString().length,
                            maxDigitsB = divisionManualDivisorRange.last.toString().length,
                            infoText = "Limiti inserimento: ${divisionManualDividendRange.first}-" +
                                "${divisionManualDividendRange.last} ÷ ${divisionManualDivisorRange.first}-" +
                                "${divisionManualDivisorRange.last} (dividendo > divisore)",
                            isAddEnabled = run {
                                val dividend = divisionManualDividendInput.toIntOrNull()
                                val divisor = divisionManualDivisorInput.toIntOrNull()
                                dividend in divisionManualDividendRange &&
                                        divisor in divisionManualDivisorRange &&
                                        (dividend ?: 0) > (divisor ?: 0)
                            },
                            manualError = run {
                                val dividend = divisionManualDividendInput.toIntOrNull()
                                val divisor = divisionManualDivisorInput.toIntOrNull()
                                val dividendBlank = divisionManualDividendInput.isBlank()
                                val divisorBlank = divisionManualDivisorInput.isBlank()
                                when {
                                    dividendBlank && divisorBlank -> null
                                    dividend == null || dividend !in divisionManualDividendRange -> {
                                        "Dividendo valido: ${divisionManualDividendRange.first}-${divisionManualDividendRange.last}."
                                    }
                                    divisor == null || divisor !in divisionManualDivisorRange -> {
                                        "Divisore valido: ${divisionManualDivisorRange.first}-${divisionManualDivisorRange.last}."
                                    }
                                    (dividend ?: 0) <= (divisor ?: 0) -> {
                                        "Il dividendo deve essere maggiore del divisore."
                                    }
                                    else -> null
                                }
                            },
                            onAddManual = {
                                val a = divisionManualDividendInput.toIntOrNull()
                                val b = divisionManualDivisorInput.toIntOrNull()
                                if (a != null && b != null && a in divisionManualDividendRange &&
                                    b in divisionManualDivisorRange && a > b
                                ) {
                                    divisionManualOps += ManualOp.AB(a, b)
                                    divisionManualDividendInput = ""
                                    divisionManualDivisorInput = ""
                                }
                            },
                            onRemoveManual = { index ->
                                divisionManualOps.removeAt(index)
                            },
                            manualItemText = { op -> "• ${op.a} ÷ ${op.b}" }
                        )
                        HelpConfigSection(
                            hintsEnabled = divisionHintsEnabled,
                            highlightsEnabled = divisionHighlightsEnabled,
                            allowSolution = divisionAllowSolution,
                            autoCheck = divisionAutoCheck,
                            showCellHelper = divisionShowCellHelper,
                            onHintsChange = { divisionHintsEnabled = it },
                            onHighlightsChange = { divisionHighlightsEnabled = it },
                            onAllowSolutionChange = { divisionAllowSolution = it },
                            onAutoCheckChange = { divisionAutoCheck = it },
                            onShowCellHelperChange = { divisionShowCellHelper = it }
                        )
                    }
                }
            }
        }

        if (hardEnabled) {
            item {
                SeaGlassPanel(title = "Configurazione moltiplicazioni difficili") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RandomSourceRow(
                            source = hardSource,
                            onSourceChange = { hardManualSelected = false }
                        )
                        OutlinedTextField(
                            value = hardMaxAInput,
                            onValueChange = {
                                val nextValue = it.filter { char -> char in '2'..'3' }.take(1)
                                hardMaxAInput = nextValue
                            },
                            label = { Text("Cifre Moltiplicando") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hardMaxBInput,
                            onValueChange = {
                                val allowed = '1'..'2'
                                hardMaxBInput = it.filter { char -> char in allowed }.take(1)
                            },
                            label = { Text("Cifre Moltiplicatore") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AmountConfigRow(
                            exercisesCountInput = hardExercisesCountInput,
                            repeatsInput = hardRepeatsInput,
                            onExercisesCountChange = { hardExercisesCountInput = it },
                            onRepeatsChange = { hardRepeatsInput = it }
                        )
                        ManualExerciseSection(
                            source = hardSource,
                            onSourceChange = { hardManualSelected = it is ExerciseSourceConfig.Manual },
                            manualOps = hardManualOps,
                            manualAInput = hardManualAInput,
                            manualBInput = hardManualBInput,
                            onManualAChange = { hardManualAInput = it },
                            onManualBChange = { hardManualBInput = it },
                            opLabel = "Moltiplicando",
                            opLabelB = "Moltiplicatore",
                            maxDigitsA = hardManualARange.last.toString().length,
                            maxDigitsB = hardManualBRange.last.toString().length,
                            infoText = "Limiti inserimento: ${hardManualARange.first}-${hardManualARange.last} " +
                                "× ${hardManualBRange.first}-${hardManualBRange.last}",
                            manualError = run {
                                val a = hardManualAInput.toIntOrNull()
                                val b = hardManualBInput.toIntOrNull()
                                val aBlank = hardManualAInput.isBlank()
                                val bBlank = hardManualBInput.isBlank()
                                when {
                                    aBlank && bBlank -> null
                                    a == null || a !in hardManualARange -> {
                                        "Moltiplicando valido: ${hardManualARange.first}-${hardManualARange.last}."
                                    }
                                    b == null || b !in hardManualBRange -> {
                                        "Moltiplicatore valido: ${hardManualBRange.first}-${hardManualBRange.last}."
                                    }
                                    else -> null
                                }
                            },
                            onAddManual = {
                                val a = hardManualAInput.toIntOrNull()
                                val b = hardManualBInput.toIntOrNull()
                                if (
                                    a != null &&
                                    b != null &&
                                    a in hardManualARange &&
                                    b in hardManualBRange
                                ) {
                                    hardManualOps += ManualOp.AB(a, b)
                                    hardManualAInput = ""
                                    hardManualBInput = ""
                                }
                            },
                            onRemoveManual = { index ->
                                hardManualOps.removeAt(index)
                            },
                            manualItemText = { op -> "• ${op.a} × ${op.b}" }
                        )
                        HelpConfigSection(
                            hintsEnabled = hardHintsEnabled,
                            highlightsEnabled = hardHighlightsEnabled,
                            allowSolution = hardAllowSolution,
                            autoCheck = hardAutoCheck,
                            onHintsChange = { hardHintsEnabled = it },
                            onHighlightsChange = { hardHighlightsEnabled = it },
                            onAllowSolutionChange = { hardAllowSolution = it },
                            onAutoCheckChange = { hardAutoCheck = it }
                        )
                    }
                }
            }
        }

        val anyEnabled = additionEnabled || subtractionEnabled || tableEnabled || divisionEnabled || hardEnabled
        fun buildConfigs(): List<HomeworkTaskConfig> = buildList {
            if (additionEnabled) {
                val digits = additionDigitsInput.toIntOrNull()?.coerceIn(1, 3) ?: 2
                val exercisesCount = additionExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 5
                val repeats = additionRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                val helpSettings = HelpSettings(
                    hintsEnabled = additionHintsEnabled,
                    highlightsEnabled = additionHighlightsEnabled,
                    allowSolution = additionAllowSolution,
                    autoCheck = additionAutoCheck,
                    showCellHelper = false
                )
                // why: manualOps is the single source of truth for manual exercises.
                val sourceConfig = if (additionManualOps.isNotEmpty()) {
                    ExerciseSourceConfig.Manual(additionManualOps.toList())
                } else {
                    ExerciseSourceConfig.Random
                }
                add(
                    HomeworkTaskConfig(
                        game = GameType.ADDITION,
                        difficulty = DifficultyConfig(digits = digits),
                        helps = helpSettings,
                        source = sourceConfig,
                        amount = AmountConfig(
                            exercisesCount = exercisesCount,
                            repeatsPerExercise = repeats
                        )
                    )
                )
            }
            if (subtractionEnabled) {
                val digits = subtractionDigitsInput.toIntOrNull()?.coerceIn(1, 3) ?: 2
                val exercisesCount = subtractionExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 5
                val repeats = subtractionRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                val helpSettings = HelpSettings(
                    hintsEnabled = subtractionHintsEnabled,
                    highlightsEnabled = subtractionHighlightsEnabled,
                    allowSolution = subtractionAllowSolution,
                    autoCheck = subtractionAutoCheck,
                    showCellHelper = false
                )
                val sourceConfig = if (subtractionManualOps.isNotEmpty()) {
                    ExerciseSourceConfig.Manual(subtractionManualOps.toList())
                } else {
                    ExerciseSourceConfig.Random
                }
                add(
                    HomeworkTaskConfig(
                        game = GameType.SUBTRACTION,
                        difficulty = DifficultyConfig(digits = digits),
                        helps = helpSettings,
                        source = sourceConfig,
                        amount = AmountConfig(
                            exercisesCount = exercisesCount,
                            repeatsPerExercise = repeats
                        )
                    )
                )
            }
            if (tableEnabled) {
                val tables = tableSelectedTables.sorted()
                val tableGame = when (tableMode) {
                    TabellineMode.CLASSIC -> GameType.MULTIPLICATION_TABLE
                    TabellineMode.GAPS -> GameType.MULTIPLICATION_GAPS
                    TabellineMode.REVERSE -> GameType.MULTIPLICATION_REVERSE
                    TabellineMode.MULTIPLE_CHOICE -> GameType.MULTIPLICATION_MULTIPLE_CHOICE
                    TabellineMode.MIXED -> GameType.MULTIPLICATION_MIXED
                }
                val repeats = tableRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                val sourceConfig = if (tables.isEmpty()) {
                    ExerciseSourceConfig.Random
                } else {
                    ExerciseSourceConfig.Manual(tables.map { ManualOp.Table(it) })
                }
                val helpSettings = HelpSettings(
                    hintsEnabled = tableHintsEnabled,
                    highlightsEnabled = tableHighlightsEnabled,
                    allowSolution = tableAllowSolution,
                    autoCheck = tableAutoCheck,
                    showCellHelper = false
                )
                add(
                    HomeworkTaskConfig(
                        game = tableGame,
                        difficulty = DifficultyConfig(tables = tables.takeIf { it.isNotEmpty() }),
                        helps = helpSettings,
                        source = sourceConfig,
                        amount = AmountConfig(
                            exercisesCount = tables.size.coerceAtLeast(1),
                            repeatsPerExercise = repeats
                        )
                    )
                )
            }
            if (divisionEnabled) {
                val digits = divisionDigitsInput.toIntOrNull()?.coerceIn(1, 3) ?: 2
                val divisorDigits = divisionDivisorDigitsInput.toIntOrNull()?.coerceIn(1, 3) ?: 1
                val exercisesCount = divisionExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 4
                val repeats = divisionRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                val helpSettings = HelpSettings(
                    hintsEnabled = divisionHintsEnabled,
                    highlightsEnabled = divisionHighlightsEnabled,
                    allowSolution = divisionAllowSolution,
                    autoCheck = divisionAutoCheck,
                    showCellHelper = divisionShowCellHelper
                )
                val sourceConfig = if (divisionManualOps.isNotEmpty()) {
                    ExerciseSourceConfig.Manual(divisionManualOps.toList())
                } else {
                    ExerciseSourceConfig.Random
                }
                add(
                    HomeworkTaskConfig(
                        game = GameType.DIVISION_STEP,
                        difficulty = DifficultyConfig(digits = digits, divisorDigits = divisorDigits),
                        helps = helpSettings,
                        source = sourceConfig,
                        amount = AmountConfig(
                            exercisesCount = exercisesCount,
                            repeatsPerExercise = repeats
                        )
                    )
                )
            }
            if (hardEnabled) {
                val multiplicandDigits = hardMaxAInput.toIntOrNull()?.coerceIn(2, 3) ?: 2
                val multiplierDigits = hardMaxBInput.toIntOrNull()?.coerceIn(1, 2) ?: 1
                val exercisesCount = hardExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 4
                val repeats = hardRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                val helpSettings = HelpSettings(
                    hintsEnabled = hardHintsEnabled,
                    highlightsEnabled = hardHighlightsEnabled,
                    allowSolution = hardAllowSolution,
                    autoCheck = hardAutoCheck,
                    showCellHelper = false
                )
                val sourceConfig = if (hardManualOps.isNotEmpty()) {
                    ExerciseSourceConfig.Manual(hardManualOps.toList())
                } else {
                    ExerciseSourceConfig.Random
                }
                add(
                    HomeworkTaskConfig(
                        game = GameType.MULTIPLICATION_HARD,
                        difficulty = DifficultyConfig(maxA = multiplicandDigits, maxB = multiplierDigits),
                        helps = helpSettings,
                        source = sourceConfig,
                        amount = AmountConfig(
                            exercisesCount = exercisesCount,
                            repeatsPerExercise = repeats
                        )
                    )
                )
            }
        }

        item {
            Button(
                onClick = {
                    val configs = buildConfigs()
                    onStartHomework(configs)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = anyEnabled
            ) { Text("Avvia Compito") }
        }

        item {
            Button(
                onClick = {
                    pendingConfigs = buildConfigs()
                    showSaveDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = anyEnabled
            ) { Text("Salva compito per dopo") }
        }
    }

    if (showSaveDialog) {
        val trimmedName = saveNameInput.trim()
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveNameInput = ""
            },
            title = { Text("Nome compito") },
            text = {
                OutlinedTextField(
                    value = saveNameInput,
                    onValueChange = { input ->
                        if (input.length <= 32) {
                            saveNameInput = input
                        }
                    },
                    placeholder = { Text("Es. Ripasso divisioni") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val savedHomework = SavedHomework(
                            id = UUID.randomUUID().toString(),
                            name = trimmedName,
                            createdAt = System.currentTimeMillis(),
                            tasks = pendingConfigs
                        )
                        onSaveHomework(savedHomework)
                        showSaveDialog = false
                        saveNameInput = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Compito salvato")
                        }
                    },
                    enabled = trimmedName.isNotEmpty()
                ) { Text("Salva") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        saveNameInput = ""
                    }
                ) { Text("Annulla") }
            }
        )
    }
}

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssignedHomeworksScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    savedHomeworks: List<SavedHomework>,
    onStartHomework: (SavedHomework) -> Unit
) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val sortedHomeworks = remember(savedHomeworks) { savedHomeworks.sortedByDescending { it.createdAt } }

    Scaffold(
        topBar = {
            GameHeader(
                title = "Compiti assegnati",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = {}
            )
        }
    ) { padding ->
        if (sortedHomeworks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SeaGlassPanel {
                    Text(
                        "Nessun compito assegnato",
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Chiedi al genitore di creare un compito",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(sortedHomeworks, key = { _, item -> item.id }) { _, homework ->
                    val totalExercises = homework.tasks.sumOf {
                        it.amount.exercisesCount * it.amount.repeatsPerExercise
                    }
                    SeaGlassPanel(
                        title = homework.name,
                        modifier = Modifier.combinedClickable(onClick = { onStartHomework(homework) })
                    ) {
                        Text(
                            formatter.format(Date(homework.createdAt)),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            if (totalExercises > 0) {
                                "Esercizi previsti: $totalExercises"
                            } else {
                                "Task: ${homework.tasks.size}"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { onStartHomework(homework) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Inizia") }
                    }
                }
            }
        }
    }
}

@Composable
fun GameToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HelpConfigSection(
    hintsEnabled: Boolean,
    highlightsEnabled: Boolean,
    allowSolution: Boolean,
    autoCheck: Boolean,
    showCellHelper: Boolean? = null,
    onHintsChange: (Boolean) -> Unit,
    onHighlightsChange: (Boolean) -> Unit,
    onAllowSolutionChange: (Boolean) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    onShowCellHelperChange: ((Boolean) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aiuti", fontWeight = FontWeight.Bold)
        HelpToggleRow("Suggerimenti", hintsEnabled) { onHintsChange(it) }
        if (showCellHelper != null && onShowCellHelperChange != null) {
            HelpToggleRow("Aiuto nella cella", showCellHelper) { onShowCellHelperChange(it) }
        }
        HelpToggleRow("Evidenziazioni", highlightsEnabled) { onHighlightsChange(it) }
        HelpToggleRow("Soluzione", allowSolution) { onAllowSolutionChange(it) }
        HelpToggleRow("Auto-check", autoCheck) { onAutoCheckChange(it) }
    }
}

@Composable
fun AmountConfigRow(
    exercisesCountInput: String,
    repeatsInput: String,
    onExercisesCountChange: (String) -> Unit,
    onRepeatsChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = exercisesCountInput,
            onValueChange = { onExercisesCountChange(it.filter(Char::isDigit).take(3)) },
            label = { Text("Quantità") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = repeatsInput,
            onValueChange = { onRepeatsChange(it.filter(Char::isDigit).take(2)) },
            label = { Text("Ripetizioni") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RandomSourceRow(
    source: ExerciseSourceConfig,
    onSourceChange: (ExerciseSourceConfig) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            SourceChip(
                label = "Random",
                selected = source is ExerciseSourceConfig.Random,
                onClick = { onSourceChange(ExerciseSourceConfig.Random) }
            )
        }
    }
}

@Composable
fun ManualExerciseSection(
    source: ExerciseSourceConfig,
    onSourceChange: (ExerciseSourceConfig) -> Unit,
    manualOps: List<ManualOp.AB>,
    manualAInput: String,
    manualBInput: String,
    onManualAChange: (String) -> Unit,
    onManualBChange: (String) -> Unit,
    opLabel: String,
    opLabelB: String,
    maxDigitsA: Int = 3,
    maxDigitsB: Int = 3,
    isAddEnabled: Boolean = true,
    manualError: String? = null,
    infoText: String? = null,
    onAddManual: () -> Unit,
    onRemoveManual: (Int) -> Unit,
    manualItemText: (ManualOp.AB) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SourceChip(
                    label = "Manuale",
                    selected = source is ExerciseSourceConfig.Manual,
                    onClick = { onSourceChange(ExerciseSourceConfig.Manual(manualOps.toList())) }
                )
            }
        }
    }

    if (source is ExerciseSourceConfig.Manual) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Operazioni manuali", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualAInput,
                    onValueChange = { onManualAChange(it.filter { ch -> ch.isDigit() }.take(maxDigitsA)) },
                    label = { Text(opLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = manualBInput,
                    onValueChange = { onManualBChange(it.filter { ch -> ch.isDigit() }.take(maxDigitsB)) },
                    label = { Text(opLabelB) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            if (infoText != null) {
                Text(
                    text = infoText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddManual,
                enabled = isAddEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Aggiungi operazione") }

            if (manualError != null) {
                Text(manualError, color = MaterialTheme.colorScheme.error)
            }

            if (manualOps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    manualOps.forEachIndexed { index, op ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(manualItemText(op))
                            TextButton(onClick = { onRemoveManual(index) }) {
                                Text("Rimuovi")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SourceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        colors = colors,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
            )
        }
    }
}

@Composable
fun HomeworkRunnerScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    queue: List<HomeworkExerciseEntry>,
    previousReports: List<HomeworkReport>,
    onExit: (List<ExerciseResult>) -> Unit,
    onSaveReport: (HomeworkReport) -> Unit,
    onFinishHomework: (List<ExerciseResult>) -> Unit,
    homeworkId: String?,
    onHomeworkCompleted: (String) -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<ExerciseResult>() }
    var startAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var completionHandled by remember { mutableStateOf(false) }
    var completionName by remember { mutableStateOf("") }

    LaunchedEffect(index) {
        startAt = System.currentTimeMillis()
    }

    if (index >= queue.size) {
        if (!completionHandled) {
            completionHandled = true
            showCompletionDialog = true
        }

        if (showCompletionDialog) {
            val trimmedName = completionName.trim()
            val nameMissing = trimmedName.isBlank()
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                title = { Text("🎉 Bravo! Hai finito i compiti!") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Prima di andare alla Home, scrivi il tuo nome.")
                        OutlinedTextField(
                            value = completionName,
                            onValueChange = { completionName = it.take(24) },
                            label = { Text("Scrivi il tuo nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (nameMissing) {
                            Text(
                                "Scrivi il tuo nome per salvare il risultato 🙂",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val totalExercises = queue.size
                            val report = HomeworkReport(
                                childName = trimmedName,
                                createdAt = System.currentTimeMillis(),
                                results = results.toList(),
                                interrupted = false,
                                completedExercises = totalExercises,
                                totalExercises = totalExercises
                            )
                            onSaveReport(report)
                            showCompletionDialog = false
                            homeworkId?.let(onHomeworkCompleted)
                            onFinishHomework(results.toList())
                        },
                        enabled = !nameMissing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salva e torna alla Home")
                    }
                }
            )
        }
        return
    }

    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Stai svolgendo un compito") },
            text = { Text("Se esci ora, il compito verrà salvato come interrotto.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        val completedExercises = results.size
                        val totalExercises = queue.size
                        val interrupted = completedExercises < totalExercises
                        if (interrupted) {
                            val defaultName = previousReports.firstOrNull()?.childName?.ifBlank { null }
                                ?: "Senza nome"
                            val report = HomeworkReport(
                                childName = defaultName,
                                createdAt = System.currentTimeMillis(),
                                results = results.toList(),
                                interrupted = true,
                                completedExercises = completedExercises,
                                totalExercises = totalExercises
                            )
                            onSaveReport(report)
                        }
                        onExit(results.toList())
                    }
                ) { Text("Esci") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continua")
                }
            }
        )
    }

    val current = queue[index]
    val remainingExercises = (queue.size - index).coerceAtLeast(0)
    Box(Modifier.fillMaxSize()) {
        HomeworkExerciseGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = { showExitDialog = true },
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            entry = current,
            remainingExercises = remainingExercises,
            totalExercises = queue.size,
            exerciseIndex = index,
            onExerciseFinished = { partial ->
                val endAt = System.currentTimeMillis()
                results += ExerciseResult(
                    instance = current.instance,
                    correct = partial.correct,
                    attempts = partial.attempts,
                    wrongAnswers = partial.wrongAnswers,
                    stepErrors = partial.stepErrors,
                    solutionUsed = partial.solutionUsed,
                    startedAt = startAt,
                    endedAt = endAt
                )
                index += 1
            }
        )
    }
}

@Composable
private fun HomeworkExerciseGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    entry: HomeworkExerciseEntry,
    remainingExercises: Int,
    totalExercises: Int,
    exerciseIndex: Int,
    onExerciseFinished: (ExerciseResultPartial) -> Unit
) {
    val instance = entry.instance
    val helps = entry.helps
    val homeworkBonusLabel = "Esercizi rimanenti: $remainingExercises"
    val homeworkBonusProgress = if (totalExercises > 0) {
        ((totalExercises - remainingExercises).toFloat() / totalExercises.toFloat())
            .coerceIn(0f, 1f)
    } else {
        null
    }

    when (instance.game) {
        GameType.ADDITION -> LongAdditionGame(
            digits = (instance.a?.toString()?.length ?: 2).coerceAtLeast(1),
            startMode = StartMode.MANUAL,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished
        )
        GameType.SUBTRACTION -> {
            val safeInstance = if (instance.a != null && instance.b != null && instance.a < instance.b) {
                instance.copy(a = instance.b, b = instance.a)
            } else {
                instance
            }
            LongSubtractionGame(
                digits = (safeInstance.a?.toString()?.length ?: 2).coerceAtLeast(1),
                startMode = StartMode.MANUAL,
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                fx = fx,
                onBack = onBack,
                onOpenLeaderboard = onOpenLeaderboard,
                onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
                exercise = safeInstance,
                helps = helps,
                onExerciseFinished = onExerciseFinished
            )
        }
        GameType.MULTIPLICATION_MIXED -> TabellineMixedGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            onExerciseFinished = onExerciseFinished,
            helps = helps,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.MULTIPLICATION_TABLE -> MultiplicationTableGame(
            table = instance.table ?: 1,
            startMode = StartMode.MANUAL,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.MULTIPLICATION_GAPS -> TabellineGapsGame(
            table = instance.table ?: 1,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.MULTIPLICATION_REVERSE -> TabellinaReverseGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.MULTIPLICATION_MULTIPLE_CHOICE -> TabellineMultipleChoiceGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.DIVISION_STEP -> DivisionStepGame(
            startMode = StartMode.MANUAL,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished
        )
        GameType.MULTIPLICATION_HARD -> HardMultiplication2x2Game(
            startMode = StartMode.MANUAL,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = helps,
            onExerciseFinished = onExerciseFinished
        )
        else -> HomeworkUnsupportedScreen(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            message = "Questo esercizio non è ancora disponibile nella modalità compiti."
        )
    }
}

@Composable
private fun HomeworkReportScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    results: List<ExerciseResult>,
    totalExercises: Int,
    interrupted: Boolean,
    previousReports: List<HomeworkReport>,
    onSaveReport: (HomeworkReport) -> Unit
) {
    val perfectCount = results.count { it.outcome() == ExerciseOutcome.PERFECT }
    val withErrorsCount = results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
    val total = results.size
    val context = LocalContext.current
    var childName by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(true) }
    var reportSaved by remember { mutableStateOf(false) }
    var currentReport by remember { mutableStateOf<HomeworkReport?>(null) }

    if (showNameDialog && !reportSaved) {
        val trimmedName = childName.trim()
        val nameMissing = trimmedName.isBlank()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Scrivi il tuo nome") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it.take(24) },
                        label = { Text("Scrivi il tuo nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nameMissing) {
                        Text(
                            "Scrivi il tuo nome per salvare il risultato 🙂",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeName = trimmedName
                        val report = HomeworkReport(
                            childName = safeName,
                            createdAt = System.currentTimeMillis(),
                            results = results,
                            interrupted = interrupted,
                            completedExercises = total,
                            totalExercises = totalExercises
                        )
                        onSaveReport(report)
                        currentReport = report
                        reportSaved = true
                        showNameDialog = false
                    },
                    enabled = !nameMissing
                ) { Text("OK") }
            }
        )
    }

    val errorPatterns = remember(results) { analyzeErrorPatterns(results) }
    val suggestions = remember(errorPatterns) { suggestionsForPatterns(errorPatterns) }
    val now = remember { System.currentTimeMillis() }
    val progressInsights = remember(results, previousReports, now) {
        buildProgressInsights(results, previousReports, now)
    }
    val badges = remember(results, previousReports, now) {
        buildEducationalBadges(results, previousReports, now)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameHeader(
            title = "Report Compito",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (interrupted) {
                item {
                    SeaGlassPanel(title = "Avviso") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚠ Compito interrotto prima del completamento")
                            Text("Esercizi completati: $total su $totalExercises")
                        }
                    }
                }
            }

            item {
                SeaGlassPanel(title = "Riepilogo") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Totale esercizi: $total", fontWeight = FontWeight.Bold)
                        Text("Corretto: $perfectCount")
                        Text("Completato con errori: $withErrorsCount")
                        Text("Da ripassare: ${total - perfectCount - withErrorsCount}")
                    }
                }
            }

            if (errorPatterns.isNotEmpty()) {
                item {
                    SeaGlassPanel(title = "🔍 Difficoltà principali") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            errorPatterns.take(3).forEach { pattern ->
                                Text("• ${pattern.category}")
                            }
                        }
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    SeaGlassPanel(title = "💡 Suggerimenti") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            suggestions.take(2).forEach { suggestion ->
                                Text("• $suggestion")
                            }
                        }
                    }
                }
            }

            if (progressInsights.isNotEmpty()) {
                item {
                    SeaGlassPanel(title = "📈 Progressi") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            progressInsights.forEach { insight ->
                                Text(insight)
                            }
                        }
                    }
                }
            }

            if (badges.isNotEmpty()) {
                item {
                    SeaGlassPanel(title = "🏅 Riconoscimenti") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            badges.forEach { badge ->
                                Text("• $badge")
                            }
                        }
                    }
                }
            }

            item {
                SeaGlassPanel(title = "Stampa") {
                    val report = currentReport
                    if (report == null) {
                        Text("Inserisci il nome per abilitare la stampa del report.")
                    } else {
                        Button(
                            onClick = { printHomeworkReport(context, report) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stampa o esporta PDF")
                        }
                    }
                }
            }

            item {
                Text(
                    "Dettaglio esercizi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (results.isEmpty()) {
                item {
                    Text("Nessun risultato disponibile.")
                }
            } else {
                itemsIndexed(results) { idx, result ->
                    val expected = expectedAnswer(result.instance)
                    val outcome = result.outcome()
                    SeaGlassPanel(title = "Esercizio ${idx + 1}") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exerciseLabel(result.instance), fontWeight = FontWeight.Bold)
                            val statusText = outcomeLabel(outcome)
                            Text(statusText)
                            expected?.let { Text("Risposta corretta: $it") }
                            Text("Tentativi: ${result.attempts}")
                            val wrongAnswers = result.wrongAnswers
                            if (wrongAnswers.isNotEmpty()) {
                                Text("Risposte da rivedere: ${wrongAnswers.joinToString()}")
                            }
                            if (result.stepErrors.isNotEmpty()) {
                                Text("Passaggi da rinforzare:")
                                result.stepErrors.forEach { err ->
                                    Text("• ${stepErrorDescription(err)}")
                                }
                            }
                            if (result.solutionUsed) {
                                Text("Soluzione guidata usata")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkReportsScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    reports: List<HomeworkReport>,
    onDeleteReports: (List<HomeworkReport>) -> Unit
) {
    val context = LocalContext.current
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var multiSelectEnabled by remember { mutableStateOf(false) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedReports = remember(reports, selectedKeys) {
        reports.filter {
            "${it.childName}_${it.createdAt}" in selectedKeys
        }
    }

    fun toggleSelection(key: String) {
        val updated =
            if (key in selectedKeys) selectedKeys - key else selectedKeys + key
        selectedKeys = updated
        if (updated.isEmpty()) {
            multiSelectEnabled = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /* ───────── HEADER FISSO ───────── */
        GameHeader(
            title = "Archivio report",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        /* ───────── CONTENUTO SCROLLABILE ───────── */
        LazyColumn(
            modifier = Modifier
                .weight(1f)          // 🔴 QUESTO È IL PEZZO CHIAVE
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            /* ───── LISTA REPORT ───── */
            if (reports.isEmpty()) {
                item {
                    SeaGlassPanel(title = "Nessun report") {
                        Text("Non ci sono report salvati.")
                    }
                }
            } else {
                itemsIndexed(reports) { idx, report ->
                    val key = "${report.childName}_${report.createdAt}"
                    val selected = key in selectedKeys
                    val expanded = expandedKey == key
                    val completedExercises = if (report.totalExercises > 0) {
                        report.completedExercises
                    } else {
                        report.results.size
                    }
                    val plannedTotal = if (report.totalExercises > 0) {
                        report.totalExercises
                    } else {
                        completedExercises
                    }
                    val correct = report.results.count { it.outcome() == ExerciseOutcome.PERFECT }
                    val withErrors = report.results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
                    val wrong = completedExercises - correct - withErrors
                    val durationMillis = report.results.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0) }
                    val errorPatterns = analyzeErrorPatterns(report.results)
                    val solutionUsedCount = report.results.count { it.solutionUsed }
                    val homeworkTypes = report.results.map { it.instance.game.title }.distinct().ifEmpty { listOf("Compito") }

                    SeaGlassPanel(
                        title = if (report.interrupted) {
                            "Report ${idx + 1} – ⚠ Compito interrotto"
                        } else {
                            "Report ${idx + 1}"
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    if (multiSelectEnabled) {
                                        toggleSelection(key)
                                    } else {
                                        expandedKey = if (expanded) null else key
                                    }
                                },
                                onLongClick = {
                                    multiSelectEnabled = true
                                    toggleSelection(key)
                                }
                            )
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selected) {
                                Text("✅ Selezionato", fontWeight = FontWeight.Bold)
                            }
                            if (report.interrupted) {
                                Text("⚠ Compito interrotto", fontWeight = FontWeight.SemiBold)
                                Text("Esercizi completati: $completedExercises su $plannedTotal")
                            }
                            Text("Bambino: ${report.childName}", fontWeight = FontWeight.Bold)
                            Text("Data e ora: ${formatTimestamp(report.createdAt)}")
                            Text("Durata: ${formatDurationMillis(durationMillis)}")
                            Text("Totale esercizi: $completedExercises")
                            Text("Corretti: $correct • Con errori: $withErrors • Da ripassare: $wrong")
                            Text(
                                if (multiSelectEnabled) {
                                    "Tocca per selezionare"
                                } else {
                                    "Tocca per aprire il dettaglio • Tieni premuto per selezionare"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )

                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                Text("Testata sessione", fontWeight = FontWeight.Bold)
                                Text("Bambino: ${report.childName}")
                                Text("Data e ora: ${formatTimestamp(report.createdAt)}")
                                Text("Durata: ${formatDurationMillis(durationMillis)}")
                                Text("Modalità: Compiti")
                                Text("Tipi esercizi: ${homeworkTypes.joinToString(", ")}")

                                Spacer(Modifier.height(8.dp))
                                if (report.interrupted) {
                                    Text("⚠ Compito interrotto prima del completamento", fontWeight = FontWeight.SemiBold)
                                    Text("Esercizi completati: $completedExercises su $plannedTotal")
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text("Riepilogo sessione", fontWeight = FontWeight.Bold)
                                Text("Totale esercizi: $completedExercises")
                                Text("Corretti: $correct")
                                Text("Completati con errori: $withErrors")
                                Text("Da ripassare: $wrong")

                                Spacer(Modifier.height(8.dp))
                                Text("Aiuti usati durante la sessione", fontWeight = FontWeight.Bold)
                                Text("Suggerimenti: non registrati nei report salvati")
                                Text("Evidenziazioni: non registrate nei report salvati")
                                Text("Soluzione guidata: $solutionUsedCount utilizzi")
                                Text("Auto-check: non registrato nei report salvati")

                                Spacer(Modifier.height(8.dp))
                                SeaGlassPanel(title = "Errori della sessione") {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Errori commessi nella sessione", fontWeight = FontWeight.Bold)
                                        if (errorPatterns.isEmpty()) {
                                            Text("Nessun errore rilevato.")
                                        } else {
                                            errorPatterns.forEach { pattern ->
                                                Text("• ${pattern.category} (${pattern.occurrences})")
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("Errori più frequenti", fontWeight = FontWeight.Bold)
                                        if (errorPatterns.isEmpty()) {
                                            Text("Nessun errore frequente rilevato.")
                                        } else {
                                            errorPatterns.take(3).forEach { pattern ->
                                                Text("• ${pattern.category} (${pattern.occurrences})")
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Dettaglio esercizi", fontWeight = FontWeight.Bold)
                                report.results.forEachIndexed { index, result ->
                                    val outcome = outcomeLabel(result.outcome())
                                    val exerciseDuration = formatDurationMillis(result.endedAt - result.startedAt)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Esercizio ${index + 1}", fontWeight = FontWeight.SemiBold)
                                    Text("Tipo di gioco: ${result.instance.game.title}")
                                    Text("Operazione: ${exerciseLabel(result.instance)}")
                                    Text("Esito: $outcome")
                                    Text("Tentativi: ${result.attempts}")
                                    Text("Tempo impiegato: $exerciseDuration")
                                    Text(
                                        if (result.solutionUsed) {
                                            "Soluzione guidata: sì"
                                        } else {
                                            "Soluzione guidata: no"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (reports.isNotEmpty()) {
            SeaGlassPanel(
                title = "Azioni report selezionati",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (selectedReports.isEmpty())
                            "Seleziona uno o più report con una pressione prolungata."
                        else
                            "Report selezionati: ${selectedReports.size}"
                    )

                    Button(
                        onClick = { printHomeworkReports(context, selectedReports) },
                        enabled = selectedReports.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stampa o esporta PDF")
                    }

                    Button(
                        onClick = { shareHomeworkReports(context, selectedReports) },
                        enabled = selectedReports.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Condividi")
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = selectedReports.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Elimina", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare i report selezionati?") },
            text = {
                Text(
                    "Sei sicuro di voler eliminare i report selezionati?\n" +
                        "Questa operazione non può essere annullata."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = selectedReports
                        showDeleteConfirm = false
                        if (toDelete.isNotEmpty()) {
                            onDeleteReports(toDelete)
                        }
                        selectedKeys = emptySet()
                        multiSelectEnabled = false
                        expandedKey = null
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

private fun shareHomeworkReports(context: android.content.Context, reports: List<HomeworkReport>) {
    if (reports.isEmpty()) return
    val pdfFile = createHomeworkReportPdf(context, reports) ?: return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )
    val subject = if (reports.size == 1) "Report compiti" else "Report compiti (${reports.size})"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Condividi report")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}



private data class ReportStats(val total: Int, val correct: Int, val withErrors: Int, val wrong: Int)

private data class HomeworkStatistics(
    val today: ReportStats,
    val week: ReportStats,
    val topWeeklyError: String?,
    val recurringErrors: List<String>
)

private fun buildReportStatistics(reports: List<HomeworkReport>): HomeworkStatistics {
    // Statistiche giornaliere e settimanali calcolate dai report salvati.
    val now = Calendar.getInstance()
    val startOfToday = now.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val startOfWeek = (Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }).timeInMillis

    val todayResults = reports.filter { it.createdAt >= startOfToday }.flatMap { it.results }
    val weekResults = reports.filter { it.createdAt >= startOfWeek }.flatMap { it.results }

    val todayStats = computeReportStats(todayResults)
    val weekStats = computeReportStats(weekResults)

    val patterns = analyzeErrorPatterns(weekResults)
    val topWeeklyError = patterns.maxByOrNull { it.occurrences }?.category
    val recurringErrors = patterns.sortedByDescending { it.occurrences }
        .take(3)
        .map { it.category }

    return HomeworkStatistics(
        today = todayStats,
        week = weekStats,
        topWeeklyError = topWeeklyError,
        recurringErrors = recurringErrors
    )
}

private fun computeReportStats(results: List<ExerciseResult>): ReportStats {
    var correct = 0
    var withErrors = 0
    var wrong = 0
    results.forEach { result ->
        when (result.outcome()) {
            ExerciseOutcome.FAILED -> wrong++
            ExerciseOutcome.COMPLETED_WITH_ERRORS -> withErrors++
            ExerciseOutcome.PERFECT -> correct++
        }
    }
    return ReportStats(
        total = results.size,
        correct = correct,
        withErrors = withErrors,
        wrong = wrong
    )
}

private fun buildProgressInsights(
    currentResults: List<ExerciseResult>,
    previousReports: List<HomeworkReport>,
    referenceTime: Long
): List<String> {
    if (currentResults.isEmpty() || previousReports.isEmpty()) return emptyList()
    val weekStart = referenceTime - 7L * 24 * 60 * 60 * 1000
    val previousResults = previousReports.filter { it.createdAt >= weekStart }.flatMap { it.results }
    if (previousResults.isEmpty()) return emptyList()

    val currentPerfectPercent = percentPerfect(currentResults)
    val previousPerfectPercent = percentPerfect(previousResults)
    val currentIssues = currentResults.count { it.outcome() != ExerciseOutcome.PERFECT }
    val previousIssues = previousResults.count { it.outcome() != ExerciseOutcome.PERFECT }

    val insights = mutableListOf<String>()
    if (currentPerfectPercent > previousPerfectPercent + 4) {
        insights += "Più esercizi corretti rispetto agli ultimi 7 giorni"
    }
    if (currentIssues < previousIssues) {
        insights += "Meno esercizi da ripassare rispetto agli ultimi 7 giorni"
    }

    val previousTop = analyzeErrorPatterns(previousResults).firstOrNull()
    val currentTop = analyzeErrorPatterns(currentResults).firstOrNull()
    if (previousTop != null && (currentTop == null || currentTop.category != previousTop.category)) {
        insights += "Miglioramento nei passaggi legati a: ${previousTop.category.lowercase()}"
    }

    return insights
}

private fun buildEducationalBadges(
    currentResults: List<ExerciseResult>,
    previousReports: List<HomeworkReport>,
    referenceTime: Long
): List<String> {
    if (currentResults.isEmpty()) return emptyList()
    val badges = mutableListOf<String>()
    val allPerfect = currentResults.all { it.outcome() == ExerciseOutcome.PERFECT }
    if (allPerfect && currentResults.size >= 3) {
        badges += "Sessione con esercizi tutti corretti"
    }

    if (previousReports.isNotEmpty()) {
        val weekStart = referenceTime - 7L * 24 * 60 * 60 * 1000
        val previousResults = previousReports.filter { it.createdAt >= weekStart }.flatMap { it.results }
        if (previousResults.isNotEmpty()) {
            val improvement = percentPerfect(currentResults) - percentPerfect(previousResults)
            if (improvement >= 10) {
                badges += "Ha migliorato la precisione rispetto alla settimana scorsa"
            }
            val previousPatterns = analyzeErrorPatterns(previousResults)
            val currentPatterns = analyzeErrorPatterns(currentResults).map { it.category }.toSet()
            val improvedCategory = previousPatterns.firstOrNull { it.category !in currentPatterns }
            if (improvedCategory != null) {
                badges += "Ha rafforzato: ${improvedCategory.category.lowercase()}"
            }
        }
    }

    return badges.take(2)
}

private fun percentPerfect(results: List<ExerciseResult>): Int {
    if (results.isEmpty()) return 0
    val perfectCount = results.count { it.outcome() == ExerciseOutcome.PERFECT }
    return (perfectCount.toFloat() / results.size.toFloat() * 100).toInt()
}

private fun outcomeColor(outcome: ExerciseOutcome): Color {
    return when (outcome) {
        ExerciseOutcome.PERFECT -> Color(0xFF16A34A)
        ExerciseOutcome.COMPLETED_WITH_ERRORS -> Color(0xFFF59E0B)
        ExerciseOutcome.FAILED -> Color(0xFFDC2626)
    }
}

@Composable
private fun HomeworkUnsupportedScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    message: String
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameHeader(
            title = "Compiti",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )
        SeaGlassPanel(title = "Non disponibile") {
            Text(message)
        }
    }
}
