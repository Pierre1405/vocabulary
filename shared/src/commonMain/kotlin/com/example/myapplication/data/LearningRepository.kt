package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.db.learning.LearningDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningRepository(driver: SqlDriver) {

    private val db = LearningDatabase(driver)
    private val queries = db.learningQueries
    private val wordQueries = db.wordLearningQueries

    suspend fun saveGrade(
        sentenceKey: String,
        sourceLocale: String,
        targetLocale: String,
        grade: Int
    ) = withContext(Dispatchers.Default) {
        queries.upsertGrade(sentenceKey, sourceLocale, targetLocale, grade.toLong())
    }

    suspend fun countByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countByDirection(sourceLocale, targetLocale).executeAsOne()
        }

    suspend fun getSentenceKeysByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<String> = withContext(Dispatchers.Default) {
        queries.getSentenceKeysByDirection(sourceLocale, targetLocale).executeAsList()
    }

    suspend fun getGradesByDirection(
        sourceLocale: String,
        targetLocale: String
    ): Map<String, Int> = withContext(Dispatchers.Default) {
        queries.getGradesByDirection(sourceLocale, targetLocale).executeAsList()
            .associate { it.sentence_key to it.grade.toInt() }
    }

    suspend fun saveWordGrade(entryId: Long, translationId: Long, grade: Int) =
        withContext(Dispatchers.Default) {
            wordQueries.upsertWordGrade(entryId, translationId, grade.toLong())
        }

    suspend fun getWordGradesByEntry(entryId: Long): Map<Long, Int> =
        withContext(Dispatchers.Default) {
            wordQueries.getWordGradesByEntry(entryId).executeAsList()
                .associate { it.translation_id to it.grade.toInt() }
        }

    suspend fun getAllWordLearning(): List<Triple<Long, Long, Int>> =
        withContext(Dispatchers.Default) {
            wordQueries.getAllWordLearning().executeAsList()
                .map { Triple(it.entry_id, it.translation_id, it.grade.toInt()) }
        }

    suspend fun countWordLearning(): Long =
        withContext(Dispatchers.Default) {
            wordQueries.getAllWordLearning().executeAsList().size.toLong()
        }
}
