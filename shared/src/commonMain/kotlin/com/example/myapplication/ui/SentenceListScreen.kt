package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.myapplication.data.AudioPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceListScreen(
    viewModel: SentenceViewModel,
    audioPlayer: AudioPlayer,
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
    val currentPlayingIndex by viewModel.currentPlayingIndex.collectAsState()
    val grades by viewModel.grades.collectAsState()

    var showNative by remember { mutableStateOf(false) }
    var showLearned by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(currentPlayingIndex) {
        if (currentPlayingIndex >= 0) {
            listState.animateScrollToItem(currentPlayingIndex)
        }
    }

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
                    IconButton(onClick = { viewModel.toggleLoop() }) {
                        Icon(
                            imageVector = Icons.Filled.Loop,
                            contentDescription = "Loop",
                            tint = if (isLooping) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = {
                        if (isPlayingAll) viewModel.stopPlayAll(audioPlayer)
                        else viewModel.playAll(audioPlayer)
                    }) {
                        Icon(
                            imageVector = if (isPlayingAll) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingAll) "Stop" else "Play all"
                        )
                    }
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
        if (sentences.isEmpty()) {
            Text(
                text = "Aucune sentence trouvée.",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sentences) { index, sentence ->
                    SwipeableGradeCard(
                        onGradeSelected = { grade -> viewModel.saveGrade(sentence.sentenceKey, grade) },
                        currentGrade = grades[sentence.sentenceKey]
                    ) {
                        SentenceCard(
                            sentence = sentence,
                            audioPlayer = audioPlayer,
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
    audioPlayer: AudioPlayer,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (!showLearned) Modifier.clickable { onToggleLearned() }.blur(8.dp) else Modifier)
            ) {
                if (showLearned) {
                    ClickableWordText(
                        text = sentence.getTranslation(learnedLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        onWordClick = onWordClick
                    )
                } else {
                    Text(
                        text = sentence.getTranslation(learnedLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { audioPlayer.play(sentence.sentenceKey, learnedLanguage) }) {
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (!showNative) Modifier.clickable { onToggleNative() }.blur(8.dp) else Modifier)
            ) {
                if (showNative) {
                    ClickableWordText(
                        text = sentence.getTranslation(nativeLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        onWordClick = onWordClick
                    )
                } else {
                    Text(
                        text = sentence.getTranslation(nativeLanguage),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ClickableWordText(
    text: String,
    style: TextStyle,
    color: Color,
    onWordClick: (String) -> Unit
) {
    val tokens = text.split(" ")
    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { i, token ->
            val word = token.trimEnd { !it.isLetter() }
            if (word.isNotEmpty()) {
                pushStringAnnotation("WORD", word)
                withStyle(SpanStyle(color = color)) { append(token) }
                pop()
            } else {
                withStyle(SpanStyle(color = color)) { append(token) }
            }
            if (i < tokens.size - 1) append(" ")
        }
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
fun SwipeableGradeCard(
    onGradeSelected: (Int) -> Unit,
    currentGrade: Int? = null,
    content: @Composable () -> Unit
) {
    val revealWidthDp = 200.dp
    val revealWidthPx = with(LocalDensity.current) { revealWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        // Boutons de note révélés derrière la carte
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(revealWidthDp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..5).forEach { grade ->
                val isSelected = grade == currentGrade
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) gradeColor(grade).copy(alpha = 0.3f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .clickable {
                            onGradeSelected(grade)
                            scope.launch { offsetX.animateTo(0f) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$grade",
                        style = if (isSelected) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                     else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = gradeColor(grade)
                    )
                }
            }
        }

        // Carte glissante avec fond opaque pour masquer les boutons derrière
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(-revealWidthPx, 0f)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value < -revealWidthPx / 2) {
                                offsetX.animateTo(-revealWidthPx)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

