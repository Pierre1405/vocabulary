package com.example.myapplication.ui.conjugation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.GERMAN_VERB_LEARNING_LIST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConjugationViewModel(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _entryIdByInfinitive = MutableStateFlow<Map<String, Long>>(emptyMap())
    val entryIdByInfinitive: StateFlow<Map<String, Long>> = _entryIdByInfinitive

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            val map = withContext(Dispatchers.Default) {
                val result = mutableMapOf<String, Long>()
                for (verb in GERMAN_VERB_LEARNING_LIST) {
                    val lemma = verb.infinitive.removePrefix("sich ")
                    dictionaryRepository.getByLemma(lemma, "de").firstOrNull()
                        ?.let { result[verb.infinitive] = it.id }
                }
                result
            }
            _entryIdByInfinitive.value = map
            _isLoading.value = false
        }
    }
}
