package com.example.myapplication.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictEntry
import com.example.myapplication.data.DictTranslation
import com.example.myapplication.data.DictionaryRepository
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
    val wordGrades: Map<Long, Int> = emptyMap()
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
                DictionaryDetailState(entry, translations, formGroups, wordGrades)
            }
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
