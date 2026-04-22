package com.example.myapplication.ui.practice

data class StoryWithTranslations(
    val storyId: Long,
    val translations: Map<String, String>
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
