package com.example.myapplication.data

import com.example.myapplication.db.Category
import com.example.myapplication.db.Phrases
import com.example.myapplication.db.Story
import com.example.myapplication.db.Story_category
import com.example.myapplication.db.VocabularyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabularyRepository(driverFactory: DatabaseDriverFactory) {

    private val database = VocabularyDatabase(driverFactory.createDriver())

    suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.Default) {
        database.categoryQueries.getAllCategories().executeAsList()
    }

    suspend fun getAllStories(): List<Story> = withContext(Dispatchers.Default) {
        database.storyQueries.getAllStories().executeAsList()
    }

    suspend fun getStoryById(id: Long): Story? = withContext(Dispatchers.Default) {
        database.storyQueries.getStoryById(id).executeAsOneOrNull()
    }

    suspend fun getAllPhrases(): List<Phrases> = withContext(Dispatchers.Default) {
        database.phraseQueries.getAllPhrases().executeAsList()
    }

    suspend fun getPhrasesByCategory(categoryId: Long): List<Phrases> = withContext(Dispatchers.Default) {
        database.phraseQueries.getPhrasesByCategory(categoryId).executeAsList()
    }

    suspend fun getPhrasesByStory(storyId: Long): List<Phrases> = withContext(Dispatchers.Default) {
        database.phraseQueries.getPhrasesByStory(storyId).executeAsList()
    }

    suspend fun getAllStoryCategories(): List<Story_category> = withContext(Dispatchers.Default) {
        database.storyCategoryQueries.getAllStoryCategories().executeAsList()
    }
}
