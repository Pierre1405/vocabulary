package com.example.myapplication.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictEntry
import com.example.myapplication.data.DictTranslation
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.GERMAN_TENSE_LIST
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.forms.FormGroup
import com.example.myapplication.data.forms.FormRow
import com.example.myapplication.data.forms.getFormsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DictionaryDetailState(
    val entry: DictEntry? = null,
    val translations: List<DictTranslation> = emptyList(),
    val formGroups: List<FormGroup> = emptyList(),
    val wordGrades: Map<Long, Int> = emptyMap(),
    // key: "$entryId|$groupKey|$pronouns" → grade (only for groups in GERMAN_TENSE_LIST)
    val conjugationGrades: Map<String, Int> = emptyMap()
)

class DictionaryDetailViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val learningRepository: LearningRepository,
    private val entryId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(DictionaryDetailState())
    val state: StateFlow<DictionaryDetailState> = _state

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                val entry = dictionaryRepository.getById(entryId)
                val translations = dictionaryRepository.getTranslations(entryId)
                val forms = dictionaryRepository.getForms(entryId)
                val config = getFormsConfig(entry?.locale ?: "")
                val formGroups = buildFormGroups(forms.map { Triple(it.form, it.groupKey, it.pronouns) }, config, entry?.lemma ?: "", entry?.pos)
                val sourceLocale = entry?.locale ?: ""
                val targetLocale = translations.firstOrNull()?.targetLocale ?: ""
                val wordGrades = learningRepository.getWordGradesForTranslations(translations.map { it.id }, sourceLocale, targetLocale)
                val allConjGrades = learningRepository.getAllConjugationGrades()
                val prefix = "$entryId|"
                val conjugationGrades = allConjGrades.filterKeys { it.startsWith(prefix) }
                DictionaryDetailState(entry, translations, formGroups, wordGrades, conjugationGrades)
            }
        }
    }

    fun saveConjugationGroupGrade(groupKey: String, grade: Int) {
        viewModelScope.launch {
            val group = _state.value.formGroups.find { it.key == groupKey } ?: return@launch
            val newGrades = _state.value.conjugationGrades.toMutableMap()
            for (row in group.rows) {
                val pronouns = row.label ?: ""
                val key = "$entryId|$groupKey|$pronouns"
                learningRepository.saveConjugationGrade(key, grade)
                newGrades[key] = grade
            }
            _state.value = _state.value.copy(conjugationGrades = newGrades)
        }
    }

    fun saveWordGrade(translationId: Long, grade: Int) {
        viewModelScope.launch {
            val sourceLocale = _state.value.entry?.locale ?: return@launch
            val targetLocale = _state.value.translations.find { it.id == translationId }?.targetLocale ?: return@launch
            learningRepository.saveWordGrade(translationId, sourceLocale, targetLocale, grade)
            learningRepository.saveWordGrade(translationId, targetLocale, sourceLocale, grade)
            _state.value = _state.value.copy(wordGrades = _state.value.wordGrades + (translationId to grade))
        }
    }

}
