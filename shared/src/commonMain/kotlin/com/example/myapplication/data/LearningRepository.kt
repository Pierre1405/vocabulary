package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.db.learning.LearningDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Virtual clock — anti-overwhelm mechanism
// ---------------------------------------------------------------------------
//
// Problem: if the user doesn't open the app for several days, all pending
// reviews become due at once, creating an unmanageable backlog.
//
// Solution: instead of scheduling reviews against real wall-clock time, we
// schedule them against a "virtual clock" (usage_time) that advances by at
// most MAX_DAILY_CATCH_UP_HOURS per calendar day.
//
// How it works:
//   - Two values are persisted in `learning_config`:
//       last_connection_time  — real epoch hours of the last session start
//       usage_time            — accumulated virtual hours
//   - On each session start (user opens the review selection screen):
//       elapsed   = min(now - last_connection_time, MAX_DAILY_CATCH_UP_HOURS)
//       usage_time += elapsed
//       last_connection_time = now
//   - All next_review values are stored and compared against usage_time.
//
// Effect: after a 7-day break, virtual time advances by only
// MAX_DAILY_CATCH_UP_HOURS hours, so at most that many hours' worth of
// reviews become due — not 7 days' worth.
//
// Initialisation: on first launch usage_time is seeded to currentEpochHours()
// so that existing next_review values (also in real epoch hours) remain valid.
// ---------------------------------------------------------------------------

private const val KEY_LAST_CONNECTION = "last_connection_time"
private const val KEY_USAGE_TIME = "usage_time"
private const val MAX_DAILY_CATCH_UP_HOURS = 24L

data class UpcomingWordRaw(
    val translationId: Long,
    val sourceLocale: String,
    val targetLocale: String,
    val hoursUntilDue: Long
)

data class UpcomingConjugationRaw(
    val key: String,
    val grade: Int,
    val hoursUntilDue: Long
)

