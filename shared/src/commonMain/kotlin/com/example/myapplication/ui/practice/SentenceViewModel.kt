package com.example.myapplication.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.data.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SentenceViewModel(
    private val repository: VocabularyRepository,
    private val learningRepository: LearningRepository,
    private val storyId: Long
) : ViewModel() {

    private val _story = MutableStateFlow<StoryWithTranslations?>(null)
    val story: StateFlow<StoryWithTranslations?> = _story

    private val _sentences = MutableStateFlow<List<SentenceWithTranslations>>(emptyList())
    val sentences: StateFlow<List<SentenceWithTranslations>> = _sentences

    private val _nativeLanguage = MutableStateFlow("fr")
    val nativeLanguage: StateFlow<String> = _nativeLanguage

    private val _learnedLanguage = MutableStateFlow("de")
    val learnedLanguage: StateFlow<String> = _learnedLanguage

    private val _isPlayingAll = MutableStateFlow(false)
    val isPlayingAll: StateFlow<Boolean> = _isPlayingAll

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping

    private val _currentPlayingIndex = MutableStateFlow(-1)
    val currentPlayingIndex: StateFlow<Int> = _currentPlayingIndex

    private val _grades = MutableStateFlow<Map<String, Int>>(emptyMap())
    val grades: StateFlow<Map<String, Int>> = _grades

    private val _repeatCount = MutableStateFlow(1)
    val repeatCount: StateFlow<Int> = _repeatCount

    fun cycleRepeat() {
        _repeatCount.value = if (_repeatCount.value >= 3) 1 else _repeatCount.value + 1
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
    }

    fun playAll(ttsPlayer: TtsPlayer) {
        if (_isPlayingAll.value) return
        _isPlayingAll.value = true
        playNext(ttsPlayer, _sentences.value, 0, _repeatCount.value)
    }

    private fun playNext(ttsPlayer: TtsPlayer, sentences: List<SentenceWithTranslations>, index: Int, remaining: Int) {
        if (!_isPlayingAll.value) {
            _currentPlayingIndex.value = -1
            return
        }
        if (index >= sentences.size) {
            if (_isLooping.value) {
                playNext(ttsPlayer, sentences, 0, _repeatCount.value)
            } else {
                _isPlayingAll.value = false
                _currentPlayingIndex.value = -1
            }
            return
        }
        _currentPlayingIndex.value = index
        val text = sentences[index].getTranslation(_learnedLanguage.value)
        ttsPlayer.speak(text, _learnedLanguage.value) {
            viewModelScope.launch {
                if (remaining > 1) {
                    playNext(ttsPlayer, sentences, index, remaining - 1)
                } else {
                    playNext(ttsPlayer, sentences, index + 1, _repeatCount.value)
                }
            }
        }
    }

    fun saveGrade(sentenceKey: String, grade: Int) {
        viewModelScope.launch {
            val nativeLang = _nativeLanguage.value
            val learnedLang = _learnedLanguage.value
            learningRepository.saveGrade(sentenceKey, nativeLang, learnedLang, grade)
            learningRepository.saveGrade(sentenceKey, learnedLang, nativeLang, grade)
            _grades.value = _grades.value + (sentenceKey to grade)
        }
    }

    fun stopPlayAll(ttsPlayer: TtsPlayer) {
        _isPlayingAll.value = false
        _currentPlayingIndex.value = -1
        ttsPlayer.stop()
    }

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

            val sentences = repository.getSentencesByStory(storyId)
            val translations = repository.getTranslationsForStory(storyId)
            val translationsByKey = translations.groupBy { it.sentence_key }

            _sentences.value = sentences.map { sentence ->
                SentenceWithTranslations(
                    sentenceKey = sentence.sentence_key,
                    translations = translationsByKey[sentence.sentence_key]
                        ?.associate { it.locale to it.translation }
                        ?: emptyMap()
                )
            }

            _grades.value = learningRepository.getGradesByDirection(nativeLang, learnedLang)
        }
    }
}
