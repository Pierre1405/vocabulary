package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.VocabularyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSelectionScreen(
    repository: VocabularyRepository,
    learningRepository: LearningRepository,
    onReviewClick: (sourceLocale: String, targetLocale: String, sourceBlurred: Boolean) -> Unit,
    onWordReviewClick: (reversed: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: StoryViewModel = viewModel { StoryViewModel(repository, learningRepository) }
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val learnedLanguage by viewModel.learnedLanguage.collectAsState()
    val countNativeToLearned by viewModel.countNativeToLearned.collectAsState()
    val countLearnedToNative by viewModel.countLearnedToNative.collectAsState()
    val countWordLearning by viewModel.countWordLearning.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshCounts()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Révision") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Phrases",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onReviewClick(nativeLanguage, learnedLanguage, false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${localeToFlag(nativeLanguage)} → ${localeToFlag(learnedLanguage)} ($countNativeToLearned)")
                }
                Button(
                    onClick = { onReviewClick(learnedLanguage, nativeLanguage, false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null)
                    Text(" ${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countLearnedToNative)")
                }
                Button(
                    onClick = { onReviewClick(learnedLanguage, nativeLanguage, true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Headphones, contentDescription = null)
                    Text(" ${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countLearnedToNative)")
                }
            }

            HorizontalDivider()

            Text(
                text = "Mots",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onWordReviewClick(false) },
                    modifier = Modifier.weight(1f),
                    enabled = countWordLearning > 0
                ) {
                    Text("${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countWordLearning)")
                }
                Button(
                    onClick = { onWordReviewClick(true) },
                    modifier = Modifier.weight(1f),
                    enabled = countWordLearning > 0
                ) {
                    Text("${localeToFlag(nativeLanguage)} → ${localeToFlag(learnedLanguage)} ($countWordLearning)")
                }
            }
        }
    }
}
