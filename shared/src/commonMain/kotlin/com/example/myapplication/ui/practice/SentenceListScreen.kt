package com.example.myapplication.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.ui.ClickableWordText
import com.example.myapplication.ui.LocalStrings
import com.example.myapplication.ui.SwipeableGradeCard
import com.example.myapplication.ui.localeToFlag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceListScreen(
    viewModel: SentenceViewModel,
    ttsPlayer: TtsPlayer,
    onWordClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val story by viewModel.story.collectAsState()
    val sentences by viewModel.sentences.collectAsState()
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val learnedLanguage by viewModel.learnedLanguage.collectAsState()
    val isPlayingAll by viewModel.isPlayingAll.collectAsState()
    val isLooping by viewModel.isLooping.collectAsState()
    val repeatCount by viewModel.repeatCount.collectAsState()
    val currentPlayingIndex by viewModel.currentPlayingIndex.collectAsState()
    val grades by viewModel.grades.collectAsState()

    var showNative by remember { mutableStateOf(false) }
    var showLearned by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(currentPlayingIndex) {
        if (currentPlayingIndex >= 0) listState.animateScrollToItem(currentPlayingIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(text = story?.getTranslation(nativeLanguage) ?: "", style = MaterialTheme.typography.titleMedium)
                        Text(text = story?.getTranslation(learnedLanguage) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cycleRepeat() }) {
                        Text("×$repeatCount", style = MaterialTheme.typography.labelLarge, color = if (repeatCount > 1) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { viewModel.toggleLoop() }) {
                        Icon(imageVector = Icons.Filled.Loop, contentDescription = LocalStrings.current.loop, tint = if (isLooping) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = {
                        if (isPlayingAll) viewModel.stopPlayAll(ttsPlayer)
                        else viewModel.playAll(ttsPlayer)
                    }) {
                        Icon(imageVector = if (isPlayingAll) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (isPlayingAll) LocalStrings.current.stop else LocalStrings.current.playAll)
                    }
                    FilterChip(selected = showNative, onClick = { showNative = !showNative }, label = { Text(localeToFlag(nativeLanguage)) })
                    FilterChip(selected = showLearned, onClick = { showLearned = !showLearned }, label = { Text(localeToFlag(learnedLanguage)) }, modifier = Modifier.padding(end = 8.dp))
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (sentences.isEmpty()) {
            Text(text = LocalStrings.current.sentenceEmpty, modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sentences) { index, sentence ->
                    SwipeableGradeCard(
                        onGradeSelected = { grade -> viewModel.saveGrade(sentence.sentenceKey, grade) },
                        currentGrade = grades[sentence.sentenceKey]
                    ) {
                        SentenceCard(
                            sentence = sentence,
                            ttsPlayer = ttsPlayer,
                            nativeLanguage = nativeLanguage,
                            learnedLanguage = learnedLanguage,
                            showNative = showNative,
                            showLearned = showLearned,
                            onToggleNative = { showNative = !showNative },
                            onToggleLearned = { showLearned = !showLearned },
                            onWordClick = onWordClick,
                            isPlaying = index == currentPlayingIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SentenceCard(
    sentence: SentenceWithTranslations,
    ttsPlayer: TtsPlayer,
    nativeLanguage: String,
    learnedLanguage: String,
    showNative: Boolean,
    showLearned: Boolean,
    onToggleNative: () -> Unit,
    onToggleLearned: () -> Unit,
    onWordClick: (String) -> Unit,
    isPlaying: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).then(if (!showLearned) Modifier.clickable { onToggleLearned() }.blur(8.dp) else Modifier)
            ) {
                if (showLearned) {
                    ClickableWordText(text = sentence.getTranslation(learnedLanguage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, onWordClick = onWordClick)
                } else {
                    Text(text = sentence.getTranslation(learnedLanguage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = { ttsPlayer.speak(sentence.getTranslation(learnedLanguage), learnedLanguage) }) {
                Text(LocalStrings.current.play, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).then(if (!showNative) Modifier.clickable { onToggleNative() }.blur(8.dp) else Modifier)
            ) {
                if (showNative) {
                    ClickableWordText(text = sentence.getTranslation(nativeLanguage), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, onWordClick = onWordClick)
                } else {
                    Text(text = sentence.getTranslation(nativeLanguage), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
