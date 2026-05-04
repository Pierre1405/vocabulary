package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
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
                ReviewCard(
                    itemKey = item.key,
                    sourceText = sourceText,
                    targetText = item.expectedForm,
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
