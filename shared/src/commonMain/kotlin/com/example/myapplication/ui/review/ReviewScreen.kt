package com.example.myapplication.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.LocalStrings
import com.example.myapplication.ui.gradeColor
import com.example.myapplication.ui.localeToFlag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    repository: VocabularyRepository,
    learningRepository: LearningRepository,
    audioPlayer: AudioPlayer,
    speechRecognizer: SpeechRecognizer,
    sourceLocale: String,
    targetLocale: String,
    sourceBlurred: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReviewViewModel = viewModel(key = "$sourceLocale-$targetLocale") {
        ReviewViewModel(repository, learningRepository, sourceLocale, targetLocale)
    }

    val sentences by viewModel.sentences.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentGrade by viewModel.currentGrade.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val showPreview by viewModel.showPreview.collectAsState()
    val previewItems by viewModel.previewItems.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val reviewPlayer = remember { ReviewPlayer(audioPlayer, scope) }
    val isPlayingAll by reviewPlayer.isPlaying.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            reviewPlayer.release()
            speechRecognizer.release()
        }
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                },
                actions = {
                    if (sentences.isNotEmpty() && !isCompleted) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = LocalStrings.current.reviewDelete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = {
                        reviewPlayer.toggle(
                            sentences = sentences,
                            startIndex = currentIndex,
                            sourceLocale = sourceLocale,
                            targetLocale = targetLocale,
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
        if (showDeleteDialog) {
            val strings = LocalStrings.current
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
        } else if (sentences.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(LocalStrings.current.reviewEmpty)
            }
        } else {
            val sentence = sentences[currentIndex]
            ReviewCard(
                itemKey = sentence.sentenceKey,
                sourceText = sentence.getTranslation(sourceLocale),
                targetText = sentence.getTranslation(targetLocale),
                targetLocale = targetLocale,
                currentGrade = currentGrade,
                sourceBlurred = sourceBlurred,
                forceShowTarget = isPlayingAll,
                onPlaySource = { audioPlayer.play(sentence.sentenceKey, sourceLocale) },
                onPlayTarget = { audioPlayer.play(sentence.sentenceKey, targetLocale) },
                speechRecognizer = speechRecognizer,
                onGradeSelected = { grade -> viewModel.saveGrade(sentence.sentenceKey, grade) },
                onNext = { viewModel.moveToNext() },
                onPrevious = { viewModel.moveToPrevious() },
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            )
        }
    }
}

@Composable
fun ReviewCard(
    itemKey: Any,
    sourceText: String,
    targetText: String,
    targetLocale: String,
    currentGrade: Int?,
    sourceBlurred: Boolean = false,
    forceShowTarget: Boolean = false,
    onPlaySource: (() -> Unit)? = null,
    onPlayTarget: (() -> Unit)? = null,
    speechRecognizer: SpeechRecognizer,
    onGradeSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSource by remember(itemKey) { mutableStateOf(false) }
    val sourceVisible = !sourceBlurred || showSource
    var showTarget by remember(itemKey) { mutableStateOf(false) }
    val targetVisible = showTarget || forceShowTarget
    var spokenText by remember(itemKey) { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .then(if (sourceBlurred) Modifier.clickable { showSource = !showSource } else Modifier)
                            .then(if (!sourceVisible) Modifier.blur(8.dp) else Modifier)
                    ) {
                        Text(text = sourceText, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (onPlaySource != null) {
                        IconButton(onClick = onPlaySource) {
                            Text("▶", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .clickable { showTarget = !showTarget }
                            .then(if (!targetVisible) Modifier.blur(8.dp) else Modifier)
                    ) {
                        Text(text = targetText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    if (onPlayTarget != null) {
                        IconButton(onClick = onPlayTarget) {
                            Text("▶", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = spokenText,
                onValueChange = { spokenText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("${localeToFlag(targetLocale)} ${LocalStrings.current.reviewSpeakHint}") },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    capitalization = KeyboardCapitalization.None
                )
            )
            IconButton(onClick = {
                if (isListening) {
                    speechRecognizer.stopListening()
                    isListening = false
                } else {
                    isListening = true
                    speechRecognizer.startListening(
                        locale = targetLocale,
                        onResult = { result -> spokenText = result; isListening = false },
                        onError = { isListening = false }
                    )
                }
            }) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isListening) LocalStrings.current.reviewStopListen else LocalStrings.current.reviewListen,
                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { grade ->
                val isSelected = grade == currentGrade
                Button(
                    onClick = { onGradeSelected(grade) },
                    modifier = Modifier.weight(if (isSelected) 1.3f else 1f),
                    colors = ButtonDefaults.buttonColors(containerColor = gradeColor(grade).copy(alpha = if (isSelected) 1f else 0.45f)),
                    border = if (isSelected) BorderStroke(2.dp, gradeColor(grade)) else null
                ) {
                    Text(text = "$grade", style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) { Text(LocalStrings.current.reviewPrevious) }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(LocalStrings.current.reviewNext) }
        }
    }
}
