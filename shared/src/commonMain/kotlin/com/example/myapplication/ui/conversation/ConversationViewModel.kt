package com.example.myapplication.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AiService
import com.example.myapplication.data.ChatMessage
import com.example.myapplication.data.ConversationProgress
import com.example.myapplication.data.ConversationStore
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.data.forms.FormsConfigDe
import com.example.myapplication.ui.localeToFlag
import com.example.myapplication.ui.localeToName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HandsFreeState { OFF, AI_SPEAKING, LISTENING, SENDING }

data class UiMessage(
    val isUser: Boolean,
    val learnedText: String,
    val nativeText: String? = null
)

data class ConversationUiState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val learnedLanguage: String = "de",
    val nativeLanguage: String = "fr",
    val hasApiKey: Boolean = false,
    val started: Boolean = false,
    val handsFreeState: HandsFreeState = HandsFreeState.OFF
)

private const val INACTIVITY_TIMEOUT_MS = 2 * 60 * 1000L

class ConversationViewModel(
    private val aiService: AiService,
    private val conversationStore: ConversationStore,
    private val vocabularyRepository: VocabularyRepository,
    private val learningRepository: LearningRepository,
    private val dictionaryRepository: DictionaryRepository,
    val ttsPlayer: TtsPlayer,
    val speechRecognizer: SpeechRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<ChatMessage>()
    private var progress = ConversationProgress()
    private var weakWords: List<Pair<String, String>> = emptyList()
    private var activeTenses: List<String> = emptyList()
    private var inactivityJob: Job? = null
    private var conversationLevel: String = "A1"

    init {
        viewModelScope.launch {
            val nativeLang = withContext(Dispatchers.Default) {
                vocabularyRepository.getConfiguration("native_language") ?: "fr"
            }
            val learnedLang = withContext(Dispatchers.Default) {
                vocabularyRepository.getConfiguration("learned_language") ?: "de"
            }
            progress = withContext(Dispatchers.Default) { conversationStore.getProgress() }
            conversationLevel = withContext(Dispatchers.Default) { conversationStore.getLevel() }
            loadLearningContext(learnedLang, nativeLang)
            _uiState.value = ConversationUiState(
                learnedLanguage = learnedLang,
                nativeLanguage = nativeLang,
                hasApiKey = conversationStore.hasApiKey()
            )
        }
    }

    private suspend fun loadLearningContext(learnedLang: String, nativeLang: String) {
        withContext(Dispatchers.Default) {
            val lowRaws = learningRepository.getLowGradeRaws("word", learnedLang, nativeLang, maxGrade = 3)
            weakWords = lowRaws.take(20).mapNotNull { raw ->
                val translation = dictionaryRepository.getTranslationById(raw.key.toLong()) ?: return@mapNotNull null
                val entry = dictionaryRepository.getById(translation.entryId) ?: return@mapNotNull null
                entry.lemma to translation.text
            }

            val tenseLabels = FormsConfigDe.groups.associate { it.key to it.label }
            val conjGrades = learningRepository.getAllConjugationGrades()
            activeTenses = conjGrades.entries
                .filter { it.value < 4 }
                .mapNotNull { (key, _) -> key.split("|").getOrNull(1) }
                .distinct()
                .mapNotNull { tenseLabels[it] }
                .take(8)
        }
    }

    fun refreshApiKey() {
        conversationLevel = conversationStore.getLevel()
        _uiState.value = _uiState.value.copy(hasApiKey = conversationStore.hasApiKey())
    }

    fun startConversation() {
        viewModelScope.launch {
            progress = progress.copy(sessionsCount = progress.sessionsCount + 1)
            withContext(Dispatchers.Default) { conversationStore.saveProgress(progress) }
            _uiState.value = _uiState.value.copy(started = true)
            sendToAi("Commence la conversation. Salue l'apprenant et propose de pratiquer.")
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return
        val userMsg = UiMessage(isUser = true, learnedText = text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            error = null
        )
        history.add(ChatMessage("user", text))
        viewModelScope.launch { sendToAi(text, alreadyInHistory = true) }
    }

    // --- Hands-free mode ---

    fun toggleHandsFree() {
        val current = _uiState.value.handsFreeState
        if (current != HandsFreeState.OFF) {
            stopHandsFree()
        } else {
            val lastAiMsg = _uiState.value.messages.lastOrNull { !it.isUser }
            if (lastAiMsg != null) {
                speakAndThenListen(lastAiMsg.learnedText)
            } else {
                _uiState.value = _uiState.value.copy(handsFreeState = HandsFreeState.LISTENING)
                startListeningInternal()
            }
        }
    }

    private fun stopHandsFree() {
        inactivityJob?.cancel()
        ttsPlayer.stop()
        speechRecognizer.stopListening()
        _uiState.value = _uiState.value.copy(handsFreeState = HandsFreeState.OFF)
    }

    private fun speakAndThenListen(text: String) {
        _uiState.value = _uiState.value.copy(handsFreeState = HandsFreeState.AI_SPEAKING)
        resetInactivityTimer()
        ttsPlayer.speak(text, _uiState.value.learnedLanguage) {
            // onDone fires on TTS synthesis thread — dispatch to main before touching SpeechRecognizer
            viewModelScope.launch {
                if (_uiState.value.handsFreeState == HandsFreeState.AI_SPEAKING) {
                    _uiState.value = _uiState.value.copy(handsFreeState = HandsFreeState.LISTENING)
                    startListeningInternal()
                }
            }
        }
    }

    private fun startListeningInternal() {
        resetInactivityTimer()
        speechRecognizer.startListening(
            locale = _uiState.value.learnedLanguage,
            onResult = { text ->
                if (_uiState.value.handsFreeState == HandsFreeState.LISTENING) {
                    if (text.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(handsFreeState = HandsFreeState.SENDING)
                        inactivityJob?.cancel()
                        sendMessage(text)
                    } else {
                        startListeningInternal()
                    }
                }
            },
            onError = {
                if (_uiState.value.handsFreeState == HandsFreeState.LISTENING) {
                    startListeningInternal()
                }
            }
        )
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            if (_uiState.value.handsFreeState != HandsFreeState.OFF) stopHandsFree()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopHandsFree()
    }

    // --- AI interaction ---

    private suspend fun sendToAi(userText: String, alreadyInHistory: Boolean = false) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        if (!alreadyInHistory) history.add(ChatMessage("user", userText))

        try {
            val state = _uiState.value
            val providerType = withContext(Dispatchers.Default) { conversationStore.getProviderType() }
            val apiKey = withContext(Dispatchers.Default) { conversationStore.getApiKey() }
            val systemPrompt = buildSystemPrompt(state.learnedLanguage, state.nativeLanguage, progress)

            val reply = withContext(Dispatchers.IO) {
                aiService.complete(providerType, apiKey, systemPrompt, history.toList())
            }

            history.add(ChatMessage("assistant", reply))
            applyBilan(reply)

            val (learnedText, nativeText) = parseResponse(
                reply,
                localeToFlag(state.learnedLanguage),
                localeToFlag(state.nativeLanguage)
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + UiMessage(false, learnedText, nativeText),
                isLoading = false
            )

            if (_uiState.value.handsFreeState == HandsFreeState.SENDING) {
                speakAndThenListen(learnedText)
            } else {
                ttsPlayer.speak(learnedText, state.learnedLanguage)
            }
        } catch (e: Exception) {
            val wasHandsFree = _uiState.value.handsFreeState != HandsFreeState.OFF
            if (wasHandsFree) stopHandsFree()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Erreur inconnue"
            )
        }
    }

    private fun applyBilan(text: String) {
        val (words, errors, topics) = parseBilan(text) ?: return
        fun merge(a: List<String>, b: List<String>) = (a + b).distinct()
        progress = progress.copy(
            wordsLearned = merge(progress.wordsLearned, words),
            commonErrors = merge(progress.commonErrors, errors),
            topicsSeen = merge(progress.topicsSeen, topics),
            totalMessages = progress.totalMessages + 1,
            lastSession = conversationStore.todayString()
        )
        conversationStore.saveProgress(progress)
    }

    private fun parseResponse(text: String, learnedFlag: String, nativeFlag: String): Pair<String, String?> {
        val clean = text.replace(Regex("📊 BILAN:[\\s\\S]*$"), "").trim()
        val parts = clean.split(nativeFlag)
        return if (parts.size >= 2) {
            parts[0].removePrefix(learnedFlag).trim() to parts[1].trim()
        } else {
            clean.removePrefix(learnedFlag).trim() to null
        }
    }

    private fun parseBilan(text: String): Triple<List<String>, List<String>, List<String>>? {
        val match = Regex("📊 BILAN:([\\s\\S]*)$").find(text) ?: return null
        fun extract(key: String): List<String> {
            val r = Regex("$key=\\[([^\\]]*)\\]").find(match.groupValues[1]) ?: return emptyList()
            return r.groupValues[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return Triple(extract("mots_appris"), extract("erreurs_frequentes"), extract("sujets_vus"))
    }

    private fun buildSystemPrompt(learnedLang: String, nativeLang: String, @Suppress("UNUSED_PARAMETER") p: ConversationProgress): String {
        val learnedName = localeToName(learnedLang)
        val nativeName = localeToName(nativeLang)
        val learnedFlag = localeToFlag(learnedLang)
        val nativeFlag = localeToFlag(nativeLang)
        return buildString {
            appendLine("You are a friendly $conversationLevel-level $learnedName teacher. The student's native language is $nativeName.")
            appendLine("You can imagine a personal background story that you will reuse to make deeper conversation and bring personal point.")
            appendLine()
            appendLine("Rules:")
            appendLine("1. Use only $conversationLevel-level $learnedName (vocabulary and grammar appropriate for $conversationLevel)")
            appendLine("2. After each student response, give a brief correction if necessary")
            appendLine("4. Try to reuse what the student say to tell a personal story or point of view. The idea is to train the student understanding skills.")
            appendLine("3. Always ask a simple question at the end of answers to continue the conversation")
            appendLine("5. End with a short $nativeName translation")
            appendLine("6. If the student's message seems to contain a speech recognition error (phonetically similar word confusion), interpret the most likely intent before responding.")
            appendLine()
            appendLine("STRICT format — ALWAYS these 3 blocks:")
            appendLine("$learnedFlag [Your message in $learnedName]")
            appendLine("$nativeFlag [Short $nativeName translation]")
            appendLine("📊 BILAN: mots_appris=[word1,word2] erreurs_frequentes=[error1] sujets_vus=[topic1]")
            appendLine()
            appendLine("For BILAN: list ALL words seen cumulatively.")
            if (weakWords.isNotEmpty()) {
                appendLine("Words the student struggles with (grade < 4) — try to use and reinforce these:")
                appendLine(weakWords.joinToString(", ") { (lemma, trad) -> "$lemma ($trad)" })
            }
            if (activeTenses.isNotEmpty()) {
                appendLine("Conjugation tenses currently being practiced — favour these in examples:")
                appendLine(activeTenses.joinToString(", "))
            }
            if (p.wordsLearned.isNotEmpty()) appendLine("Known words: ${p.wordsLearned.joinToString(", ")}")
            if (p.commonErrors.isNotEmpty()) appendLine("Common errors: ${p.commonErrors.joinToString(", ")}")
            if (p.topicsSeen.isNotEmpty()) appendLine("Topics seen: ${p.topicsSeen.joinToString(", ")}")
            if (p.sessionsCount > 1) appendLine("This is session ${p.sessionsCount}. Welcome the student back warmly!")
            appendLine()
            append("Stay encouraging, patient and friendly!")
        }
    }
}
