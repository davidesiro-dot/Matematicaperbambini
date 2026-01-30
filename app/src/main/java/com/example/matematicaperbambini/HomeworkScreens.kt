package com.example.matematicaperbambini

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeworkBuilderScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    lastResults: List<ExerciseResult>,
    onStartHomework: (List<HomeworkTaskConfig>) -> Unit
) {
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

    var divisionEnabled by remember { mutableStateOf(false) }
    var divisionDigitsInput by remember { mutableStateOf("2") }
    var divisionDivisorDigitsInput by remember { mutableStateOf("1") }
    var divisionExercisesCountInput by remember { mutableStateOf("4") }
    var divisionRepeatsInput by remember { mutableStateOf("1") }
    var divisionHintsEnabled by remember { mutableStateOf(false) }
    var divisionHighlightsEnabled by remember { mutableStateOf(false) }
    var divisionAllowSolution by remember { mutableStateOf(false) }
    var divisionAutoCheck by remember { mutableStateOf(false) }
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
    var hardMaxAInput by remember { mutableStateOf("99") }
    var hardMaxBInput by remember { mutableStateOf("99") }
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
    val hardManualARange = 10..99
    val hardManualBRange = 1..99
    val hardSource = if (hardManualSelected) {
        ExerciseSourceConfig.Manual(hardManualOps.toList())
    } else {
        ExerciseSourceConfig.Random
    }

    LazyColumn(
        Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GameHeader(
                title = "Compiti (genitore)",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = {}
            )
        }

        if (lastResults.isNotEmpty()) {
            item {
                val correctCount = lastResults.count { it.correct }
                SeaGlassPanel(title = "Ultimo report") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Esercizi: ${lastResults.size}", fontWeight = FontWeight.Bold)
                        Text("Corretti: $correctCount")
                        Text("Sbagliati: ${lastResults.size - correctCount}")
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { table ->
                                    val selected = tableSelectedTables.contains(table)
                                    SourceChip(
                                        label = table.toString(),
                                        selected = selected,
                                        onClick = {
                                            if (selected) {
                                                tableSelectedTables.remove(table)
                                            } else {
                                                tableSelectedTables.add(table)
                                            }
                                        }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (6..10).forEach { table ->
                                    val selected = tableSelectedTables.contains(table)
                                    SourceChip(
                                        label = table.toString(),
                                        selected = selected,
                                        onClick = {
                                            if (selected) {
                                                tableSelectedTables.remove(table)
                                            } else {
                                                tableSelectedTables.add(table)
                                            }
                                        }
                                    )
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
                                SourceChip(
                                    label = "Classica",
                                    selected = tableMode == TabellineMode.CLASSIC,
                                    onClick = { tableMode = TabellineMode.CLASSIC }
                                )
                                SourceChip(
                                    label = "Buchi",
                                    selected = tableMode == TabellineMode.GAPS,
                                    onClick = { tableMode = TabellineMode.GAPS }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SourceChip(
                                    label = "Scelta multipla",
                                    selected = tableMode == TabellineMode.MULTIPLE_CHOICE,
                                    onClick = { tableMode = TabellineMode.MULTIPLE_CHOICE }
                                )
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
                            onHintsChange = { divisionHintsEnabled = it },
                            onHighlightsChange = { divisionHighlightsEnabled = it },
                            onAllowSolutionChange = { divisionAllowSolution = it },
                            onAutoCheckChange = { divisionAutoCheck = it }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = hardMaxAInput,
                                onValueChange = { hardMaxAInput = it.filter(Char::isDigit).take(2) },
                                label = { Text("Numero A (10-99)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hardMaxBInput,
                                onValueChange = { hardMaxBInput = it.filter(Char::isDigit).take(2) },
                                label = { Text("Numero B (1-99)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                            opLabel = "A",
                            opLabelB = "B",
                            maxDigitsA = hardManualARange.last.toString().length,
                            maxDigitsB = hardManualBRange.last.toString().length,
                            onAddManual = {
                                val a = hardManualAInput.toIntOrNull()
                                val b = hardManualBInput.toIntOrNull()
                                if (a != null && b != null && a in hardManualARange && b in hardManualBRange) {
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

        item {
            Button(
                onClick = {
                    val configs = buildList {
                        if (additionEnabled) {
                            val digits = additionDigitsInput.toIntOrNull()?.coerceIn(1, 3) ?: 2
                            val exercisesCount = additionExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 5
                            val repeats = additionRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                            val helpSettings = HelpSettings(
                                hintsEnabled = additionHintsEnabled,
                                highlightsEnabled = additionHighlightsEnabled,
                                allowSolution = additionAllowSolution,
                                autoCheck = additionAutoCheck
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
                                autoCheck = subtractionAutoCheck
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
                                autoCheck = tableAutoCheck
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
                                autoCheck = divisionAutoCheck
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
                            val maxA = hardMaxAInput.toIntOrNull()?.coerceIn(10, 99) ?: 99
                            val maxB = hardMaxBInput.toIntOrNull()?.coerceIn(1, 99) ?: 99
                            val exercisesCount = hardExercisesCountInput.toIntOrNull()?.coerceIn(1, 99) ?: 4
                            val repeats = hardRepeatsInput.toIntOrNull()?.coerceIn(1, 20) ?: 1
                            val helpSettings = HelpSettings(
                                hintsEnabled = hardHintsEnabled,
                                highlightsEnabled = hardHighlightsEnabled,
                                allowSolution = hardAllowSolution,
                                autoCheck = hardAutoCheck
                            )
                            val sourceConfig = if (hardManualOps.isNotEmpty()) {
                                ExerciseSourceConfig.Manual(hardManualOps.toList())
                            } else {
                                ExerciseSourceConfig.Random
                            }
                            add(
                                HomeworkTaskConfig(
                                    game = GameType.MULTIPLICATION_HARD,
                                    difficulty = DifficultyConfig(maxA = maxA, maxB = maxB),
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
                    onStartHomework(configs)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = anyEnabled
            ) { Text("Avvia Compito") }
        }
    }
}

@Composable
private fun GameToggleRow(
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
    onHintsChange: (Boolean) -> Unit,
    onHighlightsChange: (Boolean) -> Unit,
    onAllowSolutionChange: (Boolean) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aiuti", fontWeight = FontWeight.Bold)
        HelpToggleRow("Suggerimenti", hintsEnabled) { onHintsChange(it) }
        HelpToggleRow("Evidenziazioni", highlightsEnabled) { onHighlightsChange(it) }
        HelpToggleRow("Soluzione", allowSolution) { onAllowSolutionChange(it) }
        HelpToggleRow("Auto-check", autoCheck) { onAutoCheckChange(it) }
    }
}

@Composable
private fun AmountConfigRow(
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
private fun RandomSourceRow(
    source: ExerciseSourceConfig,
    onSourceChange: (ExerciseSourceConfig) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SourceChip(
            label = "Random",
            selected = source is ExerciseSourceConfig.Random,
            onClick = { onSourceChange(ExerciseSourceConfig.Random) }
        )
    }
}

@Composable
private fun ManualExerciseSection(
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
    onAddManual: () -> Unit,
    onRemoveManual: (Int) -> Unit,
    manualItemText: (ManualOp.AB) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceChip(
                label = "Manuale",
                selected = source is ExerciseSourceConfig.Manual,
                onClick = { onSourceChange(ExerciseSourceConfig.Manual(manualOps.toList())) }
            )
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
private fun HelpToggleRow(
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
        colors = colors,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) { Text(label) }
}

@Composable
fun HomeworkRunnerScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    queue: List<HomeworkExerciseEntry>,
    onExit: (List<ExerciseResult>) -> Unit,
    onSaveReport: (HomeworkReport) -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<ExerciseResult>() }
    var startAt by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(index) {
        startAt = System.currentTimeMillis()
    }

    if (index >= queue.size) {
        HomeworkReportScreen(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = { onExit(results.toList()) },
            results = results,
            onSaveReport = onSaveReport
        )
        return
    }

    val current = queue[index]
    val remainingExercises = (queue.size - index).coerceAtLeast(0)
    Box(Modifier.fillMaxSize()) {
        HomeworkExerciseGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
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
    onSaveReport: (HomeworkReport) -> Unit
) {
    val correctCount = results.count { it.correct }
    val total = results.size
    var childName by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(true) }
    var reportSaved by remember { mutableStateOf(false) }

    if (showNameDialog && !reportSaved) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Nome del bambino") },
            text = {
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it.take(24) },
                    label = { Text("Inserisci il nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeName = childName.trim().ifBlank { "Senza nome" }
                        onSaveReport(
                            HomeworkReport(
                                childName = safeName,
                                createdAt = System.currentTimeMillis(),
                                results = results
                            )
                        )
                        reportSaved = true
                        showNameDialog = false
                    },
                    enabled = childName.isNotBlank()
                ) { Text("OK") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameHeader(
            title = "Report Compito",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        SeaGlassPanel(title = "Riepilogo") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Totale esercizi: $total", fontWeight = FontWeight.Bold)
                Text("Corretti: $correctCount")
                Text("Sbagliati: ${total - correctCount}")
            }
        }

        SeaGlassPanel(title = "Dettaglio") {
            if (results.isEmpty()) {
                Text("Nessun risultato disponibile.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(results) { idx, result ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Esercizio ${idx + 1}: ${exerciseLabel(result.instance)}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(if (result.correct) "✅ Corretto" else "❌ Sbagliato")
                            Text("Tentativi: ${result.attempts}")
                            if (result.wrongAnswers.isNotEmpty()) {
                                Text("Errori: ${result.wrongAnswers.joinToString()}")
                            }
                            if (result.solutionUsed) {
                                Text("Soluzione usata")
                            }
                        }
                        if (idx < results.lastIndex) {
                            Divider(Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeworkReportsScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    reports: List<HomeworkReport>
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameHeader(
            title = "Archivio report",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        if (reports.isEmpty()) {
            SeaGlassPanel(title = "Nessun report") {
                Text("Non ci sono report salvati.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(reports) { idx, report ->
                    SeaGlassPanel(title = "Report ${idx + 1}") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bambino: ${report.childName}", fontWeight = FontWeight.Bold)
                            Text("Data: ${formatTimestamp(report.createdAt)}")
                            val correctCount = report.results.count { it.correct }
                            Text("Risultati: $correctCount/${report.results.size} corretti")
                            Divider(Modifier.padding(vertical = 4.dp))
                            report.results.forEachIndexed { rIdx, result ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Esercizio ${rIdx + 1}: ${exerciseLabel(result.instance)}")
                                    Text("Tentativi: ${result.attempts}")
                                    if (!result.correct) {
                                        Text("Esito: Sbagliato", color = Color(0xFFDC2626))
                                    } else {
                                        Text("Esito: Corretto", color = Color(0xFF16A34A))
                                    }
                                    if (result.wrongAnswers.isNotEmpty()) {
                                        Text("Errori: ${result.wrongAnswers.joinToString()}")
                                    }
                                    if (result.solutionUsed) {
                                        Text("Soluzione usata")
                                    }
                                }
                                if (rIdx < report.results.lastIndex) {
                                    Divider(Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exerciseLabel(instance: ExerciseInstance): String {
    val a = instance.a ?: "?"
    val b = instance.b ?: "?"
    return when (instance.game) {
        GameType.ADDITION -> "$a + $b"
        GameType.SUBTRACTION -> "$a - $b"
        GameType.MULTIPLICATION_TABLE,
        GameType.MULTIPLICATION_GAPS -> "Tabellina del ${instance.table ?: a}"
        GameType.MULTIPLICATION_REVERSE,
        GameType.MULTIPLICATION_MULTIPLE_CHOICE -> "${instance.table ?: a} × $b"
        GameType.DIVISION_STEP -> "$a ÷ $b"
        else -> "$a × $b"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
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
