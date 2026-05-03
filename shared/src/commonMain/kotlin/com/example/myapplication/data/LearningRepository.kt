package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.db.learning.LearningDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class UpcomingWordRaw(
    val translationId: Long,
    val sourceLocale: String,
    val targetLocale: String,
    val hoursUntilDue: Long
)

data class UpcomingGroup(
    val sourceLocale: String,
    val targetLocale: String,
    val type: String,
    val count: Int,
    val hoursUntilDue: Long
)

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
        val current = queries.getGrade(sentenceKey, sourceLocale, targetLocale, TYPE_SENTENCE).executeAsOneOrNull()
        val newInterval = computeNextIntervalHours(current?.interval_hours?.toInt() ?: 0, grade)
        val nextReview = if (current == null) currentEpochHours() else currentEpochHours() + newInterval
        queries.upsertGrade(sentenceKey, sourceLocale, targetLocale, grade.toLong(), TYPE_SENTENCE, newInterval.toLong(), nextReview)
    }

    suspend fun countByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countDue(sourceLocale, targetLocale, TYPE_SENTENCE, currentEpochHours()).executeAsOne()
        }

    suspend fun getSentenceKeysByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<String> = withContext(Dispatchers.Default) {
        queries.getKeysDue(sourceLocale, targetLocale, TYPE_SENTENCE, currentEpochHours()).executeAsList()
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
        val key = translationId.toString()
        val current = queries.getGrade(key, sourceLocale, targetLocale, TYPE_WORD).executeAsOneOrNull()
        val newInterval = computeNextIntervalHours(current?.interval_hours?.toInt() ?: 0, grade)
        val nextReview = if (current == null) currentEpochHours() else currentEpochHours() + newInterval
        queries.upsertGrade(key, sourceLocale, targetLocale, grade.toLong(), TYPE_WORD, newInterval.toLong(), nextReview)
    }

    suspend fun getWordGradesForTranslations(
        translationIds: List<Long>,
        sourceLocale: String,
        targetLocale: String
    ): Map<Long, Int> = withContext(Dispatchers.Default) {
        translationIds.mapNotNull { id ->
            val grade = queries.getGrade(id.toString(), sourceLocale, targetLocale, TYPE_WORD)
                .executeAsOneOrNull()?.grade?.toInt() ?: return@mapNotNull null
            id to grade
        }.toMap()
    }

    suspend fun getAllWordsByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<Pair<Long, Int>> = withContext(Dispatchers.Default) {
        queries.getAllDue(sourceLocale, targetLocale, TYPE_WORD, currentEpochHours()).executeAsList()
            .map { it.key.toLong() to it.grade.toInt() }
    }

    suspend fun countWordsByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countDue(sourceLocale, targetLocale, TYPE_WORD, currentEpochHours()).executeAsOne()
        }

    suspend fun getUpcomingGroups(): List<UpcomingGroup> = withContext(Dispatchers.Default) {
        val now = currentEpochHours()
        queries.getUpcomingGroups(now).executeAsList().map {
            UpcomingGroup(
                sourceLocale = it.source_locale,
                targetLocale = it.target_locale,
                type = it.type,
                count = it.count.toInt(),
                hoursUntilDue = (it.next_review_epoch ?: now) - now
            )
        }
    }

    suspend fun getUpcomingWordRaws(): List<UpcomingWordRaw> = withContext(Dispatchers.Default) {
        val now = currentEpochHours()
        queries.getUpcomingWordItems(now).executeAsList().map {
            UpcomingWordRaw(
                translationId = it.key.toLong(),
                sourceLocale = it.source_locale,
                targetLocale = it.target_locale,
                hoursUntilDue = it.next_review - now
            )
        }
    }

    private fun computeNextIntervalHours(currentHours: Int, grade: Int): Int = when (grade) {
        1 -> 0
        2 -> 1
        3 -> if (currentHours > 0) (currentHours * 1.5).roundToInt() else 24
        4 -> if (currentHours > 0) (currentHours * 2.0).roundToInt() else 24
        else -> if (currentHours > 0) (currentHours * 2.5).roundToInt() else 24
    }
}
