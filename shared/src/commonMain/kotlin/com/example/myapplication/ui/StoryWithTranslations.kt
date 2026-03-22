package com.example.myapplication.ui

data class StoryWithTranslations(
    val storyId: Long,
    val translations: Map<String, String>  // locale -> titre
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
