package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
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
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    repository: VocabularyRepository,
    audioPlayer: AudioPlayer,
    speechRecognizer: SpeechRecognizer,
    sourceLocale: String,
    targetLocale: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReviewViewModel = viewModel(key = "$sourceLocale-$targetLocale") {
        ReviewViewModel(repository, sourceLocale, targetLocale)
    }

    val sentences by viewModel.sentences.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${localeToFlag(sourceLocale)} → ${localeToFlag(targetLocale)}" +
                        if (sentences.isNotEmpty()) "  ${currentIndex + 1} / ${sentences.size}" else ""
                    )
                },
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
        if (sentences.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune phrase à réviser.")
            }
        } else {
            val sentence = sentences[currentIndex]
            ReviewCard(
                sentence = sentence,
                sourceLocale = sourceLocale,
                targetLocale = targetLocale,
                speechRecognizer = speechRecognizer,
                onNext = { viewModel.moveToNext() },
                onPrevious = { viewModel.moveToPrevious() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun ReviewCard(
    sentence: SentenceWithTranslations,
    sourceLocale: String,
    targetLocale: String,
    speechRecognizer: SpeechRecognizer,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTarget by remember(sentence.sentenceId) { mutableStateOf(false) }
    var spokenText by remember(sentence.sentenceId) { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                // Phrase source (toujours visible)
                Text(
                    text = sentence.getTranslation(sourceLocale),
                    style = MaterialTheme.typography.bodyLarge
                )

                HorizontalDivider()

                // Phrase cible (floue, clic pour révéler)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTarget = !showTarget }
                        .then(if (!showTarget) Modifier.blur(8.dp) else Modifier)
                ) {
                    Text(
                        text = sentence.getTranslation(targetLocale),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Zone de saisie avec bouton micro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = spokenText,
                onValueChange = { spokenText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("${localeToFlag(targetLocale)} Prononcez ou écrivez...") },
                singleLine = false,
                maxLines = 3
            )
            IconButton(onClick = {
                if (isListening) {
                    speechRecognizer.stopListening()
                    isListening = false
                } else {
                    isListening = true
                    speechRecognizer.startListening(
                        locale = targetLocale,
                        onResult = { result ->
                            spokenText = result
                            isListening = false
                        },
                        onError = { isListening = false }
                    )
                }
            }) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isListening) "Arrêter" else "Écouter",
                    tint = if (isListening) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Navigation précédent / suivant
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Précédent")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("Suivant →")
            }
        }
    }
}
