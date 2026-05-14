package com.example.myapplication.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AiService
import com.example.myapplication.data.ConversationStore
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.LocalStrings
import com.example.myapplication.ui.localeToFlag

private val TOPICS = listOf(
    "👋" to "Stell dich vor! Wie heißt du?",
    "🏠" to "Beschreib dein Haus.",
    "🍕" to "Was isst du gern?",
    "🌤️" to "Wie ist das Wetter?",
    "👨‍👩‍👧" to "Erzähl mir von deiner Familie!",
    "🎯" to "Was machst du in deiner Freizeit?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    aiService: AiService,
    conversationStore: ConversationStore,
    vocabularyRepository: VocabularyRepository,
    learningRepository: LearningRepository,
    dictionaryRepository: DictionaryRepository,
    onSettingsClick: () -> Unit,
    onWordClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val vm: ConversationViewModel = viewModel {
        ConversationViewModel(aiService, conversationStore, vocabularyRepository, learningRepository, dictionaryRepository)
    }
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.conversationTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                },
                actions = {
                    IconButton(onClick = { onSettingsClick(); vm.refreshApiKey() }) {
                        Icon(Icons.Filled.Settings, contentDescription = LocalStrings.current.settingsTitle)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (!state.hasApiKey) {
                NoApiKeyBanner(onSettingsClick = { onSettingsClick(); vm.refreshApiKey() })
            } else if (!state.started) {
                StartBanner(
                    learnedLanguage = state.learnedLanguage,
                    nativeLanguage = state.nativeLanguage,
                    onStart = { vm.startConversation() }
                )
            } else {
                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages) { msg -> MessageBubble(msg, onWordClick) }
                    if (state.isLoading) {
                        item { TypingIndicator() }
                    }
                }

                state.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Topic chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TOPICS) { (emoji, prompt) ->
                        FilterChip(
                            selected = false,
                            enabled = !state.isLoading,
                            onClick = { vm.sendMessage(prompt) },
                            label = { Text(emoji) }
                        )
                    }
                }

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(LocalStrings.current.conversationInputHint) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { vm.sendMessage(inputText); inputText = "" },
                        enabled = inputText.isNotBlank() && !state.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: UiMessage, onWordClick: (String) -> Unit) {
    val isUser = msg.isUser
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(msg.learnedText, style = MaterialTheme.typography.bodyMedium)
                } else {
                    ClickableWords(
                        text = msg.learnedText,
                        style = MaterialTheme.typography.bodyMedium,
                        linkColor = MaterialTheme.colorScheme.primary,
                        onWordClick = onWordClick
                    )
                }
                if (msg.nativeText != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 0.5.dp)
                    Text(
                        text = msg.nativeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickableWords(
    text: String,
    style: TextStyle,
    linkColor: androidx.compose.ui.graphics.Color,
    onWordClick: (String) -> Unit
) {
    val wordPattern = Regex("[^\\s]+")
    val annotated = buildAnnotatedString {
        var lastEnd = 0
        wordPattern.findAll(text).forEach { match ->
            if (match.range.first > lastEnd) append(text.substring(lastEnd, match.range.first))
            val token = match.value
            val word = token.trim('.', ',', '!', '?', ';', ':', '"', '\'', ')', ']', '(', '[', '…')
            if (word.isNotEmpty()) {
                pushStringAnnotation("WORD", word)
                pushStyle(SpanStyle(color = linkColor))
                append(token)
                pop()
                pop()
            } else {
                append(token)
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) append(text.substring(lastEnd))
    }
    ClickableText(
        text = annotated,
        style = style,
        onClick = { offset ->
            annotated.getStringAnnotations("WORD", offset, offset)
                .firstOrNull()?.let { onWordClick(it.item) }
        }
    )
}

@Composable
private fun TypingIndicator() {
    Box(modifier = Modifier.padding(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun NoApiKeyBanner(onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔑", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.size(16.dp))
        Text(
            LocalStrings.current.conversationNoKey,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onSettingsClick) {
            Text(LocalStrings.current.settingsTitle)
        }
    }
}

@Composable
private fun StartBanner(learnedLanguage: String, nativeLanguage: String, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${localeToFlag(learnedLanguage)} ↔ ${localeToFlag(nativeLanguage)}",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.size(16.dp))
        Text(
            LocalStrings.current.conversationStart,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(24.dp))
        Button(onClick = onStart) {
            Text(LocalStrings.current.conversationStartBtn)
        }
    }
}
