package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.VocabularyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseListScreen(
    repository: VocabularyRepository,
    audioPlayer: AudioPlayer,
    storyId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PhraseViewModel = viewModel(key = storyId.toString()) {
        PhraseViewModel(repository, storyId)
    }
    val story by viewModel.story.collectAsState()
    val phrases by viewModel.phrases.collectAsState()
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val learnedLanguage by viewModel.learnedLanguage.collectAsState()

    var showNative by remember { mutableStateOf(false) }
    var showLearned by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = story?.getTranslation(nativeLanguage) ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = story?.getTranslation(learnedLanguage) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = showNative,
                        onClick = { showNative = !showNative },
                        label = { Text(localeToFlag(nativeLanguage)) }
                    )
                    FilterChip(
                        selected = showLearned,
                        onClick = { showLearned = !showLearned },
                        label = { Text(localeToFlag(learnedLanguage)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (phrases.isEmpty()) {
            Text(
                text = "Aucune phrase trouvée.",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(phrases) { phrase ->
                    PhraseCard(
                        phrase = phrase,
                        audioPlayer = audioPlayer,
                        nativeLanguage = nativeLanguage,
                        learnedLanguage = learnedLanguage,
                        showNative = showNative,
                        showLearned = showLearned
                    )
                }
            }
        }
    }
}

@Composable
fun PhraseCard(
    phrase: PhraseWithTranslations,
    audioPlayer: AudioPlayer,
    nativeLanguage: String,
    learnedLanguage: String,
    showNative: Boolean,
    showLearned: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).then(if (!showLearned) Modifier.blur(8.dp) else Modifier)) {
                Text(
                    text = phrase.getTranslation(learnedLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { audioPlayer.play(phrase.phraseId, learnedLanguage) }) {
                Text("▶", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).then(if (!showNative) Modifier.blur(8.dp) else Modifier)) {
                Text(
                    text = phrase.getTranslation(nativeLanguage),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
