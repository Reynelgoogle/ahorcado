package com.example.presentation.word

import androidx.lifecycle.ViewModel
import com.example.data.models.CategorizedWord
import com.example.data.models.WordCategory
import com.example.data.repository.GameRepository
import com.example.domain.usecases.ValidateWordUseCase
import com.example.domain.usecases.WordValidationResult
import com.example.utils.GameUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WordSelectionUiState(
    val wordInput: String = "",
    val categoryInput: String = "General",
    val hintInput: String = "",
    val errorMessage: String? = null,
    val selectedCategory: WordCategory? = GameUtils.Categories.firstOrNull(),
    val categories: List<WordCategory> = GameUtils.Categories
)

class WordSelectionViewModel(
    private val repository: GameRepository = GameRepository,
    private val validateWordUseCase: ValidateWordUseCase = ValidateWordUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordSelectionUiState())
    val uiState: StateFlow<WordSelectionUiState> = _uiState.asStateFlow()

    fun onWordChange(newWord: String) {
        val filtered = newWord.filter { it.isLetter() || it == ' ' }
        _uiState.value = _uiState.value.copy(
            wordInput = filtered,
            errorMessage = null
        )
    }

    fun onCategoryChange(newCategory: String) {
        _uiState.value = _uiState.value.copy(categoryInput = newCategory)
    }

    fun onHintChange(newHint: String) {
        _uiState.value = _uiState.value.copy(hintInput = newHint)
    }

    fun selectCategory(category: WordCategory) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            categoryInput = category.name
        )
    }

    fun selectCategorizedWord(item: CategorizedWord, categoryName: String) {
        _uiState.value = _uiState.value.copy(
            wordInput = item.word,
            hintInput = item.hint,
            categoryInput = categoryName,
            errorMessage = null
        )
    }

    fun confirmWord(onSuccess: () -> Unit) {
        val word = _uiState.value.wordInput.trim()
        when (val result = validateWordUseCase(word)) {
            is WordValidationResult.Valid -> {
                val category = _uiState.value.categoryInput.ifBlank { "General" }
                val hint = _uiState.value.hintInput.trim()
                val started = repository.selectWordAndStartGame(word, category, hint)
                if (started) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Solo el Anfitrión puede iniciar la partida")
                }
            }
            is WordValidationResult.Invalid -> {
                _uiState.value = _uiState.value.copy(errorMessage = result.reason)
            }
        }
    }
}
