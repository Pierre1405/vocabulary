package com.example.myapplication.ui.review

import com.example.myapplication.data.TtsPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WordReviewPlayer(
    private val ttsPlayer: TtsPlayer,
    private val scope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _repeatCount = MutableStateFlow(1)
    val repeatCount: StateFlow<Int> = _repeatCount

    private var job: Job? = null

    fun cycleRepeat() {
        _repeatCount.value = if (_repeatCount.value >= 3) 1 else _repeatCount.value + 1
    }

    fun toggle(
        items: List<WordReviewItem>,
        startIndex: Int,
        sourceLocale: String,
        onIndexChanged: (Int) -> Unit
    ) {
        if (_isPlaying.value) {
            stop()
        } else {
            job = scope.launch {
                _isPlaying.value = true
                play(items, startIndex, sourceLocale, onIndexChanged)
                _isPlaying.value = false
            }
        }
    }

    private suspend fun play(
        items: List<WordReviewItem>,
        startIndex: Int,
        sourceLocale: String,
        onIndexChanged: (Int) -> Unit
    ) {
        if (items.isEmpty()) return
        var index = startIndex
        outer@ while (_isPlaying.value) {
            val item = items.getOrNull(index) ?: break
            onIndexChanged(index)
            val reversed = sourceLocale != item.wordLocale
            val srcText = if (reversed) item.translationWithArticle else item.lemmaWithArticle
            val srcLocale = if (reversed) item.translationLocale else item.wordLocale
            val tgtText = if (reversed) item.lemmaWithArticle else item.translationWithArticle
            val tgtLocale = if (reversed) item.wordLocale else item.translationLocale

            for (r in 0 until _repeatCount.value) {
                ttsPlayer.speak(srcText, srcLocale)
                delay(3_000)
                if (!_isPlaying.value) break@outer
                ttsPlayer.speak(tgtText, tgtLocale)
                delay(3_000)
                if (!_isPlaying.value) break@outer
            }
            index = (index + 1) % items.size
        }
    }

    fun stop() {
        job?.cancel()
        _isPlaying.value = false
    }

    fun release() = stop()
}
