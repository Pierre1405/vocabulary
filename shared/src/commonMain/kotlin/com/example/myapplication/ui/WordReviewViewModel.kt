package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WordReviewItem(
    val entryId: Long,
    val translationId: Long,
    val lemma: String,
    val wordLocale: String,
    val translationText: String,
    val translationLocale: String
)

class WordReviewViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val learningRepository: LearningRepository,
    val reversed: Boolean = false
) : ViewModel() {

    private val _items = MutableStateFlow<List<WordReviewItem>>(emptyList())
    val items: StateFlow<List<WordReviewItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentGrade = MutableStateFlow<Int?>(null)
    val currentGrade: StateFlow<Int?> = _currentGrade

    private val grades = mutableMapOf<Long, Int>() // translationId -> grade

    init {
        viewModelScope.launch {
            val wordLearning = learningRepository.getAllWordLearning()
            _items.value = withContext(Dispatchers.Default) {
                wordLearning.mapNotNull { (entryId, translationId, grade) ->
                    grades[translationId] = grade
                    val entry = dictionaryRepository.getById(entryId) ?: return@mapNotNull null
                    val translations = dictionaryRepository.getTranslations(entryId)
                    val translation = translations.find { it.id == translationId } ?: return@mapNotNull null
                    WordReviewItem(
                        entryId = entryId,
                        translationId = translationId,
                        lemma = entry.lemma,
                        wordLocale = entry.locale,
                        translationText = translation.text,
                        translationLocale = translation.targetLocale
                    )
                }
            }
            updateCurrentGrade()
        }
    }

    private fun updateCurrentGrade() {
        val item = _items.value.getOrNull(_currentIndex.value)
        _currentGrade.value = item?.let { grades[it.translationId] }
    }

    fun saveGrade(translationId: Long, entryId: Long, grade: Int) {
        viewModelScope.launch {
            learningRepository.saveWordGrade(entryId, translationId, grade)
            grades[translationId] = grade
            if (grade == 5) {
                val updated = _items.value.filter { it.translationId != translationId }
                _items.value = updated
                val size = updated.size
                if (size == 0) {
                    _currentIndex.value = 0
                    _currentGrade.value = null
                    return@launch
                }
                _currentIndex.value = _currentIndex.value.coerceAtMost(size - 1)
            } else {
                val size = _items.value.size
                if (size == 0) return@launch
                _currentIndex.value = (_currentIndex.value + 1) % size
            }
            updateCurrentGrade()
        }
    }

    fun navigateTo(index: Int) {
        _currentIndex.value = index
        updateCurrentGrade()
    }

    fun moveToNext() {
        val size = _items.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value + 1) % size
        updateCurrentGrade()
    }

    fun moveToPrevious() {
        val size = _items.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value - 1 + size) % size
        updateCurrentGrade()
    }
}
