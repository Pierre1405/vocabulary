package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.LearningRepository
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

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
    }

    fun playAll(audioPlayer: AudioPlayer) {
        if (_isPlayingAll.value) return
        _isPlayingAll.value = true
        playNext(audioPlayer, _sentences.value, 0)
    }

    private fun playNext(audioPlayer: AudioPlayer, sentences: List<SentenceWithTranslations>, index: Int) {
        if (!_isPlayingAll.value) {
            _currentPlayingIndex.value = -1
            return
        }
        if (index >= sentences.size) {
            if (_isLooping.value) {
                playNext(audioPlayer, sentences, 0)
            } else {
                _isPlayingAll.value = false
                _currentPlayingIndex.value = -1
            }
            return
        }
        _currentPlayingIndex.value = index
        audioPlayer.play(sentences[index].sentenceKey, _learnedLanguage.value) {
            viewModelScope.launch {
                playNext(audioPlayer, sentences, index + 1)
            }
        }
    }

    fun saveGrade(sentenceKey: String, grade: Int) {
        viewModelScope.launch {
            val nativeLang = _nativeLanguage.value
            val learnedLang = _learnedLanguage.value
            learningRepository.saveGrade(sentenceKey, nativeLang, learnedLang, grade)
            learningRepository.saveGrade(sentenceKey, learnedLang, nativeLang, grade)
        }
    }

    fun stopPlayAll(audioPlayer: AudioPlayer) {
        _isPlayingAll.value = false
        _currentPlayingIndex.value = -1
        audioPlayer.release()
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
        }
    }
}
