package com.example.matematicaperbambini

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        topBar = {
            GameHeader(
                title = "Crea compito",
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onBack = onBack,
                onLeaderboard = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SeaGlassPanel(title = "Dettagli compito") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tipoEsercizio,
                            onValueChange = { tipoEsercizio = it },
                            label = { Text("Tipo esercizio") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = numeroDomande,
                            onValueChange = { value ->
                                numeroDomande = value.filter { it.isDigit() }
                            },
                            label = { Text("Numero domande") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = difficoltaParametri,
                            onValueChange = { difficoltaParametri = it },
                            label = { Text("Difficoltà / parametri") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
    var selectedCodes by remember { mutableStateOf(setOf<String>()) }
    var multiSelectEnabled by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedItems = remember(codes, selectedCodes) {
        codes.filter { it.code in selectedCodes }
    }

    fun toggleSelection(code: String) {
        val updated = if (code in selectedCodes) selectedCodes - code else selectedCodes + code
        selectedCodes = updated
        if (updated.isEmpty()) {
            multiSelectEnabled = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sizing = menuSizing(maxHeight)
        val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()

        MenuHeaderLogoLayout(
            logoPainter = logoPainter,
            logoAreaHeight = sizing.logoAreaHeight,
            header = {
                GameHeader(
                    title = "Lista compiti",
                    soundEnabled = soundEnabled,
                    onToggleSound = onToggleSound,
                    onBack = onBack,
                    onLeaderboard = {}
                )
            },
            content = { contentModifier ->
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (codes.isEmpty()) {
                        item {
                            SeaGlassPanel(title = "Nessun compito") {
                                Text("Non ci sono compiti salvati.")
                            }
                        }
                    } else {
                        items(codes) { code ->
                            val draft = decodeTeacherDescription(code.description)
                            val dateLabel = formatReportDate(code.createdAt)
                            val selected = code.code in selectedCodes
                            SeaGlassPanel(
                                title = "Codice ${code.code}",
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (multiSelectEnabled) {
                                                toggleSelection(code.code)
                                            }
                                        },
                                        onLongClick = {
                                            multiSelectEnabled = true
                                            toggleSelection(code.code)
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
                                    Text(
                                        text = buildTeacherDescriptionLabel(draft),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("Data creazione: $dateLabel")
                                    Text(
                                        if (multiSelectEnabled) {
                                            "Tocca per selezionare"
                                        } else {
                                            "Tieni premuto per selezionare"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (codes.isNotEmpty()) {
                        item {
                            SeaGlassPanel(
                                title = "Azioni codici selezionati",
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        if (selectedItems.isEmpty())
                                            "Seleziona uno o più codici con una pressione prolungata."
                                        else
                                            "Codici selezionati: ${selectedItems.size}"
                                    )
                                    Button(
                                        onClick = { printTeacherHomeworkCodes(context, selectedItems) },
                                        enabled = selectedItems.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Stampa")
                                    }
                                    Button(
                                        onClick = { shareTeacherHomeworkCodes(context, selectedItems) },
                                        enabled = selectedItems.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Condividi")
                                    }
                                    Button(
                                        onClick = { exportTeacherHomeworkCodes(context, selectedItems) },
                                        enabled = selectedItems.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Esporta PDF")
                                    }
                                    Button(
                                        onClick = { showDeleteConfirm = true },
                                        enabled = selectedItems.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Elimina", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare i compiti selezionati?") },
            text = {
                Text("Questa operazione non può essere annullata.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = selectedItems
                        showDeleteConfirm = false
                        if (toDelete.isNotEmpty()) {
                            onDeleteCodes(toDelete)
                        }
                        selectedCodes = emptySet()
                        multiSelectEnabled = false
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

private fun buildTeacherDescriptionLabel(draft: TeacherHomeworkDraft?): String {
    return if (draft == null) {
        "Descrizione non disponibile"
    } else {
        "Descrizione: ${draft.tipoEsercizio} • ${draft.numeroDomande} domande • ${draft.difficoltaParametri}"
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
