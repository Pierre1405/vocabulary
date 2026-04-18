package com.example.myapplication.ui

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
            _query
                .debounce(300)
                .collect { q -> search(q) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    private suspend fun search(q: String) {
        if (q.isBlank()) {
            _results.value = emptyList()
            return
        }
        _results.value = withContext(Dispatchers.Default) {
            // 1. Matchs exacts (lemme ou forme) dans les deux langues
            val exact = (
                dictionaryRepository.getByLemma(q, "de") +
                dictionaryRepository.getByLemma(q, "fr") +
                dictionaryRepository.searchExactByForm(q, "de") +
                dictionaryRepository.searchExactByForm(q, "fr")
            ).distinctBy { it.id }.sortedBy { it.lemma.lowercase() }

            val exactIds = exact.map { it.id }.toSet()

            // 2. Matchs partiels (%q%) sans les exacts déjà trouvés
            val partial = (
                dictionaryRepository.searchByPrefix("%$q%", "de") +
                dictionaryRepository.searchByPrefix("%$q%", "fr") +
                dictionaryRepository.searchByFormPattern("%$q%", "de") +
                dictionaryRepository.searchByFormPattern("%$q%", "fr")
            )
                .distinctBy { it.id }
                .filter { it.id !in exactIds }
                .sortedBy { it.lemma.lowercase() }

            (exact + partial).take(10).map { entry ->
                DictEntryWithTranslations(
                    entry = entry,
                    translations = dictionaryRepository.getTranslations(entry.id)
                )
            }
        }
    }
}
