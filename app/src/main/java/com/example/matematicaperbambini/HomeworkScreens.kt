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
import androidx.compose.foundation.layout.heightIn
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
import java.util.Calendar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext

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
                            TabellineMode.values().forEach { mode ->
                                val selected = tableMode == mode
                                SourceChip(
                                    label = when (mode) {
                                        TabellineMode.CLASSIC -> "Classica"
                                        TabellineMode.MIXED -> "Mista"
                                        TabellineMode.GAPS -> "Buchi"
                                        TabellineMode.REVERSE -> "Inversa"
                                        TabellineMode.MULTIPLE_CHOICE -> "Scelta multipla"
                                    },
                                    selected = selected,
                                    onClick = { tableMode = mode }
                                )
                            }
                        }
                        AmountConfigRow(
                            exercisesCountInput = tableSelectedTables.size.takeIf { it > 0 }?.toString() ?: "0",
                            repeatsInput = tableRepeatsInput,
                            exercisesCountLabel = "Numero tabelline selezionate",
                            onExercisesCountChange = {},
                            onRepeatsChange = { tableRepeatsInput = it },
                            exercisesEditable = false
                        )
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
                            onValueChange = {
                                divisionDigitsInput = it.filter { char -> char in '1'..'3' }.take(1)
                            },
                            label = { Text("Cifre dividendo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = divisionDivisorDigitsInput,
                            onValueChange = {
                                divisionDivisorDigitsInput = it.filter { char -> char in '1'..'2' }.take(1)
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
                            manualRangeA = divisionManualDividendRange,
                            manualRangeB = divisionManualDivisorRange,
                            onAddManual = {
                                val a = divisionManualDividendInput.toIntOrNull()
                                val b = divisionManualDivisorInput.toIntOrNull()
                                if (a != null && b != null && a in divisionManualDividendRange && b in divisionManualDivisorRange) {
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
                        DivisionHelpConfigSection(
                            hintsEnabled = divisionHintsEnabled,
                            highlightsEnabled = divisionHighlightsEnabled,
                            allowSolution = divisionAllowSolution,
                            autoCheck = divisionAutoCheck,
                            showCellHelper = divisionShowCellHelper,
                            onHintsChange = { divisionHintsEnabled = it },
                            onHighlightsChange = { divisionHighlightsEnabled = it },
                            onAllowSolutionChange = { divisionAllowSolution = it },
                            onAutoCheckChange = { divisionAutoCheck = it },
                            onCellHelperChange = { divisionShowCellHelper = it }
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
                                hardMaxAInput = it.filter { char -> char in '1'..'3' }.take(1)
                            },
                            label = { Text("Cifre moltiplicando") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hardMaxBInput,
                            onValueChange = {
                                hardMaxBInput = it.filter { char -> char in '1'..'2' }.take(1)
                            },
                            label = { Text("Cifre moltiplicatore") },
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
                            manualRangeA = hardManualARange,
                            manualRangeB = hardManualBRange,
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

        item {
            val enabledConfigs = buildHomeworkConfigs(
                additionEnabled = additionEnabled,
                subtractionEnabled = subtractionEnabled,
                tableEnabled = tableEnabled,
                tableMode = tableMode,
                tableSelectedTables = tableSelectedTables,
                tableRepeatsInput = tableRepeatsInput,
                tableHintsEnabled = tableHintsEnabled,
                tableHighlightsEnabled = tableHighlightsEnabled,
                tableAllowSolution = tableAllowSolution,
                tableAutoCheck = tableAutoCheck,
                additionDigitsInput = additionDigitsInput,
                additionExercisesCountInput = additionExercisesCountInput,
                additionRepeatsInput = additionRepeatsInput,
                additionHintsEnabled = additionHintsEnabled,
                additionHighlightsEnabled = additionHighlightsEnabled,
                additionAllowSolution = additionAllowSolution,
                additionAutoCheck = additionAutoCheck,
                subtractionDigitsInput = subtractionDigitsInput,
                subtractionExercisesCountInput = subtractionExercisesCountInput,
                subtractionRepeatsInput = subtractionRepeatsInput,
                subtractionHintsEnabled = subtractionHintsEnabled,
                subtractionHighlightsEnabled = subtractionHighlightsEnabled,
                subtractionAllowSolution = subtractionAllowSolution,
                subtractionAutoCheck = subtractionAutoCheck,
                divisionDigitsInput = divisionDigitsInput,
                divisionDivisorDigitsInput = divisionDivisorDigitsInput,
                divisionExercisesCountInput = divisionExercisesCountInput,
                divisionRepeatsInput = divisionRepeatsInput,
                divisionHintsEnabled = divisionHintsEnabled,
                divisionHighlightsEnabled = divisionHighlightsEnabled,
                divisionAllowSolution = divisionAllowSolution,
                divisionAutoCheck = divisionAutoCheck,
                divisionShowCellHelper = divisionShowCellHelper,
                hardMaxAInput = hardMaxAInput,
                hardMaxBInput = hardMaxBInput,
                hardExercisesCountInput = hardExercisesCountInput,
                hardRepeatsInput = hardRepeatsInput,
                hardHintsEnabled = hardHintsEnabled,
                hardHighlightsEnabled = hardHighlightsEnabled,
                hardAllowSolution = hardAllowSolution,
                hardAutoCheck = hardAutoCheck,
                additionSource = additionSource,
                subtractionSource = subtractionSource,
                divisionSource = divisionSource,
                hardSource = hardSource
            )

            Button(
                onClick = { onStartHomework(enabledConfigs) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabledConfigs.isNotEmpty()
            ) {
                Text("Avvia compiti")
            }
        }
    }
}

@Composable
private fun buildHomeworkConfigs(
    additionEnabled: Boolean,
    subtractionEnabled: Boolean,
    tableEnabled: Boolean,
    tableMode: TabellineMode,
    tableSelectedTables: List<Int>,
    tableRepeatsInput: String,
    tableHintsEnabled: Boolean,
    tableHighlightsEnabled: Boolean,
    tableAllowSolution: Boolean,
    tableAutoCheck: Boolean,
    additionDigitsInput: String,
    additionExercisesCountInput: String,
    additionRepeatsInput: String,
    additionHintsEnabled: Boolean,
    additionHighlightsEnabled: Boolean,
    additionAllowSolution: Boolean,
    additionAutoCheck: Boolean,
    subtractionDigitsInput: String,
    subtractionExercisesCountInput: String,
    subtractionRepeatsInput: String,
    subtractionHintsEnabled: Boolean,
    subtractionHighlightsEnabled: Boolean,
    subtractionAllowSolution: Boolean,
    subtractionAutoCheck: Boolean,
    divisionDigitsInput: String,
    divisionDivisorDigitsInput: String,
    divisionExercisesCountInput: String,
    divisionRepeatsInput: String,
    divisionHintsEnabled: Boolean,
    divisionHighlightsEnabled: Boolean,
    divisionAllowSolution: Boolean,
    divisionAutoCheck: Boolean,
    divisionShowCellHelper: Boolean,
    hardMaxAInput: String,
    hardMaxBInput: String,
    hardExercisesCountInput: String,
    hardRepeatsInput: String,
    hardHintsEnabled: Boolean,
    hardHighlightsEnabled: Boolean,
    hardAllowSolution: Boolean,
    hardAutoCheck: Boolean,
    additionSource: ExerciseSourceConfig,
    subtractionSource: ExerciseSourceConfig,
    divisionSource: ExerciseSourceConfig,
    hardSource: ExerciseSourceConfig
): List<HomeworkTaskConfig> {
    val configs = mutableListOf<HomeworkTaskConfig>()
    if (additionEnabled) {
        val digits = additionDigitsInput.toIntOrNull() ?: 2
        val exercisesCount = additionExercisesCountInput.toIntOrNull() ?: 5
        val repeats = additionRepeatsInput.toIntOrNull() ?: 1
        configs += HomeworkTaskConfig(
            game = GameType.ADDITION,
            repeats = repeats,
            exerciseCount = exercisesCount,
            digits = digits,
            hintsEnabled = additionHintsEnabled,
            highlightsEnabled = additionHighlightsEnabled,
            allowSolution = additionAllowSolution,
            autoCheck = additionAutoCheck,
            source = additionSource
        )
    }
    if (subtractionEnabled) {
        val digits = subtractionDigitsInput.toIntOrNull() ?: 2
        val exercisesCount = subtractionExercisesCountInput.toIntOrNull() ?: 5
        val repeats = subtractionRepeatsInput.toIntOrNull() ?: 1
        configs += HomeworkTaskConfig(
            game = GameType.SUBTRACTION,
            repeats = repeats,
            exerciseCount = exercisesCount,
            digits = digits,
            hintsEnabled = subtractionHintsEnabled,
            highlightsEnabled = subtractionHighlightsEnabled,
            allowSolution = subtractionAllowSolution,
            autoCheck = subtractionAutoCheck,
            source = subtractionSource
        )
    }
    if (tableEnabled && tableSelectedTables.isNotEmpty()) {
        val repeats = tableRepeatsInput.toIntOrNull() ?: 1
        val configsForTables = tableSelectedTables.map { table ->
            HomeworkTaskConfig(
                game = when (tableMode) {
                    TabellineMode.CLASSIC -> GameType.MULTIPLICATION_TABLE
                    TabellineMode.MIXED -> GameType.MULTIPLICATION_MIXED
                    TabellineMode.GAPS -> GameType.MULTIPLICATION_GAPS
                    TabellineMode.REVERSE -> GameType.MULTIPLICATION_REVERSE
                    TabellineMode.MULTIPLE_CHOICE -> GameType.MULTIPLICATION_MULTIPLE_CHOICE
                },
                repeats = repeats,
                exerciseCount = 10,
                table = table,
                hintsEnabled = tableHintsEnabled,
                highlightsEnabled = tableHighlightsEnabled,
                allowSolution = tableAllowSolution,
                autoCheck = tableAutoCheck
            )
        }
        configs += configsForTables
    }
    if (divisionEnabled) {
        val digits = divisionDigitsInput.toIntOrNull() ?: 2
        val divisorDigits = divisionDivisorDigitsInput.toIntOrNull() ?: 1
        val exercisesCount = divisionExercisesCountInput.toIntOrNull() ?: 4
        val repeats = divisionRepeatsInput.toIntOrNull() ?: 1
        configs += HomeworkTaskConfig(
            game = GameType.DIVISION_STEP,
            repeats = repeats,
            exerciseCount = exercisesCount,
            digits = digits,
            divisorDigits = divisorDigits,
            hintsEnabled = divisionHintsEnabled,
            highlightsEnabled = divisionHighlightsEnabled,
            allowSolution = divisionAllowSolution,
            autoCheck = divisionAutoCheck,
            showCellHelper = divisionShowCellHelper,
            source = divisionSource
        )
    }
    if (hardEnabled) {
        val digitsA = hardMaxAInput.toIntOrNull() ?: 2
        val digitsB = hardMaxBInput.toIntOrNull() ?: 1
        val exercisesCount = hardExercisesCountInput.toIntOrNull() ?: 4
        val repeats = hardRepeatsInput.toIntOrNull() ?: 1
        configs += HomeworkTaskConfig(
            game = GameType.MULTIPLICATION_HARD,
            repeats = repeats,
            exerciseCount = exercisesCount,
            digits = digitsA,
            digitsB = digitsB,
            hintsEnabled = hardHintsEnabled,
            highlightsEnabled = hardHighlightsEnabled,
            allowSolution = hardAllowSolution,
            autoCheck = hardAutoCheck,
            source = hardSource
        )
    }
    return configs
}

@Composable
fun HomeworkRunnerScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: FxPlayer,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    queue: List<HomeworkExerciseEntry>,
    previousReports: List<HomeworkReport>,
    onExit: (List<ExerciseResult>) -> Unit,
    onSaveReport: (HomeworkReport) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<ExerciseResult>() }

    LaunchedEffect(queue) {
        currentIndex = 0
        results.clear()
    }

    if (queue.isEmpty()) {
        HomeworkUnsupportedScreen(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            message = "Nessun esercizio selezionato."
        )
        return
    }

    if (currentIndex >= queue.size) {
        HomeworkReportScreen(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = { onExit(results.toList()) },
            results = results.toList(),
            previousReports = previousReports,
            onSaveReport = onSaveReport
        )
        return
    }

    val entry = queue[currentIndex]
    val instance = entry.instance
    val exerciseIndex = currentIndex + 1
    val exerciseCount = queue.size
    val homeworkBonusLabel = "Compito $exerciseIndex/$exerciseCount"
    val homeworkBonusProgress = exerciseIndex / exerciseCount.toFloat()

    val onExerciseFinished: (ExerciseResult) -> Unit = { result ->
        results += result
        currentIndex += 1
    }

    when (instance.game) {
        GameType.ADDITION -> LongAdditionGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = entry.helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.SUBTRACTION -> LongSubtractionGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = entry.helps,
            onExerciseFinished = onExerciseFinished,
            bonusLabelOverride = homeworkBonusLabel,
            bonusProgressOverride = homeworkBonusProgress,
            exerciseKey = exerciseIndex
        )
        GameType.MULTIPLICATION_TABLE,
        GameType.MULTIPLICATION_GAPS,
        GameType.MULTIPLICATION_REVERSE,
        GameType.MULTIPLICATION_MULTIPLE_CHOICE,
        GameType.MULTIPLICATION_MIXED -> MultiplicationTableGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = instance,
            helps = entry.helps,
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
            helps = entry.helps,
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
            helps = entry.helps,
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
                        val report = HomeworkReport(
                            childName = safeName,
                            createdAt = System.currentTimeMillis(),
                            results = results
                        )
                        onSaveReport(report)
                        currentReport = report
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

        currentReport?.let { report ->
            SeaGlassPanel(title = "Stampa") {
                Button(
                    onClick = { printHomeworkReport(context, report) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stampa o esporta PDF")
                }
            }
        }

        SeaGlassPanel(title = "Riepilogo") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Totale esercizi: $total", fontWeight = FontWeight.Bold)
                Text("Corretto: $perfectCount")
                Text("Completato con errori: $withErrorsCount")
                Text("Da ripassare: ${total - perfectCount - withErrorsCount}")
            }
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

        if (errorPatterns.isNotEmpty()) {
            SeaGlassPanel(title = "🔍 Difficoltà principali") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    errorPatterns.take(3).forEach { pattern ->
                        Text("• ${pattern.category}")
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            SeaGlassPanel(title = "💡 Suggerimenti") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.take(2).forEach { suggestion ->
                        Text("• $suggestion")
                    }
                }
            }
        }

        if (progressInsights.isNotEmpty()) {
            SeaGlassPanel(title = "📈 Progressi") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    progressInsights.forEach { insight ->
                        Text(insight)
                    }
                }
            }
        }

        if (badges.isNotEmpty()) {
            SeaGlassPanel(title = "🏅 Riconoscimenti") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEach { badge ->
                        Text("• $badge")
                    }
                }
            }
        }

        SeaGlassPanel(title = "Dettaglio") {
            if (results.isEmpty()) {
                Text("Nessun risultato disponibile.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(results) { idx, result ->
                        val expected = expectedAnswer(result.instance)
                        val outcome = result.outcome()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Esercizio ${idx + 1}: ${exerciseLabel(result.instance)}",
                                fontWeight = FontWeight.Bold
                            )
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
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GameHeader(
                title = "Archivio report",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = {}
            )
        }

        if (reports.isEmpty()) {
            item {
                SeaGlassPanel(title = "Nessun report") {
                    Text("Non ci sono report salvati.")
                }
            }
        } else {
            item {
                val stats = remember(reports) { buildReportStatistics(reports) }
                SeaGlassPanel(title = "Statistiche") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📅 Giornaliero", fontWeight = FontWeight.Bold)
                        Text("Esercizi oggi: ${stats.today.total}")
                        Text("Corretto: ${stats.today.correct}")
                        Text("Con errori: ${stats.today.withErrors}")
                        Spacer(Modifier.height(8.dp))
                        Text("📊 Settimanale", fontWeight = FontWeight.Bold)
                        Text("Totale esercizi: ${stats.week.total}")
                        val percent = if (stats.week.total > 0) {
                            (stats.week.correct.toFloat() / stats.week.total.toFloat() * 100).toInt()
                        } else {
                            0
                        }
                        Text("Percentuale corretti: $percent%")
                        Text("Passaggio da rinforzare più frequente: ${stats.topWeeklyError ?: "Nessuno"}")
                        if (stats.recurringErrors.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("⭐ Difficoltà ricorrenti", fontWeight = FontWeight.Bold)
                            stats.recurringErrors.forEach { entry ->
                                Text("• $entry")
                            }
                        }
                    }
                }
            }

            itemsIndexed(reports) { idx, report ->
                SeaGlassPanel(title = "Report ${idx + 1}") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Bambino: ${report.childName}", fontWeight = FontWeight.Bold)
                        Text("Data: ${formatTimestamp(report.createdAt)}")
                        Button(
                            onClick = { printHomeworkReport(context, report) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stampa o esporta PDF")
                        }
                        val correctCount = report.results.count { it.outcome() == ExerciseOutcome.PERFECT }
                        val withErrorsCount = report.results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
                        val wrongCount = report.results.size - correctCount - withErrorsCount
                        Text("Risultati: $correctCount corretti, $withErrorsCount con errori, $wrongCount da ripassare")
                        Divider(Modifier.padding(vertical = 4.dp))
                        report.results.forEachIndexed { rIdx, result ->
                            val expected = expectedAnswer(result.instance)
                            val outcome = result.outcome()
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Esercizio ${rIdx + 1}: ${exerciseLabel(result.instance)}")
                                expected?.let { Text("Risposta corretta: $it") }
                                Text("Tentativi: ${result.attempts}")
                                val statusText = outcomeLabel(outcome)
                                val statusColor = outcomeColor(outcome)
                                Text("Esito: $statusText", color = statusColor)
                                if (result.wrongAnswers.isNotEmpty()) {
                                    Text("Risposte da rivedere: ${result.wrongAnswers.joinToString()}")
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
private fun HelpToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
        SourceChip(
            label = "Manuale",
            selected = source is ExerciseSourceConfig.Manual,
            onClick = { onSourceChange(ExerciseSourceConfig.Manual(emptyList())) }
        )
    }
}

@Composable
private fun ManualExerciseSection(
    source: ExerciseSourceConfig,
    onSourceChange: (ExerciseSourceConfig) -> Unit,
    manualOps: MutableList<ManualOp.AB>,
    manualAInput: String,
    manualBInput: String,
    onManualAChange: (String) -> Unit,
    onManualBChange: (String) -> Unit,
    opLabel: String,
    opLabelB: String,
    maxDigitsA: Int? = null,
    maxDigitsB: Int? = null,
    manualRangeA: IntRange? = null,
    manualRangeB: IntRange? = null,
    manualError: String? = null,
    onAddManual: () -> Unit,
    onRemoveManual: (Int) -> Unit,
    manualItemText: (ManualOp.AB) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SourceChip(
            label = "Manuale",
            selected = source is ExerciseSourceConfig.Manual,
            onClick = { onSourceChange(ExerciseSourceConfig.Manual(manualOps)) }
        )

        if (source is ExerciseSourceConfig.Manual) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualAInput,
                    onValueChange = {
                        val filtered = it.filter(Char::isDigit)
                        val limited = maxDigitsA?.let { max -> filtered.take(max) } ?: filtered
                        onManualAChange(limited)
                    },
                    label = { Text(opLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = manualBInput,
                    onValueChange = {
                        val filtered = it.filter(Char::isDigit)
                        val limited = maxDigitsB?.let { max -> filtered.take(max) } ?: filtered
                        onManualBChange(limited)
                    },
                    label = { Text(opLabelB) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            manualError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

            Button(
                onClick = onAddManual,
                enabled = manualAInput.isNotBlank() && manualBInput.isNotBlank()
            ) {
                Text("Aggiungi")
            }

            if (manualOps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color(0xFF4C6FFF) else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = contentColor
        )
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
