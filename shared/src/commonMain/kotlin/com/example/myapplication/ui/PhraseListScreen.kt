package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = story?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
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
                    PhraseCard(phrase = phrase, audioPlayer = audioPlayer, nativeLanguage = nativeLanguage, learnedLanguage = learnedLanguage)
                }
            }
        }
    }
}

@Composable
fun PhraseCard(phrase: PhraseWithTranslations, audioPlayer: AudioPlayer, nativeLanguage: String, learnedLanguage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phrase.getTranslation(nativeLanguage),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { audioPlayer.play(phrase.phraseId, nativeLanguage) }) {
                Text("▶", style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phrase.getTranslation(learnedLanguage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { audioPlayer.play(phrase.phraseId, learnedLanguage) }) {
                Text("▶", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
