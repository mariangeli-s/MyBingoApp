package com.example.mybingoapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gridbuttons.ui.theme.GridButtonsTheme
import com.google.accompanist.flowlayout.FlowRow
import java.util.Locale

// --- ACTIVITY (Punto de entrada y configuración de Android) ---
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val bingoViewModel: BingoViewModel by viewModels()
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            GridButtonsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BingoAppNavigator(bingoViewModel)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }
            bingoViewModel.setTts(tts)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

// --- NAVEGACIÓN ---
@Composable
fun BingoAppNavigator(bingoViewModel: BingoViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(navController = navController, viewModel = bingoViewModel)
        }
        composable(
            route = "bingo/{gridSize}/{playerId}",
            arguments = listOf(
                navArgument("gridSize") { type = NavType.IntType },
                navArgument("playerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 5
            val playerId = backStackEntry.arguments?.getString("playerId") ?: "Player"
            bingoViewModel.startGame(gridSize, playerId)
            BingoScreen(viewModel = bingoViewModel)
        }
    }
}

// --- PANTALLA DE CONFIGURACIÓN ---
@Composable
fun SetupScreen(navController: NavController, viewModel: BingoViewModel) {
    val gridSize by viewModel.gridSize.collectAsState()
    val playerId by viewModel.playerId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generatePlayerId()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Configuración del Bingo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = gridSize.toString(),
            onValueChange = { viewModel.setGridSize(it) },
            label = { Text("Tamaño de la Matriz (ej. 5)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = playerId,
            onValueChange = { viewModel.setPlayerId(it) },
            label = { Text("Código de Jugador") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.generatePlayerId() }, modifier = Modifier.align(Alignment.End)) {
            Text("Generar ID")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("bingo/$gridSize/$playerId") },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("¡COMENZAR JUEGO!", fontSize = 18.sp)
        }
    }
}

// --- PANTALLA PRINCIPAL DEL JUEGO ---
@Composable
fun BingoScreen(viewModel: BingoViewModel) {
    val gridSize by viewModel.gridSize.collectAsState()
    val playerId by viewModel.playerId.collectAsState()
    val drawnNumbers by viewModel.drawnNumbers.collectAsState()
    val bingoCard = viewModel.bingoCard
    val markedCells = viewModel.markedCells
    val isBingo by viewModel.isBingo.collectAsState()

    if (isBingo) {
        BingoAlertDialog(
            playerName = playerId,
            onDismiss = { viewModel.dismissBingoAlert() }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Jugador: $playerId", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        DrawnNumbersDisplay(drawnNumbers = drawnNumbers)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            itemsIndexed(bingoCard.flatten()) { index, number ->
                val row = index / gridSize
                val col = index % gridSize
                val isMarked = markedCells.getOrNull(row)?.getOrNull(col) ?: false
                BingoCell(number = number, isMarked = isMarked)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.drawNextNumber() }, enabled = !isBingo) {
                Text("Sortear Número")
            }
            Button(onClick = { viewModel.regenerateCard() }) {
                Text("Regenerar Carta")
            }
        }
    }
}

// --- COMPONENTES DE UI REUTILIZABLES ---
@Composable
fun BingoCell(number: Int, isMarked: Boolean) {
    val backgroundColor = if (isMarked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isMarked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.padding(4.dp).aspectRatio(1f),
        border = BorderStroke(1.dp, Color.Gray),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = number.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun DrawnNumbersDisplay(drawnNumbers: Set<Int>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Números Sorteados: ${drawnNumbers.size}", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.padding(8.dp),
                mainAxisSpacing = 4.dp,
                crossAxisSpacing = 4.dp
            ) {
                drawnNumbers.sorted().forEach { number ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Text(text = number.toString(), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BingoAlertDialog(playerName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¡¡BINGO!!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("¡Felicidades, $playerName! Has ganado la partida.", textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Jugar de Nuevo") }
        }
    )
}