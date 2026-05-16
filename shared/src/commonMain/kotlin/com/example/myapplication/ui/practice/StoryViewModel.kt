package com.example.myapplication.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.UpcomingGroup
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.data.forms.FormsConfigDe
import com.example.myapplication.data.provisionNextConjugationBatch
import com.example.myapplication.ui.review.definiteArticle
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

data class LowGradeWordItem(
    val lemmaWithArticle: String,
    val translationWithArticle: String,
    val wordLocale: String,
    val translationLocale: String,
    val grade: Int
)

data class LowGradeSentenceItem(
    val sourceText: String,
    val targetText: String,
    val sourceLocale: String,
    val targetLocale: String,
    val grade: Int
)

data class LowGradeConjugationItem(
    val lemma: String,
    val tenseLabel: String,
    val pronouns: String?,
    val grade: Int
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

    private val _lowGradeWords = MutableStateFlow<List<LowGradeWordItem>>(emptyList())
    val lowGradeWords: StateFlow<List<LowGradeWordItem>> = _lowGradeWords

    private val _lowGradeSentences = MutableStateFlow<List<LowGradeSentenceItem>>(emptyList())
    val lowGradeSentences: StateFlow<List<LowGradeSentenceItem>> = _lowGradeSentences

    private val _lowGradeConjugations = MutableStateFlow<List<LowGradeConjugationItem>>(emptyList())
    val lowGradeConjugations: StateFlow<List<LowGradeConjugationItem>> = _lowGradeConjugations

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
            if (dictionaryRepository != null) {
                provisionNextConjugationBatch(dictionaryRepository, learningRepository)
            }
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

                // Low grade items (grade <= 2)
                val tenseLabels = FormsConfigDe.groups.associate { it.key to it.label }

                val lowWordRaws = learningRepository.getLowGradeRaws("word", nativeLang, learnedLang) +
                    learningRepository.getLowGradeRaws("word", learnedLang, nativeLang)
                _lowGradeWords.value = withContext(Dispatchers.Default) {
                    lowWordRaws.distinctBy { it.key }.mapNotNull { raw ->
                        val translation = dictionaryRepository.getTranslationById(raw.key.toLongOrNull() ?: return@mapNotNull null) ?: return@mapNotNull null
                        val entry = dictionaryRepository.getById(translation.entryId) ?: return@mapNotNull null
                        val article = definiteArticle(entry.locale, entry.pos, entry.gender, entry.lemma)
                        val transEntry = dictionaryRepository.getByLemma(translation.text, translation.targetLocale).firstOrNull()
                        val transArticle = definiteArticle(translation.targetLocale, transEntry?.pos, transEntry?.gender, translation.text)
                        LowGradeWordItem(
                            lemmaWithArticle = if (article != null) "$article ${entry.lemma}" else entry.lemma,
                            translationWithArticle = if (transArticle != null) "$transArticle${if (transArticle.endsWith("'")) "" else " "}${translation.text}" else translation.text,
                            wordLocale = entry.locale,
                            translationLocale = translation.targetLocale,
                            grade = raw.grade
                        )
                    }
                }

                val lowSentenceRaws = learningRepository.getLowGradeRaws("sentence", nativeLang, learnedLang) +
                    learningRepository.getLowGradeRaws("sentence", learnedLang, nativeLang)
                val distinctSentenceRaws = lowSentenceRaws.distinctBy { it.key }
                val sentenceKeys = distinctSentenceRaws.map { it.key }
                val gradeByKey = distinctSentenceRaws.associate { it.key to it.grade }
                val translations = repository.getTranslationsForSentences(sentenceKeys)
                val translationsByKey = translations.groupBy { it.sentence_key }
                _lowGradeSentences.value = withContext(Dispatchers.Default) {
                    distinctSentenceRaws.mapNotNull { raw ->
                        val trans = translationsByKey[raw.key] ?: return@mapNotNull null
                        val srcText = trans.firstOrNull { it.locale == nativeLang }?.translation ?: return@mapNotNull null
                        val tgtText = trans.firstOrNull { it.locale == learnedLang }?.translation ?: return@mapNotNull null
                        LowGradeSentenceItem(
                            sourceText = srcText,
                            targetText = tgtText,
                            sourceLocale = nativeLang,
                            targetLocale = learnedLang,
                            grade = gradeByKey[raw.key] ?: raw.grade
                        )
                    }
                }

                val lowConjRaws = learningRepository.getLowGradeRaws("conjugation", "de", "de")
                _lowGradeConjugations.value = withContext(Dispatchers.Default) {
                    lowConjRaws.mapNotNull { raw ->
                        val parts = raw.key.split("|")
                        if (parts.size != 3) return@mapNotNull null
                        val entryId = parts[0].toLongOrNull() ?: return@mapNotNull null
                        val tenseKey = parts[1]
                        val pronounsStr = parts[2].ifEmpty { null }
                        val entry = dictionaryRepository.getById(entryId) ?: return@mapNotNull null
                        LowGradeConjugationItem(
                            lemma = entry.lemma,
                            tenseLabel = tenseLabels[tenseKey] ?: tenseKey,
                            pronouns = pronounsStr,
                            grade = raw.grade
                        )
                    }
                }
            }
        }
    }
}
