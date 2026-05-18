package com.example.myapplication.ui.review

import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.ui.practice.SentenceWithTranslations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ReviewPlayer(
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
        sentences: List<SentenceWithTranslations>,
        startIndex: Int,
        sourceLocale: String,
        targetLocale: String,
        onIndexChanged: (Int) -> Unit
    ) {
        if (_isPlaying.value) {
            stop()
        } else {
            job = scope.launch {
                _isPlaying.value = true
                play(sentences, startIndex, sourceLocale, targetLocale, onIndexChanged)
                _isPlaying.value = false
            }
        }
    }

    private suspend fun play(
        sentences: List<SentenceWithTranslations>,
        startIndex: Int,
        sourceLocale: String,
        targetLocale: String,
        onIndexChanged: (Int) -> Unit
    ) {
        if (sentences.isEmpty()) return
        var index = startIndex
        outer@ while (_isPlaying.value) {
            val sentence = sentences.getOrNull(index) ?: break
            onIndexChanged(index)
            for (r in 0 until _repeatCount.value) {
                suspendCancellableCoroutine { cont ->
                    ttsPlayer.speak(sentence.getTranslation(sourceLocale), sourceLocale) { cont.resume(Unit) }
                    cont.invokeOnCancellation { ttsPlayer.stop() }
                }
                if (!_isPlaying.value) break@outer
                delay(5_000)
                if (!_isPlaying.value) break@outer

                suspendCancellableCoroutine { cont ->
                    ttsPlayer.speak(sentence.getTranslation(targetLocale), targetLocale) { cont.resume(Unit) }
                    cont.invokeOnCancellation { ttsPlayer.stop() }
                }
                if (!_isPlaying.value) break@outer
                delay(3_000)
                if (!_isPlaying.value) break@outer
            }
            index = (index + 1) % sentences.size
        }
    }

    fun stop() {
        job?.cancel()
        _isPlaying.value = false
        ttsPlayer.stop()
    }

    fun release() {
        stop()
    }
}
