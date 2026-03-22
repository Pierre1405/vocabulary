package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StoryViewModel(private val repository: VocabularyRepository) : ViewModel() {

    private val _stories = MutableStateFlow<List<StoryWithTranslations>>(emptyList())
    val stories: StateFlow<List<StoryWithTranslations>> = _stories

    private val _nativeLanguage = MutableStateFlow("fr")
    val nativeLanguage: StateFlow<String> = _nativeLanguage

    private val _learnedLanguage = MutableStateFlow("de")
    val learnedLanguage: StateFlow<String> = _learnedLanguage

    init {
        viewModelScope.launch {
            _nativeLanguage.value = repository.getConfiguration("native_language") ?: "fr"
            _learnedLanguage.value = repository.getConfiguration("learned_language") ?: "de"

            val stories = repository.getAllStories()
            val allTranslations = repository.getAllStoryTranslations()
            val translationsByStoryId = allTranslations.groupBy { it.story_id }

            _stories.value = stories.map { storyId ->
                StoryWithTranslations(
                    storyId = storyId,
                    translations = translationsByStoryId[storyId]
                        ?.associate { it.locale to it.translation } ?: emptyMap()
                )
            }
        }
    }
}
