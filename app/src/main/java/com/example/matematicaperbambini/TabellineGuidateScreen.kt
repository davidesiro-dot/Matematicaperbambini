package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GuidedPhase(
    val title: String,
    val message: String,
    val visibleRows: Set<Int>,
    val validate: Boolean
)

@Composable
fun TabellineGuidateScreen(
    table: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    onPickAnother: () -> Unit,
    onExitToLearnMenu: () -> Unit
) {
    val phases = remember {
        listOf(
            GuidedPhase(
                title = "Fase 1/5 - Aiuto completo",
                message = "Guarda e riscrivi con calma 🙂",
                visibleRows = (1..10).toSet(),
                validate = false
            ),
            GuidedPhase(
                title = "Fase 2/5 - Aiuto con 3 numeri mancanti",
                message = "Prova da solo, guarda gli altri numeri se ti aiutano",
                visibleRows = (1..10).filterNot { it in setOf(3, 6, 8) }.toSet(),
                validate = true
            ),
            GuidedPhase(
                title = "Fase 3/5 - Aiuto con 7 numeri mancanti",
                message = "Bravo! Ora sai quasi tutta la tabellina 💪",
                visibleRows = setOf(1, 5, 10),
                validate = true
            ),
            GuidedPhase(
                title = "Fase 4/5 - Aiuto minimo",
                message = "Usa quello che sai per ricostruire tutto",
                visibleRows = setOf(5, 10),
                validate = true
            ),
            GuidedPhase(
                title = "Fase 5/5 - Nessun aiuto",
                message = "Se sbagli va bene, impariamo insieme ❤️",
                visibleRows = emptySet(),
                validate = true
            )
        )
    }

    val results = remember(table) { (1..10).map { table * it } }
    val inputs = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    val correctness = remember { mutableStateListOf<Boolean?>().apply { repeat(10) { add(null) } } }
    var phaseIndex by remember { mutableStateOf(0) }
    var showAllResults by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf(phases.first().message) }
    var completed by remember { mutableStateOf(false) }

    fun resetPhase() {
        for (i in 0 until 10) {
            inputs[i] = ""
            correctness[i] = null
        }
        showAllResults = false
        infoMessage = phases[phaseIndex].message
    }

    LaunchedEffect(table, phaseIndex) {
        resetPhase()
        completed = false
    }

    fun progressionHint(): String {
        val samples = listOf(table, table * 2, table * 3)
        return "Il $table si aggiunge ogni volta: ${samples.joinToString()}, …"
    }

    fun validatePhase(phase: GuidedPhase): Boolean {
        var allFilled = true
        var allCorrect = true
        for (i in 0 until 10) {
            val value = inputs[i].trim()
            if (value.isBlank()) {
                allFilled = false
                correctness[i] = null
                continue
            }
            val numeric = value.toIntOrNull()
            val expected = results[i]
            val isOk = numeric == expected
            correctness[i] = isOk
            if (!isOk) {
                allCorrect = false
            }
        }
        if (!allFilled) {
            infoMessage = "Completa tutte le righe con calma 🙂"
            return false
        }
        if (phase.validate && !allCorrect) {
            infoMessage = "Va bene sbagliare: controlla gli aiuti e riprova. ${progressionHint()}"
            return false
        }
        return true
    }

    fun fillOneExample() {
        val index = inputs.indexOfFirst { it.isBlank() }.takeIf { it >= 0 } ?: 0
        inputs[index] = results[index].toString()
        correctness[index] = true
        infoMessage = "Fammi vedere: ${table} × ${index + 1} = ${results[index]}. ${progressionHint()}"
    }

    val phase = phases[phaseIndex]

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallCircleButton("⬅") { onBack() }
                    Column {
                        Text(
                            "Tabellina del $table",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            phase.title,
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                SmallCircleButton(if (soundEnabled) "🔊" else "🔇") { onToggleSound() }
            }

            SeaGlassPanel(title = "AIUTO e TU") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("AIUTO", fontWeight = FontWeight.Black, color = Color(0xFF2563EB))
                        for (i in 1..10) {
                            val showResult = showAllResults || phase.visibleRows.contains(i)
                            val value = if (showResult) results[i - 1].toString() else "__"
                            val color = if (showResult) Color(0xFF111827) else Color(0xFF94A3B8)
                            Text(
                                text = "$table × $i = $value",
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("TU", fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                        for (i in 1..10) {
                            val index = i - 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$table × $i =",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(72.dp)
                                )
                                OutlinedTextField(
                                    value = inputs[index],
                                    onValueChange = { value ->
                                        inputs[index] = value.filter { it.isDigit() }.take(3)
                                        correctness[index] = null
                                    },
                                    singleLine = true,
                                    isError = correctness[index] == false,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            SeaGlassPanel(title = "Messaggio") {
                Text(
                    text = infoMessage,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        showAllResults = true
                        infoMessage = "Puoi guardare tutti i risultati e copiarli con calma."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Mostra tutti i risultati")
                }
                Button(
                    onClick = { fillOneExample() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Fammi vedere come si fa")
                }
            }

            Button(
                onClick = {
                    if (phase.validate) {
                        if (validatePhase(phase)) {
                            if (phaseIndex == phases.lastIndex) {
                                completed = true
                                infoMessage = "Hai imparato la tabellina del $table! 🌟"
                            } else {
                                phaseIndex += 1
                            }
                        }
                    } else {
                        val ready = inputs.all { it.isNotBlank() }
                        if (!ready) {
                            infoMessage = "Completa tutte le righe con calma 🙂"
                        } else {
                            phaseIndex += 1
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                Text("Continua", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }

            Spacer(Modifier.weight(1f))
        }

        if (completed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hai imparato la tabellina del $table! 🌟",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Vuoi ripetere o scegliere un’altra tabellina?",
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                phaseIndex = 0
                                resetPhase()
                                completed = false
                                onRepeat()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                        ) {
                            Text("Ripeti")
                        }
                        Button(
                            onClick = { onPickAnother() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Scegli un’altra")
                        }
                        Button(
                            onClick = { onExitToLearnMenu() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("Menu Impara")
                        }
                    }
                }
            }
        }
    }
}
