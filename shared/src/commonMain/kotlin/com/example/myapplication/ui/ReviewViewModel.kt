package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: VocabularyRepository,
    val sourceLocale: String,
    val targetLocale: String
) : ViewModel() {

    private val _sentences = MutableStateFlow<List<SentenceWithTranslations>>(emptyList())
    val sentences: StateFlow<List<SentenceWithTranslations>> = _sentences

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentGrade = MutableStateFlow<Int?>(null)
    val currentGrade: StateFlow<Int?> = _currentGrade

    private val grades = mutableMapOf<Long, Int>()

    init {
        viewModelScope.launch {
            val sentenceIds = repository.getSentenceIdsByDirection(sourceLocale, targetLocale)
            val translations = repository.getTranslationsForSentences(sentenceIds)
            val translationsBySentenceId = translations.groupBy { it.sentence_id }

            repository.getGradesByDirection(sourceLocale, targetLocale)
                .forEach { (id, grade) -> grades[id] = grade.toInt() }

            _sentences.value = sentenceIds.map { sentenceId ->
                SentenceWithTranslations(
                    sentenceId = sentenceId,
                    translations = translationsBySentenceId[sentenceId]
                        ?.associate { it.locale to it.translation }
                        ?: emptyMap()
                )
            }
            updateCurrentGrade()
        }
    }

    private fun updateCurrentGrade() {
        val sentence = _sentences.value.getOrNull(_currentIndex.value)
        _currentGrade.value = sentence?.let { grades[it.sentenceId] }
    }

    fun saveGrade(sentenceId: Long, grade: Int) {
        viewModelScope.launch {
            repository.saveGrade(sentenceId, sourceLocale, targetLocale, grade)
            grades[sentenceId] = grade
            if (grade == 5) {
                val updated = _sentences.value.filter { it.sentenceId != sentenceId }
                _sentences.value = updated
                val size = updated.size
                if (size == 0) {
                    _currentIndex.value = 0
                    _currentGrade.value = null
                    return@launch
                }
                _currentIndex.value = _currentIndex.value.coerceAtMost(size - 1)
            } else {
                val size = _sentences.value.size
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
        val size = _sentences.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value + 1) % size
        updateCurrentGrade()
    }

    fun moveToPrevious() {
        val size = _sentences.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value - 1 + size) % size
        updateCurrentGrade()
    }
}
