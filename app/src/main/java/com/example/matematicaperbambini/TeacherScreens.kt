package com.example.matematicaperbambini

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Random

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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sizing = menuSizing(maxHeight)
        val logoPainter = runCatching { painterResource(R.drawable.math_kids_logo) }.getOrNull()

        MenuHeaderLogoLayout(
            logoPainter = logoPainter,
            logoAreaHeight = sizing.logoAreaHeight,
            header = {
                GameHeader(
                    title = "Area Insegnante",
                    soundEnabled = soundEnabled,
                    onToggleSound = onToggleSound,
                    onBack = onBack,
                    onLeaderboard = onOpenLeaderboard
                )
            },
            content = { contentModifier ->
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SeaGlassPanel {
                            Text(
                                "Crea compiti e condividili con un codice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = onCreateTask,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Crea compito")
                        }
                    }
                    item {
                        Button(
                            onClick = onEditTask,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Modifica compito")
                        }
                    }
                    item {
                        Button(
                            onClick = onOpenTaskList,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lista compiti")
                        }
                    }
                }
            }
        )
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
    val snackbarHostState = remember { SnackbarHostState() }
    var description by remember { mutableStateOf(initialDescription) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialDescription) {
        description = initialDescription
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeaGlassPanel(title = "Descrizione compito") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(180) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Es. Allenamento con addizioni e sottrazioni") }
                    )
                }
            }

            item {
                SeaGlassPanel(title = "Come funziona") {
                    Text("Genera un codice da condividere con i tuoi studenti.")
                }
            }

            item {
                Button(
                    onClick = {
                        val trimmed = description.trim()
                        if (trimmed.isBlank()) return@Button
                        val createdAt = System.currentTimeMillis()
                        val seed = buildTeacherSeed(trimmed, createdAt)
                        val (code, finalSeed) = generateUniqueTeacherCode(seed, existingCodes)
                        val entry = TeacherHomeworkCode(
                            code = code,
                            description = trimmed,
                            createdAt = createdAt,
                            seed = finalSeed
                        )
                        onCreateCode(entry)
                        scope.launch {
                            snackbarHostState.showSnackbar("Codice creato: $code")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = description.trim().isNotEmpty()
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
    Column(modifier = Modifier.fillMaxSize()) {
        GameHeader(
            title = "Modifica compito",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (codes.isEmpty()) {
                item {
                    SeaGlassPanel(title = "Nessun compito") {
                        Text("Non ci sono codici compito disponibili.")
                    }
                }
            } else {
                itemsIndexed(codes) { _, code ->
                    SeaGlassPanel(
                        title = "Codice ${code.code}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCode(code) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(code.description)
                            Text("Creato il: ${formatTimestamp(code.createdAt)}")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crea nuovo codice compito")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeacherTaskListScreen(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBack: () -> Unit,
    codes: List<TeacherHomeworkCode>,
    onDeleteCodes: (List<TeacherHomeworkCode>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var multiSelectEnabled by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingExportFile by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = pendingExportFile
        if (uri != null && file != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        pendingExportFile = null
    }

    val selectedCodes = remember(codes, selectedKeys) {
        codes.filter { it.code in selectedKeys }
    }

    fun toggleSelection(key: String) {
        val updated = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
        selectedKeys = updated
        if (updated.isEmpty()) {
            multiSelectEnabled = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameHeader(
            title = "Lista compiti",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onLeaderboard = {}
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (codes.isEmpty()) {
                item {
                    SeaGlassPanel(title = "Nessun compito") {
                        Text("Non ci sono codici compito salvati.")
                    }
                }
            } else {
                itemsIndexed(codes) { _, code ->
                    val selected = code.code in selectedKeys
                    SeaGlassPanel(
                        title = "Codice ${code.code}",
                        modifier = Modifier
                            .fillMaxWidth()
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
                            Text(code.description)
                            Text("Data creazione: ${formatTimestamp(code.createdAt)}")
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
                    SeaGlassPanel(title = "Azioni compiti selezionati") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (selectedCodes.isEmpty())
                                    "Seleziona uno o più compiti con una pressione prolungata."
                                else
                                    "Compiti selezionati: ${selectedCodes.size}"
                            )

                            Button(
                                onClick = { printTeacherHomeworkCodes(context, selectedCodes) },
                                enabled = selectedCodes.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stampa")
                            }

                            Button(
                                onClick = { shareTeacherHomeworkCodes(context, selectedCodes) },
                                enabled = selectedCodes.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Condividi")
                            }

                            Button(
                                onClick = {
                                    val pdfFile = createTeacherHomeworkPdf(context, selectedCodes)
                                    if (pdfFile != null) {
                                        pendingExportFile = pdfFile
                                        exportLauncher.launch(pdfFile.name)
                                    }
                                },
                                enabled = selectedCodes.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Esporta PDF")
                            }

                            Button(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedCodes.isNotEmpty(),
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare i compiti selezionati?") },
            text = {
                Text(
                    "Sei sicuro di voler eliminare i compiti selezionati?\n" +
                        "Questa operazione non può essere annullata."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = selectedCodes
                        showDeleteConfirm = false
                        if (toDelete.isNotEmpty()) {
                            onDeleteCodes(toDelete)
                        }
                        selectedKeys = emptySet()
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

private fun shareTeacherHomeworkCodes(context: android.content.Context, codes: List<TeacherHomeworkCode>) {
    if (codes.isEmpty()) return
    val pdfFile = createTeacherHomeworkPdf(context, codes) ?: return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )
    val subject = if (codes.size == 1) "Codice compito" else "Codici compito (${codes.size})"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Condividi codici compito")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}

private fun buildTeacherSeed(description: String, createdAt: Long): Long {
    val normalized = description.lowercase().trim()
    return (normalized.hashCode().toLong() shl 32) xor createdAt
}

private fun generateUniqueTeacherCode(
    seed: Long,
    existingCodes: List<TeacherHomeworkCode>
): Pair<String, Long> {
    val used = existingCodes.map { it.code }.toSet()
    var currentSeed = seed
    while (true) {
        val code = generateTeacherCode(currentSeed)
        if (code !in used) {
            return code to currentSeed
        }
        currentSeed += 1
    }
}

private fun generateTeacherCode(seed: Long): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val random = Random(seed)
    return buildString {
        repeat(6) {
            append(chars[random.nextInt(chars.length)])
        }
    }
}
