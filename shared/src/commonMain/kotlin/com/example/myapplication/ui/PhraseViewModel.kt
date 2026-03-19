package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.db.Phrases
import com.example.myapplication.db.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhraseViewModel(
    private val repository: VocabularyRepository,
    private val storyId: Long
) : ViewModel() {

    private val _story = MutableStateFlow<Story?>(null)
    val story: StateFlow<Story?> = _story

    private val _phrases = MutableStateFlow<List<Phrases>>(emptyList())
    val phrases: StateFlow<List<Phrases>> = _phrases

    init {
        viewModelScope.launch {
            _story.value = repository.getStoryById(storyId)
            _phrases.value = repository.getPhrasesByStory(storyId)
        }
    }
}
