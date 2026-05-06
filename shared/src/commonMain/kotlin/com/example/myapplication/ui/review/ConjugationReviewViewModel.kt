package com.example.myapplication.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.GERMAN_TENSE_LIST
import com.example.myapplication.data.GERMAN_VERB_LEARNING_LIST
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.forms.FormsConfigDe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConjugationReviewItem(
    val key: String,           // "$entryId|$groupKey|$pronouns"
    val lemma: String,         // "lieben"
    val tenseLabel: String,    // "Präsens"
    val pronouns: String?,     // "ich" / null for participles
    val expectedForm: String,  // "liebe"
    val grade: Int
)

class ConjugationReviewViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val learningRepository: LearningRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<ConjugationReviewItem>>(emptyList())
    val items: StateFlow<List<ConjugationReviewItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentGrade = MutableStateFlow<Int?>(null)
    val currentGrade: StateFlow<Int?> = _currentGrade

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    private val grades = mutableMapOf<String, Int>()
    private val sessionGrades = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val sessionItems = withContext(Dispatchers.Default) {
            val tenseLabels = FormsConfigDe.groups.associate { it.key to it.label }
            val tenseKeySet = GERMAN_TENSE_LIST.toHashSet()

            // Resolve entryId for each verb (strip "sich " for reflexives)
            val entryIdByInfinitive = mutableMapOf<String, Long>()
            for (verb in GERMAN_VERB_LEARNING_LIST) {
                val lemma = verb.infinitive.removePrefix("sich ")
                dictionaryRepository.getByLemma(lemma, "de").firstOrNull()
                    ?.let { entryIdByInfinitive[verb.infinitive] = it.id }
            }

            // Load all relevant forms per entryId (one pass, one slot per pronoun+tense)
            val formsByEntryId = mutableMapOf<Long, List<com.example.myapplication.data.DictForm>>()
            for (verb in GERMAN_VERB_LEARNING_LIST) {
                val entryId = entryIdByInfinitive[verb.infinitive] ?: continue
                if (entryId !in formsByEntryId) {
                    formsByEntryId[entryId] = dictionaryRepository.getForms(entryId)
                        .filter { it.groupKey != null && it.groupKey in tenseKeySet }
                        .distinctBy { it.groupKey to it.pronouns }
                }
            }

            // --- Auto-provisioning -------------------------------------------
            // Ordered batches: all verbs at tense0, then all verbs at tense1, ...
            val orderedBatches = GERMAN_TENSE_LIST.flatMap { tenseKey ->
                GERMAN_VERB_LEARNING_LIST.map { verb -> verb.infinitive to tenseKey }
            }

            var allGrades = learningRepository.getAllConjugationGrades()
            var lastBatchMastered = true  // true = no previous batch

            for ((infinitive, tenseKey) in orderedBatches) {
                val entryId = entryIdByInfinitive[infinitive] ?: continue
                val forms = formsByEntryId[entryId]?.filter { it.groupKey == tenseKey }
                if (forms.isNullOrEmpty()) continue  // no forms in dict → skip

                val keys = forms.map { f -> "$entryId|$tenseKey|${f.pronouns ?: ""}" }
                val anyInTable = keys.any { it in allGrades }

                if (!anyInTable) {
                    // First locked batch found: provision if previous was mastered
                    if (lastBatchMastered) {
                        for (key in keys) {
                            learningRepository.saveConjugationGrade(key, 1)
                        }
                        allGrades = learningRepository.getAllConjugationGrades()
                    }
                    break
                }

                val batchMastered = keys.all { (allGrades[it] ?: 0) >= 4 }
                if (!batchMastered) break  // still working on this batch

                lastBatchMastered = true  // mastered, continue to next
            }
            // -----------------------------------------------------------------

            // Build session from due items only
            val dueGrades = learningRepository.getDueConjugationGrades()
            val dueItems = mutableListOf<ConjugationReviewItem>()

            for (verb in GERMAN_VERB_LEARNING_LIST) {
                val entryId = entryIdByInfinitive[verb.infinitive] ?: continue
                for (tenseKey in GERMAN_TENSE_LIST) {
                    val forms = formsByEntryId[entryId]?.filter { it.groupKey == tenseKey }
                        ?: continue
                    for (form in forms) {
                        val key = "$entryId|$tenseKey|${form.pronouns ?: ""}"
                        if (key in dueGrades) {
                            dueItems.add(
                                ConjugationReviewItem(
                                    key = key,
                                    lemma = verb.infinitive,
                                    tenseLabel = tenseLabels[tenseKey] ?: tenseKey,
                                    pronouns = form.pronouns,
                                    expectedForm = form.form,
                                    grade = dueGrades[key] ?: 1
                                )
                            )
                        }
                    }
                }
            }

            dueItems.shuffled()
        }

        sessionItems.forEach { grades[it.key] = it.grade }
        _items.value = sessionItems
        _isLoading.value = false
        updateCurrentGrade()
    }

    private fun updateCurrentGrade() {
        _currentGrade.value = _items.value.getOrNull(_currentIndex.value)?.let { grades[it.key] }
    }

    fun saveGrade(key: String, grade: Int) {
        viewModelScope.launch {
            learningRepository.saveConjugationGrade(key, grade)
            grades[key] = grade
            sessionGrades[key] = grade
            val next = _currentIndex.value + 1
            if (next >= _items.value.size) _isCompleted.value = true
            else { _currentIndex.value = next; updateCurrentGrade() }
        }
    }

    fun moveToNext() {
        val next = _currentIndex.value + 1
        if (next >= _items.value.size) _isCompleted.value = true
        else { _currentIndex.value = next; updateCurrentGrade() }
    }

    fun moveToPrevious() {
        val size = _items.value.size
        if (size == 0) return
        _currentIndex.value = (_currentIndex.value - 1 + size) % size
        updateCurrentGrade()
    }

    fun restart(filterLowGrades: Boolean) {
        val list = if (filterLowGrades)
            _items.value.filter { (sessionGrades[it.key] ?: Int.MAX_VALUE) < 3 }
        else
            _items.value
        sessionGrades.clear()
        _items.value = list
        _currentIndex.value = 0
        _isCompleted.value = false
        updateCurrentGrade()
    }
}
