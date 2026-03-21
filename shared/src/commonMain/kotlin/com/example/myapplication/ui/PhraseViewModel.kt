package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.db.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhraseViewModel(
    private val repository: VocabularyRepository,
    private val storyId: Long
) : ViewModel() {

    private val _story = MutableStateFlow<Story?>(null)
    val story: StateFlow<Story?> = _story

    private val _phrases = MutableStateFlow<List<PhraseWithTranslations>>(emptyList())
    val phrases: StateFlow<List<PhraseWithTranslations>> = _phrases

    init {
        viewModelScope.launch {
            _story.value = repository.getStoryById(storyId)

            val phrases = repository.getPhrasesByStory(storyId)
            val translations = repository.getTranslationsForStory(storyId)
            val translationsByPhraseId = translations.groupBy { it.sentence_id }

            _phrases.value = phrases.map { phrase ->
                PhraseWithTranslations(
                    phraseId = phrase.id,
                    translations = translationsByPhraseId[phrase.id]
                        ?.associate { it.locale to it.translation }
                        ?: emptyMap()
                )
            }
        }
    }
}
