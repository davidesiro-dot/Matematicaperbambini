package com.example.matematicaperbambini

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun TeacherHubScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: () -> Unit,
    onOpenTaskList: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Area Insegnante",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Crea compiti e condividili con un codice",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCreateTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crea compito")
            }
            Button(
                onClick = onEditTask,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Modifica compito")
            }
            Button(
                onClick = onOpenTaskList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lista compiti")
            }
        }
    }
}

@Composable
fun TeacherCreateTaskScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    existingCodes: List<TeacherHomeworkCode>,
    initialDescription: String,
    onCreateCode: (TeacherHomeworkCode) -> Unit
) {
    var tipoEsercizio by remember { mutableStateOf("") }
    var numeroDomande by remember { mutableStateOf("") }
    var difficoltaParametri by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(initialDescription) {
        val parsed = decodeTeacherDescription(initialDescription)
        tipoEsercizio = parsed?.tipoEsercizio.orEmpty()
        numeroDomande = parsed?.numeroDomande?.toString().orEmpty()
        difficoltaParametri = parsed?.difficoltaParametri.orEmpty()
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Crea compito",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                OutlinedTextField(
                    value = tipoEsercizio,
                    onValueChange = { tipoEsercizio = it },
                    label = { Text("Tipo esercizio") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = numeroDomande,
                    onValueChange = { value ->
                        numeroDomande = value.filter { it.isDigit() }
                    },
                    label = { Text("Numero domande") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = difficoltaParametri,
                    onValueChange = { difficoltaParametri = it },
                    label = { Text("Difficoltà / parametri") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val domande = numeroDomande.toIntOrNull() ?: 0
                        if (tipoEsercizio.isBlank() || domande <= 0 || difficoltaParametri.isBlank()) {
                            Toast.makeText(context, "Completa tutti i campi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val draft = TeacherHomeworkDraft(
                            tipoEsercizio = tipoEsercizio.trim(),
                            numeroDomande = domande,
                            difficoltaParametri = difficoltaParametri.trim()
                        )
                        val createdAt = System.currentTimeMillis()
                        val seed = generateDeterministicSeed(draft)
                        val code = generateUniqueTeacherCode(seed, createdAt, existingCodes)
                        val description = encodeTeacherDescription(draft)
                        onCreateCode(
                            TeacherHomeworkCode(
                                code = code,
                                description = description,
                                createdAt = createdAt,
                                seed = seed
                            )
                        )
                        Toast.makeText(context, "Codice creato: $code", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crea codice compito")
                }
            }
        }
    }
}

@Composable
fun TeacherEditTaskScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    codes: List<TeacherHomeworkCode>,
    onSelectCode: (TeacherHomeworkCode) -> Unit,
    onCreateNew: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Modifica compito",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (codes.isEmpty()) {
                Text("Nessun compito creato")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(codes) { code ->
                        val draft = decodeTeacherDescription(code.description)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCode(code) },
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = code.code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (draft != null) {
                                    Text("Tipo esercizio: ${draft.tipoEsercizio}")
                                    Text("Numero domande: ${draft.numeroDomande}")
                                    Text("Difficoltà: ${draft.difficoltaParametri}")
                                } else {
                                    Text("Descrizione non disponibile")
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crea nuovo codice compito")
            }
        }
    }
}

@Composable
fun TeacherTaskListScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    codes: List<TeacherHomeworkCode>,
    onDeleteCodes: (List<TeacherHomeworkCode>) -> Unit
) {
    val context = LocalContext.current
    val selectedCodes = remember { mutableStateOf(setOf<String>()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Lista compiti",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (codes.isEmpty()) {
                Text("Nessun compito creato")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(codes) { code ->
                        val draft = decodeTeacherDescription(code.description)
                        val dateLabel = dateFormatter.format(Date(code.createdAt))
                        val isSelected = selectedCodes.value.contains(code.code)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedCodes.value = if (checked) {
                                                selectedCodes.value + code.code
                                            } else {
                                                selectedCodes.value - code.code
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = code.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (draft != null) {
                                    Text("Tipo esercizio: ${draft.tipoEsercizio}")
                                    Text("Numero domande: ${draft.numeroDomande}")
                                    Text("Difficoltà: ${draft.difficoltaParametri}")
                                } else {
                                    Text("Descrizione non disponibile")
                                }
                                Text("Data creazione: $dateLabel")
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        onDeleteCodes(listOf(code))
                                    }) {
                                        Text("Elimina")
                                    }
                                    TextButton(onClick = {
                                        val parsed = parseTeacherHomeworkCode(code)
                                        if (parsed != null) {
                                            shareTeacherHomeworkPdf(context, parsed.homework, parsed.draft)
                                        }
                                    }) {
                                        Text("Condividi")
                                    }
                                    TextButton(onClick = {
                                        val parsed = parseTeacherHomeworkCode(code)
                                        if (parsed != null) {
                                            printTeacherHomeworkPdf(context, parsed.homework, parsed.draft)
                                        }
                                    }) {
                                        Text("Stampa")
                                    }
                                    TextButton(onClick = {
                                        val parsed = parseTeacherHomeworkCode(code)
                                        if (parsed != null) {
                                            exportTeacherHomeworkPdf(context, parsed.homework, parsed.draft)
                                        }
                                    }) {
                                        Text("Esporta PDF")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectedCodes.value.isNotEmpty()) {
                Button(
                    onClick = {
                        val toDelete = codes.filter { selectedCodes.value.contains(it.code) }
                        onDeleteCodes(toDelete)
                        selectedCodes.value = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Elimina selezionati")
                }
            }
        }
    }
}

private fun generateDeterministicSeed(draft: TeacherHomeworkDraft): Long {
    val raw = "${draft.tipoEsercizio}|${draft.numeroDomande}|${draft.difficoltaParametri}"
    return raw.hashCode().toLong()
}

private fun generateUniqueTeacherCode(
    seed: Long,
    createdAt: Long,
    existingCodes: List<TeacherHomeworkCode>
): String {
    val used = existingCodes.map { it.code }.toSet()
    var base = (seed xor createdAt).absoluteValue
    if (base == 0L) base = 1L
    var code = base.toString(36).uppercase().padStart(8, '0').take(8)
    while (used.contains(code)) {
        base += 1
        code = base.toString(36).uppercase().padStart(8, '0').take(8)
    }
    return code
}
