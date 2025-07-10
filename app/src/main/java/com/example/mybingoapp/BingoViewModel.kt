package com.example.mybingoapp

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class BingoViewModel : ViewModel() {
    // --- State ---
    private val _gridSize = MutableStateFlow(5)
    val gridSize = _gridSize.asStateFlow()

    private val _playerId = MutableStateFlow("")
    val playerId = _playerId.asStateFlow()

    val bingoCard = mutableStateListOf<List<Int>>()
    val markedCells = mutableStateListOf<List<Boolean>>()

    private val _drawnNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val drawnNumbers = _drawnNumbers.asStateFlow()

    private val _isBingo = MutableStateFlow(false)
    val isBingo = _isBingo.asStateFlow()

    // --- TTS ---
    private var tts: TextToSpeech? = null

    // Lista de números disponibles para sortear (1-75 es el estándar)
    private var availableNumbers = (1..75).toMutableList()

    init {
        generatePlayerId()
    }

    fun setTts(textToSpeech: TextToSpeech) {
        tts = textToSpeech
    }

    fun setGridSize(size: String) {
        _gridSize.value = size.toIntOrNull()?.coerceIn(3, 10) ?: 5 // Limita el tamaño entre 3 y 10
    }

    fun setPlayerId(id: String) {
        _playerId.value = id
    }

    fun generatePlayerId() {
        val chars = ('A'..'Z') + ('0'..'9')
        _playerId.value = (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    // Inicia o reinicia el juego con nuevos parámetros
    fun startGame(size: Int, id: String) {
        _gridSize.value = size
        _playerId.value = id
        regenerateCard()
    }

    fun regenerateCard() {
        bingoCard.clear()
        markedCells.clear()
        _drawnNumbers.value = emptySet()
        availableNumbers = (1..75).toMutableList()
        _isBingo.value = false

        val cardNumbers = mutableSetOf<Int>()
        val totalCells = _gridSize.value * _gridSize.value

        while (cardNumbers.size < totalCells) {
            cardNumbers.add((1..75).random())
        }

        val numberList = cardNumbers.toList()
        for (i in 0 until _gridSize.value) {
            val row = numberList.subList(i * _gridSize.value, (i + 1) * _gridSize.value)
            bingoCard.add(row)
            markedCells.add(List(_gridSize.value) { false })
        }
    }

    fun drawNextNumber() {
        if (availableNumbers.isNotEmpty() && !_isBingo.value) {
            val randomIndex = Random.nextInt(availableNumbers.size)
            val newNumber = availableNumbers.removeAt(randomIndex)
            _drawnNumbers.value += newNumber

            updateMarkedCells()
            checkForBingo()
        }
    }

    private fun updateMarkedCells() {
        for (row in 0 until _gridSize.value) {
            for (col in 0 until _gridSize.value) {
                if (_drawnNumbers.value.contains(bingoCard[row][col])) {
                    markedCells[row] = markedCells[row].toMutableList().also { it[col] = true }
                }
            }
        }
    }

    fun checkForBingo() {
        val size = _gridSize.value
        var bingoFound = false

        // Check rows
        for (row in 0 until size) {
            if (markedCells[row].all { it }) {
                bingoFound = true
                break
            }
        }

        // Check columns
        if (!bingoFound) {
            for (col in 0 until size) {
                if ((0 until size).all { row -> markedCells[row][col] }) {
                    bingoFound = true
                    break
                }
            }
        }

        // Check diagonals
        if (!bingoFound) {
            if ((0 until size).all { i -> markedCells[i][i] }) bingoFound = true
            if (!bingoFound && (0 until size).all { i -> markedCells[i][size - 1 - i] }) bingoFound = true
        }

        if (bingoFound) {
            _isBingo.value = true
            viewModelScope.launch {
                speak("¡BINGO! ¡BINGO! ¡Felicitaciones!")
            }
        }
    }

    fun dismissBingoAlert() {
        _isBingo.value = false
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}