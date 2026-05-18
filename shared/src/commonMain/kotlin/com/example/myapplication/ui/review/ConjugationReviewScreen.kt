package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.ui.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConjugationReviewScreen(
    dictionaryRepository: DictionaryRepository,
    learningRepository: LearningRepository,
    ttsPlayer: TtsPlayer,
    speechRecognizer: SpeechRecognizer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ConjugationReviewViewModel = viewModel {
        ConjugationReviewViewModel(dictionaryRepository, learningRepository)
    }

    val items by viewModel.items.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentGrade by viewModel.currentGrade.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val strings = LocalStrings.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.reviewConjugation +
                        if (items.isNotEmpty()) "  ${currentIndex + 1} / ${items.size}" else ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (items.isNotEmpty() && !isCompleted) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.reviewDelete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(strings.reviewDeleteConfirmTitle) },
                text = { Text(strings.reviewDeleteConfirmMessage) },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false; viewModel.deleteCurrent() }) {
                        Text(strings.reviewDeleteConfirm, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(strings.reviewDeleteCancel) }
                }
            )
        }
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            isCompleted -> ReviewCompletionScreen(
                onRestart = { viewModel.restart(filterLowGrades = false) },
                onRestartLowGrades = { viewModel.restart(filterLowGrades = true) },
                onFinish = onBack,
                modifier = Modifier.padding(innerPadding)
            )

            items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(strings.reviewEmpty) }

            else -> {
                val item = items[currentIndex]
                val sourceText = buildString {
                    append(item.lemma)
                    append("\n")
                    append(item.tenseLabel)
                    if (item.pronouns != null) append(" · ${item.pronouns}")
                }
                val targetText = if (item.pronouns != null) "${item.pronouns} ${item.expectedForm}" else item.expectedForm
                ReviewCard(
                    itemKey = item.key,
                    sourceText = sourceText,
                    targetText = targetText,
                    targetLocale = "de",
                    currentGrade = currentGrade,
                    onPlayTarget = { ttsPlayer.speak(item.expectedForm, "de") },
                    speechRecognizer = speechRecognizer,
                    onGradeSelected = { grade -> viewModel.saveGrade(item.key, grade) },
                    onNext = { viewModel.moveToNext() },
                    onPrevious = { viewModel.moveToPrevious() },
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
                )
            }
        }
    }
}
