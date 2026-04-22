package com.example.myapplication.ui.practice

data class SentenceWithTranslations(
    val sentenceKey: String,
    val translations: Map<String, String>
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
