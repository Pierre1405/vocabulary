package com.example.myapplication.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun definiteArticle(locale: String, pos: String?, gender: String?, word: String = ""): String? {
    if (pos != "noun" || gender == null) return null
    return when (locale) {
        "de" -> when (gender) {
            "masculine" -> "der"
            "feminine"  -> "die"
            "neuter"    -> "das"
            else        -> null
        }
        "fr" -> {
            val startsWithVowelOrH = word.firstOrNull()?.lowercaseChar()?.let {
                it in "aeiouyhàâéèêëîïôùûü"
            } ?: false
            when {
                startsWithVowelOrH  -> "l'"
                gender == "masculine" -> "le"
                gender == "feminine"  -> "la"
                else -> null
            }
        }
        else -> null
    }
}

data class WordReviewItem(
    val entryId: Long,
    val translationId: Long,
    val lemma: String,
    val lemmaWithArticle: String,
    val wordLocale: String,
    val pos: String?,
    val gender: String?,
    val translationText: String,
    val translationWithArticle: String,
    val translationLocale: String
)

class WordReviewViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val learningRepository: LearningRepository,
    val sourceLocale: String,
    val targetLocale: String
) : ViewModel() {

    private val _items = MutableStateFlow<List<WordReviewItem>>(emptyList())
    val items: StateFlow<List<WordReviewItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentGrade = MutableStateFlow<Int?>(null)
    val currentGrade: StateFlow<Int?> = _currentGrade

    private val grades = mutableMapOf<Long, Int>()
    private val sessionGrades = mutableMapOf<Long, Int>()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    private val _showPreview = MutableStateFlow(false)
    val showPreview: StateFlow<Boolean> = _showPreview

    private val _previewItems = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val previewItems: StateFlow<List<Pair<String, String>>> = _previewItems

    fun dismissPreview() { _showPreview.value = false }

    init {
        viewModelScope.launch {
            val wordLearning = learningRepository.getAllWordsByDirection(sourceLocale, targetLocale)
            _items.value = withContext(Dispatchers.Default) {
                wordLearning.shuffled().mapNotNull { (translationId, grade) ->
                    grades[translationId] = grade
                    val translation = dictionaryRepository.getTranslationById(translationId) ?: return@mapNotNull null
                    val entry = dictionaryRepository.getById(translation.entryId) ?: return@mapNotNull null
                    val article = definiteArticle(entry.locale, entry.pos, entry.gender, entry.lemma)
                    val transEntry = dictionaryRepository.getByLemma(translation.text, translation.targetLocale).firstOrNull()
                    val transArticle = definiteArticle(translation.targetLocale, transEntry?.pos, transEntry?.gender, translation.text)
                    WordReviewItem(
                        entryId = entry.id,
                        translationId = translationId,
                        lemma = entry.lemma,
                        lemmaWithArticle = if (article != null) "$article ${entry.lemma}" else entry.lemma,
                        wordLocale = entry.locale,
                        pos = entry.pos,
                        gender = entry.gender,
                        translationText = translation.text,
                        translationWithArticle = if (transArticle != null) "$transArticle${if (transArticle.endsWith("'")) "" else " "}${translation.text}" else translation.text,
                        translationLocale = translation.targetLocale
                    )
                }
            }
            val grade1 = _items.value
                .filter { grades[it.translationId] == 1 }
                .map { it.lemma to it.translationText }
            _previewItems.value = grade1
            if (grade1.isNotEmpty()) _showPreview.value = true
            updateCurrentGrade()
        }
    }

    private fun updateCurrentGrade() {
        val item = _items.value.getOrNull(_currentIndex.value)
        _currentGrade.value = item?.let { grades[it.translationId] }
    }

    fun saveGrade(translationId: Long, grade: Int) {
        viewModelScope.launch {
            learningRepository.saveWordGrade(translationId, sourceLocale, targetLocale, grade)
            grades[translationId] = grade
            sessionGrades[translationId] = grade
            val size = _items.value.size
            if (size == 0) return@launch
            val next = _currentIndex.value + 1
            if (next >= size) _isCompleted.value = true
            else { _currentIndex.value = next; updateCurrentGrade() }
        }
    }

    fun navigateTo(index: Int) {
        _currentIndex.value = index
        updateCurrentGrade()
    }

    fun moveToNext() {
        val size = _items.value.size
        if (size == 0) return
        val next = _currentIndex.value + 1
        if (next >= size) _isCompleted.value = true
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
            learningRepository.deleteWordGrade(item.translationId, sourceLocale, targetLocale)
            grades.remove(item.translationId)
            val newList = list.toMutableList().also { it.removeAt(index) }
            _items.value = newList
            if (newList.isEmpty()) { _isCompleted.value = true; return@launch }
            _currentIndex.value = index.coerceAtMost(newList.size - 1)
            updateCurrentGrade()
        }
    }

    fun restart(filterLowGrades: Boolean) {
        val list = if (filterLowGrades)
            _items.value.filter { (sessionGrades[it.translationId] ?: Int.MAX_VALUE) < 3 }
        else
            _items.value
        sessionGrades.clear()
        _items.value = list
        _currentIndex.value = 0
        _isCompleted.value = false
        updateCurrentGrade()
    }
}
