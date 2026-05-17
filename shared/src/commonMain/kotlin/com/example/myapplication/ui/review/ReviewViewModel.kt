package com.example.myapplication.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.practice.SentenceWithTranslations
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

    private val grades = mutableMapOf<String, Int>()
    private val sessionGrades = mutableMapOf<String, Int>()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    private val _showPreview = MutableStateFlow(false)
    val showPreview: StateFlow<Boolean> = _showPreview

    private val _previewItems = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val previewItems: StateFlow<List<Pair<String, String>>> = _previewItems

    fun dismissPreview() { _showPreview.value = false }

    init {
        viewModelScope.launch {
            val sentenceKeys = learningRepository.getSentenceKeysByDirection(sourceLocale, targetLocale)

            val allSentences = repository.getAllSentences()
            val sentenceByKey = allSentences.associateBy { it.sentence_key }

            learningRepository.getGradesByDirection(sourceLocale, targetLocale)
                .forEach { (key, grade) -> grades[key] = grade }

            val matched = sentenceKeys.shuffled().mapNotNull { sentenceByKey[it] }
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
            val grade1 = _sentences.value
                .filter { grades[it.sentenceKey] == 1 }
                .map { it.getTranslation(sourceLocale) to it.getTranslation(targetLocale) }
            _previewItems.value = grade1
            if (grade1.isNotEmpty()) _showPreview.value = true
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
            sessionGrades[sentenceKey] = grade
            val size = _sentences.value.size
            if (size == 0) return@launch
            val next = _currentIndex.value + 1
            if (next >= size) _isCompleted.value = true
            else { _currentIndex.value = next; updateCurrentGrade() }
        }
    }

    fun navigateTo(index: Int) {
        _currentIndex.value = index
        updateCurrentGrade()
    }

    fun moveToNext() {
        val size = _sentences.value.size
        if (size == 0) return
        val next = _currentIndex.value + 1
        if (next >= size) _isCompleted.value = true
        else { _currentIndex.value = next; updateCurrentGrade() }
    }

    fun moveToPrevious() {
        val size = _sentences.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value - 1 + size) % size
        updateCurrentGrade()
    }

    fun deleteCurrent() {
        viewModelScope.launch {
            val list = _sentences.value
            val index = _currentIndex.value
            val sentence = list.getOrNull(index) ?: return@launch
            learningRepository.deleteSentenceGrade(sentence.sentenceKey, sourceLocale, targetLocale)
            grades.remove(sentence.sentenceKey)
            val newList = list.toMutableList().also { it.removeAt(index) }
            _sentences.value = newList
            if (newList.isEmpty()) { _isCompleted.value = true; return@launch }
            _currentIndex.value = index.coerceAtMost(newList.size - 1)
            updateCurrentGrade()
        }
    }

    fun restart(filterLowGrades: Boolean) {
        val list = if (filterLowGrades)
            _sentences.value.filter { (sessionGrades[it.sentenceKey] ?: Int.MAX_VALUE) < 3 }
        else
            _sentences.value
        sessionGrades.clear()
        _sentences.value = list
        _currentIndex.value = 0
        _isCompleted.value = false
        updateCurrentGrade()
    }
}
