package com.example.matematicaperbambini

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.automirrored.filled.ArrowBack





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongMultiplication2x2Game(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    var a by remember { mutableStateOf(47) }
    var b by remember { mutableStateOf(36) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moltiplicazioni difficili") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("$a × $b", fontSize = 32.sp)
            Text("Qui andrà la moltiplicazione in colonna guidata")

            Button(
                onClick = {
                    a = Random.nextInt(10, 100)
                    b = Random.nextInt(10, 100)
                }
            ) {
                Text("Nuovo esercizio")
            }
        }
    }
}
