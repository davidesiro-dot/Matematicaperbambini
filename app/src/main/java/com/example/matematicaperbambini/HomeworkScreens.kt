package com.example.matematicaperbambini

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeworkBuilderScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    lastResults: List<ExerciseResult>,
    onStartHomework: (List<HomeworkTaskConfig>) -> Unit
) {
    var mixedEnabled by remember { mutableStateOf(true) }
    var digitsInput by remember { mutableStateOf("1") }
    var exercisesCountInput by remember { mutableStateOf("5") }
    var repeatsInput by remember { mutableStateOf("1") }
    var hintsEnabled by remember { mutableStateOf(false) }
    var highlightsEnabled by remember { mutableStateOf(false) }
    var allowSolution by remember { mutableStateOf(false) }
    var autoCheck by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<ExerciseSourceConfig>(ExerciseSourceConfig.Random) }
    val manualOps = remember { mutableStateListOf<ManualOp.AB>() }
    var manualAInput by remember { mutableStateOf("") }
    var manualBInput by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GameHeader(
            title = "Compiti (genitore)",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        if (lastResults.isNotEmpty()) {
            val correctCount = lastResults.count { it.correct }
            SeaGlassPanel(title = "Ultimo report") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Esercizi: ${lastResults.size}", fontWeight = FontWeight.Bold)
                    Text("Corretti: $correctCount")
                    Text("Sbagliati: ${lastResults.size - correctCount}")
                }
            }
        }

        SeaGlassPanel(title = "Seleziona giochi") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tabelline miste", fontWeight = FontWeight.Bold)
                        Text("Moltiplicazioni con numeri vari", fontSize = 12.sp)
                    }
                    Switch(checked = mixedEnabled, onCheckedChange = { mixedEnabled = it })
                }
            }
        }

        if (mixedEnabled) {
            SeaGlassPanel(title = "Configurazione tabelline miste") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = digitsInput,
                        onValueChange = { digitsInput = it.filter(Char::isDigit).take(2) },
                        label = { Text("Difficoltà (cifre)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Aiuti", fontWeight = FontWeight.Bold)
                        HelpToggleRow("Suggerimenti", hintsEnabled) { hintsEnabled = it }
                        HelpToggleRow("Evidenziazioni", highlightsEnabled) { highlightsEnabled = it }
                        HelpToggleRow("Soluzione", allowSolution) { allowSolution = it }
                        HelpToggleRow("Auto-check", autoCheck) { autoCheck = it }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sorgente esercizi", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SourceChip(
                                label = "Random",
                                selected = source is ExerciseSourceConfig.Random,
                                onClick = { source = ExerciseSourceConfig.Random }
                            )
                            SourceChip(
                                label = "Manuale",
                                selected = source is ExerciseSourceConfig.Manual,
                                onClick = { source = ExerciseSourceConfig.Manual(manualOps.toList()) }
                            )
                        }
                    }

                    if (source is ExerciseSourceConfig.Manual) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Operazioni manuali", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualAInput,
                                    onValueChange = { manualAInput = it.filter(Char::isDigit).take(2) },
                                    label = { Text("A") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualBInput,
                                    onValueChange = { manualBInput = it.filter(Char::isDigit).take(2) },
                                    label = { Text("B") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    val a = manualAInput.toIntOrNull()
                                    val b = manualBInput.toIntOrNull()
                                    if (a != null && b != null) {
                                        manualOps += ManualOp.AB(a, b)
                                        manualAInput = ""
                                        manualBInput = ""
                                        source = ExerciseSourceConfig.Manual(manualOps.toList())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Aggiungi operazione") }

                            if (manualOps.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    manualOps.forEachIndexed { index, op ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• ${op.a} × ${op.b}")
                                            TextButton(onClick = {
                                                manualOps.removeAt(index)
                                                source = ExerciseSourceConfig.Manual(manualOps.toList())
                                            }) {
                                                Text("Rimuovi")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = exercisesCountInput,
                            onValueChange = { exercisesCountInput = it.filter(Char::isDigit).take(3) },
                            label = { Text("Quantità") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repeatsInput,
                            onValueChange = { repeatsInput = it.filter(Char::isDigit).take(2) },
                            label = { Text("Ripetizioni") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val configs = buildList {
                    if (mixedEnabled) {
                        val digits = digitsInput.toIntOrNull()
                        val exercisesCount = exercisesCountInput.toIntOrNull() ?: 5
                        val repeats = repeatsInput.toIntOrNull() ?: 1
                        val helpSettings = HelpSettings(
                            hintsEnabled = hintsEnabled,
                            highlightsEnabled = highlightsEnabled,
                            allowSolution = allowSolution,
                            autoCheck = autoCheck
                        )
                        val sourceConfig = when (source) {
                            is ExerciseSourceConfig.Manual -> ExerciseSourceConfig.Manual(manualOps.toList())
                            else -> ExerciseSourceConfig.Random
                        }
                        add(
                            HomeworkTaskConfig(
                                game = GameType.MULTIPLICATION_MIXED,
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
                }
                onStartHomework(configs)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = mixedEnabled
        ) { Text("Avvia Compito") }
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
    queue: List<ExerciseInstance>,
    onExit: (List<ExerciseResult>) -> Unit
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
            results = results
        )
        return
    }

    val current = queue[index]
    when (current.game) {
        GameType.MULTIPLICATION_MIXED -> TabellineMixedGame(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            fx = fx,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
            exercise = current,
            onExerciseFinished = { partial ->
                val endAt = System.currentTimeMillis()
                results += ExerciseResult(
                    instance = current,
                    correct = partial.correct,
                    attempts = partial.attempts,
                    wrongAnswers = partial.wrongAnswers,
                    startedAt = startAt,
                    endedAt = endAt
                )
                index += 1
            }
        )
        else -> HomeworkUnsupportedScreen(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            message = "Questo gioco non è ancora disponibile in modalità compiti."
        )
    }
}

@Composable
private fun HomeworkReportScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    results: List<ExerciseResult>
) {
    val correctCount = results.count { it.correct }
    val total = results.size

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
                                "Esercizio ${idx + 1}: ${result.instance.a ?: "?"} × ${result.instance.b ?: "?"}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(if (result.correct) "✅ Corretto" else "❌ Sbagliato")
                            Text("Tentativi: ${result.attempts}")
                            if (result.wrongAnswers.isNotEmpty()) {
                                Text("Errori: ${result.wrongAnswers.joinToString()}")
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
