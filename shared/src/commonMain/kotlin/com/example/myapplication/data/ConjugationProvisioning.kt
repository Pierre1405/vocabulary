package com.example.myapplication.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// In-memory cache — résolution coûteuse (186 queries) faite une seule fois par session.
object ConjugationCache {
    @Volatile var entryIdByInfinitive: Map<String, Long>? = null
    @Volatile var formsByEntryId: Map<Long, List<DictForm>>? = null

    suspend fun getEntryIds(dictionaryRepository: DictionaryRepository): Map<String, Long> =
        entryIdByInfinitive ?: withContext(Dispatchers.Default) {
            val map = mutableMapOf<String, Long>()
            for (verb in GERMAN_VERB_LEARNING_LIST) {
                val lemma = verb.infinitive.removePrefix("sich ")
                dictionaryRepository.getByLemma(lemma, "de").firstOrNull()
                    ?.let { map[verb.infinitive] = it.id }
            }
            map.also { entryIdByInfinitive = it }
        }

    suspend fun getForms(dictionaryRepository: DictionaryRepository): Map<Long, List<DictForm>> =
        formsByEntryId ?: withContext(Dispatchers.Default) {
            val entryIds = getEntryIds(dictionaryRepository)
            val map = mutableMapOf<Long, List<DictForm>>()
            for (verb in GERMAN_VERB_LEARNING_LIST) {
                val entryId = entryIds[verb.infinitive] ?: continue
                if (entryId !in map) {
                    map[entryId] = dictionaryRepository.getConjugationForms(entryId)
                        .distinctBy { it.groupKey to it.pronouns }
                }
            }
            map.also { formsByEntryId = it }
        }
}

suspend fun provisionNextConjugationBatch(
    dictionaryRepository: DictionaryRepository,
    learningRepository: LearningRepository
) = withContext(Dispatchers.Default) {
    val entryIdByInfinitive = ConjugationCache.getEntryIds(dictionaryRepository)
    val formsByEntryId = ConjugationCache.getForms(dictionaryRepository)

    val orderedBatches = GERMAN_TENSE_LIST.flatMap { tenseKey ->
        GERMAN_VERB_LEARNING_LIST.map { verb -> verb.infinitive to tenseKey }
    }

    var allGrades = learningRepository.getAllConjugationGrades()
    var lastBatchMastered = true

    for ((infinitive, tenseKey) in orderedBatches) {
        val entryId = entryIdByInfinitive[infinitive] ?: continue
        val forms = formsByEntryId[entryId]?.filter { it.groupKey == tenseKey }
        if (forms.isNullOrEmpty()) continue

        val keys = forms.map { f -> "$entryId|$tenseKey|${f.pronouns ?: ""}" }
        val anyInTable = keys.any { it in allGrades }

        if (!anyInTable) {
            if (lastBatchMastered) {
                for (key in keys) {
                    learningRepository.saveConjugationGrade(key, 1)
                }
                allGrades = learningRepository.getAllConjugationGrades()
            }
            break
        }

        val batchMastered = keys.all { (allGrades[it] ?: 0) >= 4 }
        if (!batchMastered) break

        lastBatchMastered = true
    }
}
