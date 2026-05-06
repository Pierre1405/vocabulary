package com.example.myapplication.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.UpcomingGroup
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.data.forms.FormsConfigDe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpcomingWordItem(
    val sourceText: String,
    val targetText: String,
    val sourceLocale: String,
    val targetLocale: String,
    val hoursUntilDue: Long
)

data class UpcomingConjugationItem(
    val lemma: String,
    val tenseLabel: String,
    val pronouns: String?,
    val grade: Int,
    val hoursUntilDue: Long
)

class StoryViewModel(
    private val repository: VocabularyRepository,
    private val learningRepository: LearningRepository,
    private val dictionaryRepository: DictionaryRepository? = null
) : ViewModel() {

    private val _stories = MutableStateFlow<List<StoryWithTranslations>>(emptyList())
    val stories: StateFlow<List<StoryWithTranslations>> = _stories

    private val _nativeLanguage = MutableStateFlow("fr")
    val nativeLanguage: StateFlow<String> = _nativeLanguage

    private val _learnedLanguage = MutableStateFlow("de")
    val learnedLanguage: StateFlow<String> = _learnedLanguage

    private val _countNativeToLearned = MutableStateFlow(0L)
    val countNativeToLearned: StateFlow<Long> = _countNativeToLearned

    private val _countLearnedToNative = MutableStateFlow(0L)
    val countLearnedToNative: StateFlow<Long> = _countLearnedToNative

    private val _countWordLearnedToNative = MutableStateFlow(0L)
    val countWordLearnedToNative: StateFlow<Long> = _countWordLearnedToNative

    private val _countWordNativeToLearned = MutableStateFlow(0L)
    val countWordNativeToLearned: StateFlow<Long> = _countWordNativeToLearned

    private val _countConjugation = MutableStateFlow(0L)
    val countConjugation: StateFlow<Long> = _countConjugation

    private val _upcomingGroups = MutableStateFlow<List<UpcomingGroup>>(emptyList())
    val upcomingGroups: StateFlow<List<UpcomingGroup>> = _upcomingGroups

    private val _upcomingWordItems = MutableStateFlow<List<UpcomingWordItem>>(emptyList())
    val upcomingWordItems: StateFlow<List<UpcomingWordItem>> = _upcomingWordItems

    private val _upcomingConjugationItems = MutableStateFlow<List<UpcomingConjugationItem>>(emptyList())
    val upcomingConjugationItems: StateFlow<List<UpcomingConjugationItem>> = _upcomingConjugationItems

    init {
        viewModelScope.launch {
            val nativeLang = repository.getConfiguration("native_language") ?: "fr"
            val learnedLang = repository.getConfiguration("learned_language") ?: "de"
            _nativeLanguage.value = nativeLang
            _learnedLanguage.value = learnedLang

            val stories = repository.getAllStories()
            val allTranslations = repository.getAllStoryTranslations()
            val translationsByStoryId = allTranslations.groupBy { it.story_id }

            _stories.value = stories.map { storyId ->
                StoryWithTranslations(
                    storyId = storyId,
                    translations = translationsByStoryId[storyId]
                        ?.associate { it.locale to it.translation } ?: emptyMap()
                )
            }

            refreshCounts()
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            // Advance the virtual clock before querying due counts.
            // See LearningRepository for the anti-overwhelm mechanism.
            learningRepository.updateUsageTime()
            val nativeLang = _nativeLanguage.value
            val learnedLang = _learnedLanguage.value
            _countNativeToLearned.value = learningRepository.countByDirection(nativeLang, learnedLang)
            _countLearnedToNative.value = learningRepository.countByDirection(learnedLang, nativeLang)
            _countWordLearnedToNative.value = learningRepository.countWordsByDirection(learnedLang, nativeLang)
            _countWordNativeToLearned.value = learningRepository.countWordsByDirection(nativeLang, learnedLang)
            _countConjugation.value = learningRepository.countConjugationDue()
            _upcomingGroups.value = learningRepository.getUpcomingGroups()
            if (dictionaryRepository != null) {
                val raws = learningRepository.getUpcomingWordRaws()
                _upcomingWordItems.value = withContext(Dispatchers.Default) {
                    raws.mapNotNull { raw ->
                        val translation = dictionaryRepository.getTranslationById(raw.translationId)
                            ?: return@mapNotNull null
                        val entry = dictionaryRepository.getById(translation.entryId)
                            ?: return@mapNotNull null
                        val isReversed = entry.locale != raw.sourceLocale
                        UpcomingWordItem(
                            sourceText = if (isReversed) translation.text else entry.lemma,
                            targetText = if (isReversed) entry.lemma else translation.text,
                            sourceLocale = raw.sourceLocale,
                            targetLocale = raw.targetLocale,
                            hoursUntilDue = raw.hoursUntilDue
                        )
                    }
                }

                val conjRaws = learningRepository.getUpcomingConjugationRaws()
                _upcomingConjugationItems.value = withContext(Dispatchers.Default) {
                    val tenseLabels = FormsConfigDe.groups.associate { it.key to it.label }
                    conjRaws.mapNotNull { raw ->
                        val parts = raw.key.split("|")
                        if (parts.size != 3) return@mapNotNull null
                        val entryId = parts[0].toLongOrNull() ?: return@mapNotNull null
                        val tenseKey = parts[1]
                        val pronounsStr = parts[2].ifEmpty { null }
                        val entry = dictionaryRepository.getById(entryId) ?: return@mapNotNull null
                        UpcomingConjugationItem(
                            lemma = entry.lemma,
                            tenseLabel = tenseLabels[tenseKey] ?: tenseKey,
                            pronouns = pronounsStr,
                            grade = raw.grade,
                            hoursUntilDue = raw.hoursUntilDue
                        )
                    }
                }
            }
        }
    }
}
