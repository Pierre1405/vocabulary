package com.example.myapplication.data

import com.example.myapplication.db.Sentence
import com.example.myapplication.db.Story_category
import com.example.myapplication.db.Translation
import com.example.myapplication.db.VocabularyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabularyRepository(driverFactory: DatabaseDriverFactory) {

    private val database = VocabularyDatabase(driverFactory.createDriver())

    suspend fun getAllCategories(): List<Long> = withContext(Dispatchers.Default) {
        database.categoryQueries.getAllCategories().executeAsList()
    }

    suspend fun getAllStories(): List<Long> = withContext(Dispatchers.Default) {
        database.storyQueries.getAllStories().executeAsList()
    }

    suspend fun getStoryById(id: Long): Long? = withContext(Dispatchers.Default) {
        database.storyQueries.getStoryById(id).executeAsOneOrNull()
    }

    suspend fun getAllPhrases(): List<Sentence> = withContext(Dispatchers.Default) {
        database.sentenceQueries.getAllPhrases().executeAsList()
    }

    suspend fun getPhrasesByCategory(categoryId: Long): List<Sentence> = withContext(Dispatchers.Default) {
        database.sentenceQueries.getPhrasesByCategory(categoryId).executeAsList()
    }

    suspend fun getPhrasesByStory(storyId: Long): List<Sentence> = withContext(Dispatchers.Default) {
        database.sentenceQueries.getPhrasesByStory(storyId).executeAsList()
    }

    suspend fun getTranslationsForStory(storyId: Long): List<Translation> = withContext(Dispatchers.Default) {
        database.translationQueries.getTranslationsForStory(storyId).executeAsList()
    }

    suspend fun getAllStoryCategories(): List<Story_category> = withContext(Dispatchers.Default) {
        database.storyCategoryQueries.getAllStoryCategories().executeAsList()
    }

    suspend fun getAllStoryTranslations() = withContext(Dispatchers.Default) {
        database.storyTranslationQueries.getAllStoryTranslations().executeAsList()
    }

    suspend fun getStoryTranslation(storyId: Long, locale: String): String? = withContext(Dispatchers.Default) {
        database.storyTranslationQueries.getStoryTranslation(storyId, locale).executeAsOneOrNull()
    }

    suspend fun getConfiguration(key: String): String? = withContext(Dispatchers.Default) {
        database.configurationQueries.getValue(key).executeAsOneOrNull()
    }
}
