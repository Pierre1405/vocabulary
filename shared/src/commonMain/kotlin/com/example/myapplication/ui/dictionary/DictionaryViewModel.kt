package com.example.myapplication.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictEntry
import com.example.myapplication.data.DictTranslation
import com.example.myapplication.data.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DictEntryWithTranslations(
    val entry: DictEntry,
    val translations: List<DictTranslation>
)

class DictionaryViewModel(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<DictEntryWithTranslations>>(emptyList())
    val results: StateFlow<List<DictEntryWithTranslations>> = _results

    init {
        viewModelScope.launch {
            _query.debounce(300).collect { q -> search(q) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    private suspend fun search(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) {
            _results.value = emptyList()
            return
        }
        _results.value = withContext(Dispatchers.Default) {
            val exact = (
                dictionaryRepository.getByLemma(trimmed, "de") +
                dictionaryRepository.getByLemma(trimmed, "fr") +
                dictionaryRepository.searchExactByForm(trimmed, "de") +
                dictionaryRepository.searchExactByForm(trimmed, "fr")
            ).distinctBy { it.id }.sortedWith(
                compareBy(
                    { if (it.lemma == trimmed) 0 else 1 }, // exact case d'abord
                    { it.lemma.lowercase() }
                )
            )

            val exactIds = exact.map { it.id }.toSet()

            val partial = (
                dictionaryRepository.searchByPrefix("%$trimmed%", "de") +
                dictionaryRepository.searchByPrefix("%$trimmed%", "fr") +
                dictionaryRepository.searchByFormPattern("%$trimmed%", "de") +
                dictionaryRepository.searchByFormPattern("%$trimmed%", "fr")
            )
                .distinctBy { it.id }
                .filter { it.id !in exactIds }
                .sortedBy { it.lemma.lowercase() }

            (exact + partial).take(10).map { entry ->
                DictEntryWithTranslations(entry = entry, translations = dictionaryRepository.getTranslations(entry.id))
            }
        }
    }
}
