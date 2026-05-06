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

    private var job: Job? = null

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
        while (_isPlaying.value) {
            val item = items.getOrNull(index) ?: break
            onIndexChanged(index)
            val reversed = sourceLocale != item.wordLocale
            val srcText = if (reversed) item.translationWithArticle else item.lemmaWithArticle
            val srcLocale = if (reversed) item.translationLocale else item.wordLocale
            val tgtText = if (reversed) item.lemmaWithArticle else item.translationWithArticle
            val tgtLocale = if (reversed) item.wordLocale else item.translationLocale

            ttsPlayer.speak(srcText, srcLocale)
            delay(3_000)
            if (!_isPlaying.value) break

            ttsPlayer.speak(tgtText, tgtLocale)
            delay(3_000)
            if (!_isPlaying.value) break

            index = (index + 1) % items.size
        }
    }

    fun stop() {
        job?.cancel()
        _isPlaying.value = false
    }

    fun release() = stop()
}