data class LowGradeRaw(val key: String, val grade: Int)

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
    private val configQueries = db.learningConfigQueries

    private val TYPE_SENTENCE = "sentence"
    private val TYPE_WORD = "word"
    private val TYPE_CONJUGATION = "conjugation"
    private val LOCALE_CONJ = "de"

    // --- Virtual clock -------------------------------------------------

    /**
     * Advances the virtual clock by min(elapsed real hours, MAX_DAILY_CATCH_UP_HOURS)
     * and persists both usage_time and last_connection_time.
     * Must be called once per session (e.g. when the user opens the review screen).
     */
    suspend fun updateUsageTime() = withContext(Dispatchers.Default) {
        val now = currentEpochHours()
        val lastConnection = configQueries.getConfig(KEY_LAST_CONNECTION)
            .executeAsOneOrNull()?.toLongOrNull() ?: now
        val usageTime = configQueries.getConfig(KEY_USAGE_TIME)
            .executeAsOneOrNull()?.toLongOrNull() ?: now

        val elapsed = minOf(now - lastConnection, MAX_DAILY_CATCH_UP_HOURS)
        configQueries.setConfig(KEY_USAGE_TIME, (usageTime + elapsed).toString())
        configQueries.setConfig(KEY_LAST_CONNECTION, now.toString())
    }

    /** Returns the current virtual clock value (hours). */
    private fun currentUsageHours(): Long =
        configQueries.getConfig(KEY_USAGE_TIME)
            .executeAsOneOrNull()?.toLongOrNull() ?: currentEpochHours()

    // --- Phrases -------------------------------------------------------

    suspend fun saveGrade(
        sentenceKey: String,
        sourceLocale: String,
        targetLocale: String,
        grade: Int
    ) = withContext(Dispatchers.Default) {
        val current = queries.getGrade(sentenceKey, sourceLocale, targetLocale, TYPE_SENTENCE).executeAsOneOrNull()
        val newInterval = computeNextIntervalHours(current?.interval_hours?.toInt() ?: 0, grade)
        val nextReview = if (current == null) currentUsageHours() else currentUsageHours() + newInterval
        queries.upsertGrade(sentenceKey, sourceLocale, targetLocale, grade.toLong(), TYPE_SENTENCE, newInterval.toLong(), nextReview)
    }

    suspend fun countByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countDue(sourceLocale, targetLocale, TYPE_SENTENCE, currentUsageHours()).executeAsOne()
        }

    suspend fun getSentenceKeysByDirection(
        sourceLocale: String,
        targetLocale: String
    ): List<String> = withContext(Dispatchers.Default) {
        queries.getKeysDue(sourceLocale, targetLocale, TYPE_SENTENCE, currentUsageHours()).executeAsList()
    }

    suspend fun getGradesByDirection(
        sourceLocale: String,
        targetLocale: String
    ): Map<String, Int> = withContext(Dispatchers.Default) {
        queries.getGradesByDirection(sourceLocale, targetLocale, TYPE_SENTENCE).executeAsList()
            .associate { it.key to it.grade.toInt() }
    }

    // --- Mots ----------------------------------------------------------

    suspend fun saveWordGrade(
        translationId: Long,
        sourceLocale: String,
        targetLocale: String,
        grade: Int
    ) = withContext(Dispatchers.Default) {
        val key = translationId.toString()
        val current = queries.getGrade(key, sourceLocale, targetLocale, TYPE_WORD).executeAsOneOrNull()
        val newInterval = computeNextIntervalHours(current?.interval_hours?.toInt() ?: 0, grade)
        val nextReview = if (current == null) currentUsageHours() else currentUsageHours() + newInterval
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
        queries.getAllDue(sourceLocale, targetLocale, TYPE_WORD, currentUsageHours()).executeAsList()
            .map { it.key.toLong() to it.grade.toInt() }
    }

    suspend fun countWordsByDirection(sourceLocale: String, targetLocale: String): Long =
        withContext(Dispatchers.Default) {
            queries.countDue(sourceLocale, targetLocale, TYPE_WORD, currentUsageHours()).executeAsOne()
        }

    // --- Conjugaison ---------------------------------------------------

    suspend fun saveConjugationGrade(key: String, grade: Int) = withContext(Dispatchers.Default) {
        val current = queries.getGrade(key, LOCALE_CONJ, LOCALE_CONJ, TYPE_CONJUGATION).executeAsOneOrNull()
        val newInterval = computeNextIntervalHours(current?.interval_hours?.toInt() ?: 0, grade)
        val nextReview = if (current == null) currentUsageHours() else currentUsageHours() + newInterval
        queries.upsertGrade(key, LOCALE_CONJ, LOCALE_CONJ, grade.toLong(), TYPE_CONJUGATION, newInterval.toLong(), nextReview)
    }

    suspend fun countConjugationDue(): Long = withContext(Dispatchers.Default) {
        queries.countDue(LOCALE_CONJ, LOCALE_CONJ, TYPE_CONJUGATION, currentUsageHours()).executeAsOne()
    }

    /** All conjugation entries ever graded (including future-scheduled), key → grade. */
    suspend fun getAllConjugationGrades(): Map<String, Int> = withContext(Dispatchers.Default) {
        queries.getGradesByDirection(LOCALE_CONJ, LOCALE_CONJ, TYPE_CONJUGATION).executeAsList()
            .associate { it.key to it.grade.toInt() }
    }

    /** Only conjugation entries due now, key → grade. */
    suspend fun getDueConjugationGrades(): Map<String, Int> = withContext(Dispatchers.Default) {
        queries.getAllDue(LOCALE_CONJ, LOCALE_CONJ, TYPE_CONJUGATION, currentUsageHours()).executeAsList()
            .associate { it.key to it.grade.toInt() }
    }

    // --- Upcoming ------------------------------------------------------

    suspend fun getUpcomingGroups(): List<UpcomingGroup> = withContext(Dispatchers.Default) {
        val now = currentUsageHours()
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
        val now = currentUsageHours()
        queries.getUpcomingWordItems(now).executeAsList().map {
            UpcomingWordRaw(
                translationId = it.key.toLong(),
                sourceLocale = it.source_locale,
                targetLocale = it.target_locale,
                hoursUntilDue = it.next_review - now
            )
        }
    }

    suspend fun getUpcomingConjugationRaws(): List<UpcomingConjugationRaw> = withContext(Dispatchers.Default) {
        val now = currentUsageHours()
        queries.getUpcomingConjugationItems(now).executeAsList().map {
            UpcomingConjugationRaw(
                key = it.key,
                grade = it.grade.toInt(),
                hoursUntilDue = it.next_review - now
            )
        }
    }

    suspend fun getLowGradeRaws(type: String, sourceLocale: String, targetLocale: String, maxGrade: Int = 2): List<LowGradeRaw> =
        withContext(Dispatchers.Default) {
            queries.getLowGradeItems(sourceLocale, targetLocale, type, maxGrade.toLong()).executeAsList()
                .map { LowGradeRaw(it.key, it.grade.toInt()) }
        }

    // --- Suppression ---------------------------------------------------

    suspend fun deleteSentenceGrade(sentenceKey: String, sourceLocale: String, targetLocale: String) =
        withContext(Dispatchers.Default) {
            queries.deleteGrade(sentenceKey, sourceLocale, targetLocale, TYPE_SENTENCE)
        }

    suspend fun deleteWordGrade(translationId: Long, sourceLocale: String, targetLocale: String) =
        withContext(Dispatchers.Default) {
            queries.deleteGrade(translationId.toString(), sourceLocale, targetLocale, TYPE_WORD)
            queries.deleteGrade(translationId.toString(), targetLocale, sourceLocale, TYPE_WORD)
        }

    suspend fun deleteConjugationGroup(entryId: Long, groupKey: String) =
        withContext(Dispatchers.Default) {
            queries.deleteByKeyPrefix("$entryId|$groupKey|%", TYPE_CONJUGATION)
        }

    // --- Intervalle SRS ------------------------------------------------

    private fun computeNextIntervalHours(currentHours: Int, grade: Int): Int = when (grade) {
        1 -> 0
        2 -> 0
        3 -> if (currentHours > 0) (currentHours * 1.5).roundToInt() else 24
        4 -> if (currentHours > 0) (currentHours * 2.0).roundToInt() else 24
        else -> if (currentHours > 0) (currentHours * 2.5).roundToInt() else 24
    }
}
