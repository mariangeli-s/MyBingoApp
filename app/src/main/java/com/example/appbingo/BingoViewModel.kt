package com.example.appbingo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class BingoViewModel : ViewModel() {
    var matrixSize by mutableStateOf(5)
    var playerUID by mutableStateOf(generateUID())
    var bingoMatrix by mutableStateOf(generateMatrix(5))
    var selectedMatrix by mutableStateOf(List(5) { MutableList(5) { false } })
    var showBingoDialog by mutableStateOf(false)

    fun setSize(size: Int) {
        matrixSize = size
        bingoMatrix = generateMatrix(size)
        selectedMatrix = List(size) { MutableList(size) { false } }
    }

    fun regenerateCard() {
        bingoMatrix = generateMatrix(matrixSize)
        selectedMatrix = List(matrixSize) { MutableList(matrixSize) { false } }
        showBingoDialog = false
    }

    fun toggleCell(row: Int, col: Int) {
        selectedMatrix = selectedMatrix.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, value ->
                if (r == row && c == col) !value else value
            }.toMutableList()
        }
        if (checkBingo()) showBingoDialog = true
    }

    private fun checkBingo(): Boolean {
        // Horizontal
        for (row in selectedMatrix) if (row.all { it }) return true
        // Vertical
        for (col in 0 until matrixSize) if (selectedMatrix.all { it[col] }) return true
        // Diagonal
        if ((0 until matrixSize).all { selectedMatrix[it][it] }) return true
        if ((0 until matrixSize).all { selectedMatrix[it][matrixSize - 1 - it] }) return true
        return false
    }

    companion object {
        private fun generateMatrix(size: Int): List<List<Int>> {
            val numbers = (1..size * size * 2).shuffled().take(size * size)
            return numbers.chunked(size)
        }

        private fun generateUID(): String {
            val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..6).map { chars.random() }.joinToString("")
        }
    }
}
