package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: VocabularyRepository,
    private val learningRepository: LearningRepository,
    val sourceLocale: String,
    val targetLocale: String
) : ViewModel() {

    private val _sentences = MutableStateFlow<List<SentenceWithTranslations>>(emptyList())
    val sentences: StateFlow<List<SentenceWithTranslations>> = _sentences

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentGrade = MutableStateFlow<Int?>(null)
    val currentGrade: StateFlow<Int?> = _currentGrade

    // sentenceKey -> grade
    private val grades = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            val sentenceKeys = learningRepository.getSentenceKeysByDirection(sourceLocale, targetLocale)

            val allSentences = repository.getAllSentences()
            val sentenceByKey = allSentences.associateBy { it.sentence_key }

            learningRepository.getGradesByDirection(sourceLocale, targetLocale)
                .forEach { (key, grade) -> grades[key] = grade }

            val matched = sentenceKeys.mapNotNull { sentenceByKey[it] }
            val translations = repository.getTranslationsForSentences(matched.map { it.sentence_key })
            val translationsByKey = translations.groupBy { it.sentence_key }

            _sentences.value = matched.map { sentence ->
                SentenceWithTranslations(
                    sentenceKey = sentence.sentence_key,
                    translations = translationsByKey[sentence.sentence_key]
                        ?.associate { it.locale to it.translation }
                        ?: emptyMap()
                )
            }
            updateCurrentGrade()
        }
    }

    private fun updateCurrentGrade() {
        val sentence = _sentences.value.getOrNull(_currentIndex.value)
        _currentGrade.value = sentence?.let { grades[it.sentenceKey] }
    }

    fun saveGrade(sentenceKey: String, grade: Int) {
        viewModelScope.launch {
            learningRepository.saveGrade(sentenceKey, sourceLocale, targetLocale, grade)
            grades[sentenceKey] = grade
            if (grade == 5) {
                val updated = _sentences.value.filter { it.sentenceKey != sentenceKey }
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
