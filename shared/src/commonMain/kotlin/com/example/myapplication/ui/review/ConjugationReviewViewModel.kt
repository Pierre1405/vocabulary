package com.example.myapplication.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ConjugationCache
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
        try {
            val sessionItems = withContext(Dispatchers.Default) {
                val tenseLabels = FormsConfigDe.groups.associate { it.key to it.label }

                val entryIdByInfinitive = ConjugationCache.getEntryIds(dictionaryRepository)
                val formsByEntryId = ConjugationCache.getForms(dictionaryRepository)

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
            updateCurrentGrade()
        } finally {
            _isLoading.value = false
        }
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

    fun deleteCurrent() {
        viewModelScope.launch {
            val list = _items.value
            val index = _currentIndex.value
            val item = list.getOrNull(index) ?: return@launch
            val parts = item.key.split("|")
            val entryId = parts[0].toLongOrNull() ?: return@launch
            val groupKey = parts[1]
            learningRepository.deleteConjugationGroup(entryId, groupKey)
            val prefix = "$entryId|$groupKey|"
            val newList = list.filter { !it.key.startsWith(prefix) }
            _items.value = newList
            if (newList.isEmpty()) { _isCompleted.value = true; return@launch }
            _currentIndex.value = index.coerceAtMost(newList.size - 1)
            updateCurrentGrade()
        }
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
