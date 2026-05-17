package com.example.myapplication.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

actual class TtsPlayer(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var pendingText: String? = null
    private var pendingLocale: String? = null
    private var pendingOnDone: (() -> Unit)? = null
    private var currentOnDone: (() -> Unit)? = null
    private val counter = AtomicInteger(0)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                val text = pendingText
                val locale = pendingLocale
                val onDone = pendingOnDone
                if (text != null && locale != null) speakNow(text, locale, onDone)
                pendingText = null
                pendingLocale = null
                pendingOnDone = null
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) {
                val cb = currentOnDone
                currentOnDone = null
                cb?.invoke()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                currentOnDone = null
            }
        })
    }

    actual fun speak(text: String, locale: String, onDone: (() -> Unit)?) {
        if (initialized) {
            speakNow(text, locale, onDone)
        } else {
            pendingText = text
            pendingLocale = locale
            pendingOnDone = onDone
        }
    }

    actual fun stop() {
        currentOnDone = null
        tts?.stop()
    }

    private fun speakNow(text: String, locale: String, onDone: (() -> Unit)?) {
        val javaLocale = when (locale) {
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "en" -> Locale.ENGLISH
            else -> Locale(locale)
        }
        tts?.setLanguage(javaLocale)
        currentOnDone = onDone
        val utteranceId = "tts_${counter.incrementAndGet()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    actual fun release() {
        currentOnDone = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
