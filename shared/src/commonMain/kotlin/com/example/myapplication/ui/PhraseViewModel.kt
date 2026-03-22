package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhraseViewModel(
    private val repository: VocabularyRepository,
    private val storyId: Long
) : ViewModel() {

    private val _story = MutableStateFlow<StoryWithTranslations?>(null)
    val story: StateFlow<StoryWithTranslations?> = _story

    private val _phrases = MutableStateFlow<List<PhraseWithTranslations>>(emptyList())
    val phrases: StateFlow<List<PhraseWithTranslations>> = _phrases

    private val _nativeLanguage = MutableStateFlow("fr")
    val nativeLanguage: StateFlow<String> = _nativeLanguage

    private val _learnedLanguage = MutableStateFlow("de")
    val learnedLanguage: StateFlow<String> = _learnedLanguage

    init {
        viewModelScope.launch {
            val nativeLang = repository.getConfiguration("native_language") ?: "fr"
            val learnedLang = repository.getConfiguration("learned_language") ?: "de"
            _nativeLanguage.value = nativeLang
            _learnedLanguage.value = learnedLang

            val storyRow = repository.getStoryById(storyId)
            if (storyRow != null) {
                _story.value = StoryWithTranslations(
                    storyId = storyRow,
                    translations = mapOf(
                        nativeLang to (repository.getStoryTranslation(storyId, nativeLang) ?: ""),
                        learnedLang to (repository.getStoryTranslation(storyId, learnedLang) ?: "")
                    )
                )
            }

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
