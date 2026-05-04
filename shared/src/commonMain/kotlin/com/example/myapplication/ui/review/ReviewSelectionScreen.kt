package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.LocalStrings
import com.example.myapplication.ui.Strings
import com.example.myapplication.ui.localeToFlag
import com.example.myapplication.ui.practice.StoryViewModel
import com.example.myapplication.ui.practice.UpcomingWordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSelectionScreen(
    repository: VocabularyRepository,
    learningRepository: LearningRepository,
    dictionaryRepository: DictionaryRepository,
    onReviewClick: (sourceLocale: String, targetLocale: String, sourceBlurred: Boolean) -> Unit,
    onWordReviewClick: (sourceLocale: String, targetLocale: String) -> Unit,
    onConjugationReviewClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: StoryViewModel = viewModel { StoryViewModel(repository, learningRepository, dictionaryRepository) }
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val learnedLanguage by viewModel.learnedLanguage.collectAsState()
    val countNativeToLearned by viewModel.countNativeToLearned.collectAsState()
    val countLearnedToNative by viewModel.countLearnedToNative.collectAsState()
    val countWordLearnedToNative by viewModel.countWordLearnedToNative.collectAsState()
    val countWordNativeToLearned by viewModel.countWordNativeToLearned.collectAsState()
    val countConjugation by viewModel.countConjugation.collectAsState()
    val upcomingGroups by viewModel.upcomingGroups.collectAsState()
    val upcomingWordItems by viewModel.upcomingWordItems.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshCounts()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalStrings.current.reviewTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LocalStrings.current.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        val strings = LocalStrings.current
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = strings.reviewSentences, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onReviewClick(nativeLanguage, learnedLanguage, false) },
                        modifier = Modifier.weight(1f),
                        enabled = countNativeToLearned > 0
                    ) {
                        Text("${localeToFlag(nativeLanguage)} → ${localeToFlag(learnedLanguage)} ($countNativeToLearned)")
                    }
                    Button(
                        onClick = { onReviewClick(learnedLanguage, nativeLanguage, false) },
                        modifier = Modifier.weight(1f),
                        enabled = countLearnedToNative > 0
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null)
                        Text(" ${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countLearnedToNative)")
                    }
                    Button(
                        onClick = { onReviewClick(learnedLanguage, nativeLanguage, true) },
                        modifier = Modifier.weight(1f),
                        enabled = countLearnedToNative > 0
                    ) {
                        Icon(Icons.Filled.Headphones, contentDescription = null)
                        Text(" ${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countLearnedToNative)")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(text = strings.reviewWords, style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onWordReviewClick(learnedLanguage, nativeLanguage) },
                        modifier = Modifier.weight(1f),
                        enabled = countWordLearnedToNative > 0
                    ) {
                        Text("${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countWordLearnedToNative)")
                    }
                    Button(
                        onClick = { onWordReviewClick(nativeLanguage, learnedLanguage) },
                        modifier = Modifier.weight(1f),
                        enabled = countWordNativeToLearned > 0
                    ) {
                        Text("${localeToFlag(nativeLanguage)} → ${localeToFlag(learnedLanguage)} ($countWordNativeToLearned)")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(text = strings.reviewConjugation, style = MaterialTheme.typography.titleMedium)
            }
            item {
                Button(
                    onClick = onConjugationReviewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (countConjugation > 0) "🇩🇪 ${strings.reviewConjugation} ($countConjugation)"
                        else "🇩🇪 ${strings.reviewConjugation}"
                    )
                }
            }

            val upcomingSentenceGroups = upcomingGroups.filter { it.type == "sentence" }
            if (upcomingSentenceGroups.isNotEmpty() || upcomingWordItems.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(text = strings.reviewUpcomingTitle, style = MaterialTheme.typography.titleMedium)
                }
                items(upcomingSentenceGroups) { group ->
                    val delay = strings.reviewUpcomingIn(group.hoursUntilDue)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${localeToFlag(group.sourceLocale)} → ${localeToFlag(group.targetLocale)}  ${strings.reviewSentences}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${group.count}  $delay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(upcomingWordItems) { item ->
                    UpcomingWordRow(item = item, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun UpcomingWordRow(item: UpcomingWordItem, strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${localeToFlag(item.sourceLocale)} ${item.sourceText}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "${localeToFlag(item.targetLocale)} ${item.targetText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = strings.reviewUpcomingIn(item.hoursUntilDue),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
