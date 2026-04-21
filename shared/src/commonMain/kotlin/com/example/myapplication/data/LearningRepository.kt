package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.db.learning.LearningDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningRepository(driver: SqlDriver) {

    private val db = LearningDatabase(driver)
    private val queries = db.learningQueries

    private val TYPE_SENTENCE = "sentence"
    private val TYPE_WORD = "word"

    // Phrases

    suspend fun saveGrade(
        sentenceKey: String,
        sourceLocale: String,
        targetLocale: String,
        grade: Int
    ) = withContext(Dispatchers.Default) {
        queries.upsertGrade(sentenceKey, sourceLocale, targetLocale, grade.toLong(), TYPE_SENTENCE)
    }

    suspend fun countByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countByDirection(sourceLocale, targetLocale, TYPE_SENTENCE).executeAsOne()
        }

    suspend fun getSentenceKeysByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<String> = withContext(Dispatchers.Default) {
        queries.getKeysByDirection(sourceLocale, targetLocale, TYPE_SENTENCE).executeAsList()
    }

    suspend fun getGradesByDirection(
        sourceLocale: String,
        targetLocale: String
    ): Map<String, Int> = withContext(Dispatchers.Default) {
        queries.getGradesByDirection(sourceLocale, targetLocale, TYPE_SENTENCE).executeAsList()
            .associate { it.key to it.grade.toInt() }
    }

    // Mots

    suspend fun saveWordGrade(
        translationId: Long,
        sourceLocale: String,
        targetLocale: String,
        grade: Int
    ) = withContext(Dispatchers.Default) {
        queries.upsertGrade(translationId.toString(), sourceLocale, targetLocale, grade.toLong(), TYPE_WORD)
    }

    suspend fun getWordGradesForTranslations(
        translationIds: List<Long>,
        sourceLocale: String,
        targetLocale: String
    ): Map<Long, Int> = withContext(Dispatchers.Default) {
        translationIds.mapNotNull { id ->
            val grade = queries.getGrade(id.toString(), sourceLocale, targetLocale, TYPE_WORD)
                .executeAsOneOrNull()?.toInt() ?: return@mapNotNull null
            id to grade
        }.toMap()
    }

    suspend fun getAllWordsByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<Pair<Long, Int>> = withContext(Dispatchers.Default) {
        queries.getAllByDirection(sourceLocale, targetLocale, TYPE_WORD).executeAsList()
            .map { it.key.toLong() to it.grade.toInt() }
    }

    suspend fun countWordsByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countByDirection(sourceLocale, targetLocale, TYPE_WORD).executeAsOne()
        }
}
