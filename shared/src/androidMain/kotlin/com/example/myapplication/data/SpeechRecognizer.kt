package com.example.myapplication.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer

actual class SpeechRecognizer(private val context: Context) {
    private var recognizer: AndroidSpeechRecognizer? = null

    actual fun startListening(locale: String, onResult: (String) -> Unit, onError: () -> Unit) {
        recognizer?.destroy()
        recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                onResult(matches?.firstOrNull() ?: "")
            }
            override fun onError(error: Int) {
                android.util.Log.e("SpeechRecognizer", "onError code=$error")
                onError()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val fullLocale = localeToFullTag(locale)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, fullLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, fullLocale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    private fun localeToFullTag(locale: String): String = when (locale) {
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "en" -> "en-US"
        "es" -> "es-ES"
        "it" -> "it-IT"
        "pt" -> "pt-PT"
        "nl" -> "nl-NL"
        "ja" -> "ja-JP"
        "zh" -> "zh-CN"
        else -> locale
    }

    actual fun stopListening() {
        recognizer?.stopListening()
    }

    actual fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}
