package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.ui.LocalStrings
import com.example.myapplication.ui.localeToFlag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordReviewScreen(
    dictionaryRepository: DictionaryRepository,
    learningRepository: LearningRepository,
    ttsPlayer: TtsPlayer,
    speechRecognizer: SpeechRecognizer,
    sourceLocale: String,
    targetLocale: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: WordReviewViewModel = viewModel {
        WordReviewViewModel(dictionaryRepository, learningRepository, sourceLocale, targetLocale)
    }

    val items by viewModel.items.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentGrade by viewModel.currentGrade.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val showPreview by viewModel.showPreview.collectAsState()
    val previewItems by viewModel.previewItems.collectAsState()

    val scope = rememberCoroutineScope()
    val wordPlayer = remember { WordReviewPlayer(ttsPlayer, scope) }
    val isPlayingAll by wordPlayer.isPlaying.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            wordPlayer.release()
            speechRecognizer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        LocalStrings.current.reviewWords +
                        if (items.isNotEmpty()) "  ${currentIndex + 1} / ${items.size}" else ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        wordPlayer.toggle(
                            items = items,
                            startIndex = currentIndex,
                            sourceLocale = viewModel.sourceLocale,
                            onIndexChanged = { viewModel.navigateTo(it) }
                        )
                    }) {
                        Icon(
                            imageVector = if (isPlayingAll) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingAll) LocalStrings.current.stop else LocalStrings.current.playAll
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (showPreview) {
            ReviewPreviewScreen(
                items = previewItems,
                onStart = { viewModel.dismissPreview() },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (isCompleted) {
            ReviewCompletionScreen(
                onRestart = { viewModel.restart(filterLowGrades = false) },
                onRestartLowGrades = { viewModel.restart(filterLowGrades = true) },
                onFinish = onBack,
                modifier = Modifier.padding(innerPadding)
            )
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(LocalStrings.current.reviewEmpty)
            }
        } else {
            val item = items[currentIndex]
            val reversed = viewModel.sourceLocale != item.wordLocale
            val sourceText = if (reversed) "${localeToFlag(item.translationLocale)}  ${item.translationWithArticle}"
                             else "${localeToFlag(item.wordLocale)}  ${item.lemmaWithArticle}"
            val targetText = if (reversed) "${localeToFlag(item.wordLocale)}  ${item.lemmaWithArticle}"
                             else "${localeToFlag(item.translationLocale)}  ${item.translationWithArticle}"
            val srcLocale = if (reversed) item.translationLocale else item.wordLocale
            val tgtLocale = if (reversed) item.wordLocale else item.translationLocale

            ReviewCard(
                itemKey = item.translationId,
                sourceText = sourceText,
                targetText = targetText,
                targetLocale = tgtLocale,
                currentGrade = currentGrade,
                forceShowTarget = isPlayingAll,
                onPlaySource = { ttsPlayer.speak(if (reversed) item.translationWithArticle else item.lemmaWithArticle, srcLocale) },
                onPlayTarget = { ttsPlayer.speak(if (reversed) item.lemmaWithArticle else item.translationWithArticle, tgtLocale) },
                speechRecognizer = speechRecognizer,
                onGradeSelected = { grade -> viewModel.saveGrade(item.translationId, grade) },
                onNext = { viewModel.moveToNext() },
                onPrevious = { viewModel.moveToPrevious() },
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            )
        }
    }
}
