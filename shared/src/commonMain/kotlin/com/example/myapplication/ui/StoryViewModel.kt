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

    private val _countNativeToLearned = MutableStateFlow(0L)
    val countNativeToLearned: StateFlow<Long> = _countNativeToLearned

    private val _countLearnedToNative = MutableStateFlow(0L)
    val countLearnedToNative: StateFlow<Long> = _countLearnedToNative

    init {
        viewModelScope.launch {
            val nativeLang = repository.getConfiguration("native_language") ?: "fr"
            val learnedLang = repository.getConfiguration("learned_language") ?: "de"
            _nativeLanguage.value = nativeLang
            _learnedLanguage.value = learnedLang

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

            refreshCounts()
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val nativeLang = _nativeLanguage.value
            val learnedLang = _learnedLanguage.value
            _countNativeToLearned.value = repository.countLearningByDirection(nativeLang, learnedLang)
            _countLearnedToNative.value = repository.countLearningByDirection(learnedLang, nativeLang)
        }
    }
}
