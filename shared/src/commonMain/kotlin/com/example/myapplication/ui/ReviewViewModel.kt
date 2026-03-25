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

    init {
        viewModelScope.launch {
            val sentenceIds = repository.getSentenceIdsByDirection(sourceLocale, targetLocale)
            val translations = repository.getTranslationsForSentences(sentenceIds)
            val translationsBySentenceId = translations.groupBy { it.sentence_id }

            _sentences.value = sentenceIds.map { sentenceId ->
                SentenceWithTranslations(
                    sentenceId = sentenceId,
                    translations = translationsBySentenceId[sentenceId]
                        ?.associate { it.locale to it.translation }
                        ?: emptyMap()
                )
            }
        }
    }

    fun moveToNext() {
        val size = _sentences.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value + 1) % size
    }

    fun moveToPrevious() {
        val size = _sentences.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value - 1 + size) % size
    }
}
