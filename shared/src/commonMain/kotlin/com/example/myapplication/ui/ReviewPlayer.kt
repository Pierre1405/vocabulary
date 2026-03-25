package com.example.myapplication.ui

import com.example.myapplication.data.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ReviewPlayer(
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var job: Job? = null

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
        while (_isPlaying.value) {
            val sentence = sentences.getOrNull(index) ?: break
            onIndexChanged(index)

            suspendCancellableCoroutine { cont ->
                audioPlayer.play(sentence.sentenceId, sourceLocale) { cont.resume(Unit) }
                cont.invokeOnCancellation { audioPlayer.release() }
            }
            if (!_isPlaying.value) break
            delay(5_000)
            if (!_isPlaying.value) break

            suspendCancellableCoroutine { cont ->
                audioPlayer.play(sentence.sentenceId, targetLocale) { cont.resume(Unit) }
                cont.invokeOnCancellation { audioPlayer.release() }
            }
            if (!_isPlaying.value) break
            delay(3_000)
            if (!_isPlaying.value) break

            index = (index + 1) % sentences.size
        }
    }

    fun stop() {
        job?.cancel()
        _isPlaying.value = false
        audioPlayer.release()
    }

    fun release() {
        stop()
    }
}
