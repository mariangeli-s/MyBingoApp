package com.example.appbingo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.appbingo.ui.theme.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton


const val BINGO_SIZE_MIN = 3
const val BINGO_SIZE_MAX = 5

@Composable
fun BingoApp(viewModel: BingoViewModel, onBingo: () -> Unit) {
    var showBingoScreen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SoftBackground
    ) {
        if (!showBingoScreen) {
            StartScreen(
                matrixSize = viewModel.matrixSize,
                playerUID = viewModel.playerUID,
                onSizeChange = { viewModel.setSize(it) },
                onContinue = { showBingoScreen = true }
            )
        } else {
            BingoScreen(
                viewModel = viewModel,
                onRegenerate = { viewModel.regenerateCard() },
                onCellClick = { row, col -> viewModel.toggleCell(row, col) }
            )

            if (viewModel.showBingoDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showBingoDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White.copy(alpha = 0.98f),
                    title = { Text("¡Bingo!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BluePrimary) },
                    text = { Text("¡Felicidades! Has hecho Bingo.", fontSize = 18.sp, color = DarkText) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.showBingoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                )
                LaunchedEffect(true) { onBingo() }
            }
        }
    }
}

@Composable
fun StartScreen(
    matrixSize: Int,
    playerUID: String,
    onSizeChange: (Int) -> Unit,
    onContinue: () -> Unit
) {
    var input by remember { mutableStateOf(matrixSize.toString()) }
    var isError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Crea tu Bingo",
            color = BluePrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = {
                val newValue = it.filter { c -> c.isDigit() }.take(2)
                input = newValue
                val valueInt = newValue.toIntOrNull()
                isError = valueInt == null || valueInt < BINGO_SIZE_MIN || valueInt > BINGO_SIZE_MAX
                if (!isError && valueInt != null) onSizeChange(valueInt)
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text("Tamaño de la carta (entre $BINGO_SIZE_MIN y $BINGO_SIZE_MAX)") },
            isError = isError,
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = "Editar tamaño", tint = BluePrimary)
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color.Red else BluePrimary,
                unfocusedBorderColor = CardGray,
                cursorColor = if (isError) Color.Red else BluePrimary,
                focusedLabelColor = if (isError) Color.Red else BluePrimary
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 4.dp)
        )
        if (isError) {
            Text(
                "El tamaño debe estar entre $BINGO_SIZE_MIN y $BINGO_SIZE_MAX",
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Text(
                "Ejemplo: 5x5. Solo números del $BINGO_SIZE_MIN al $BINGO_SIZE_MAX.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("Tu UID: $playerUID", fontWeight = FontWeight.Medium, color = DarkText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                onContinue()
            },
            enabled = !isError,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.6f)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isError) CardGray else BluePrimary,
                contentColor = if (isError) Color.Gray else Color.White
            )
        ) {
            Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun BingoScreen(
    viewModel: BingoViewModel,
    onRegenerate: () -> Unit,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .padding(16.dp)
    ) {
        // Header más visible y centrado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UID: ${viewModel.playerUID}",
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MintGreen, CircleShape)
                        .shadow(2.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Regenerar",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BingoGrid(
                numbers = viewModel.bingoMatrix,
                selected = viewModel.selectedMatrix,
                onCellClick = onCellClick
            )
        }
    }
}

@Composable
fun BingoGrid(numbers: List<List<Int>>, selected: List<List<Boolean>>, onCellClick: (Int, Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        numbers.forEachIndexed { rowIndex, row ->
            Row {
                row.forEachIndexed { colIndex, number ->
                    val isSelected = selected[rowIndex][colIndex]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(52.dp)
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .background(
                                brush = if (isSelected) Brush.horizontalGradient(
                                    listOf(SelectedGradientStart, SelectedGradientEnd)
                                ) else Brush.verticalGradient(
                                    listOf(CardGray, Color.White)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onCellClick(rowIndex, colIndex) }
                    ) {
                        Text(
                            text = number.toString(),
                            color = if (isSelected) Color.White else DarkText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}
