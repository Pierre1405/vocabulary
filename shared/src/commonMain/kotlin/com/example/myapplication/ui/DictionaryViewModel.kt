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
            val deEntries = dictionaryRepository.searchByPrefix(q, "de")
            val frEntries = dictionaryRepository.searchByPrefix(q, "fr")
            (deEntries + frEntries)
                .sortedBy { it.lemma.lowercase() }
                .take(10)
                .map { entry ->
                    DictEntryWithTranslations(
                        entry = entry,
                        translations = dictionaryRepository.getTranslations(entry.id)
                    )
                }
        }
    }
}
