package com.example.myapplication.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.myapplication.ui.practice.LowGradeConjugationItem
import com.example.myapplication.ui.practice.LowGradeSentenceItem
import com.example.myapplication.ui.practice.LowGradeWordItem
import com.example.myapplication.ui.practice.StoryViewModel
import com.example.myapplication.ui.practice.UpcomingConjugationItem
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
    val upcomingConjugationItems by viewModel.upcomingConjugationItems.collectAsState()
    val lowGradeWords by viewModel.lowGradeWords.collectAsState()
    val lowGradeSentences by viewModel.lowGradeSentences.collectAsState()
    val lowGradeConjugations by viewModel.lowGradeConjugations.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshCounts()
        onPauseOrDispose { }
    }

    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.reviewTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.reviewTabReview) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.reviewTabLowGrade) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.reviewTabSchedule) })
            }

            when (selectedTab) {
                0 -> ReviewTab(
                    nativeLanguage = nativeLanguage,
                    learnedLanguage = learnedLanguage,
                    countNativeToLearned = countNativeToLearned,
                    countLearnedToNative = countLearnedToNative,
                    countWordLearnedToNative = countWordLearnedToNative,
                    countWordNativeToLearned = countWordNativeToLearned,
                    countConjugation = countConjugation,
                    onReviewClick = onReviewClick,
                    onWordReviewClick = onWordReviewClick,
                    onConjugationReviewClick = onConjugationReviewClick,
                    strings = strings
                )
                1 -> LowGradeTab(
                    lowGradeSentences = lowGradeSentences,
                    lowGradeWords = lowGradeWords,
                    lowGradeConjugations = lowGradeConjugations,
                    strings = strings
                )
                2 -> ScheduleTab(
                    upcomingGroups = upcomingGroups.filter { it.type == "sentence" },
                    upcomingWordItems = upcomingWordItems,
                    upcomingConjugationItems = upcomingConjugationItems,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun ReviewTab(
    nativeLanguage: String,
    learnedLanguage: String,
    countNativeToLearned: Long,
    countLearnedToNative: Long,
    countWordLearnedToNative: Long,
    countWordNativeToLearned: Long,
    countConjugation: Long,
    onReviewClick: (String, String, Boolean) -> Unit,
    onWordReviewClick: (String, String) -> Unit,
    onConjugationReviewClick: () -> Unit,
    strings: Strings
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
        item { Text(text = strings.reviewWords, style = MaterialTheme.typography.titleMedium) }
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
        item { Text(text = strings.reviewConjugation, style = MaterialTheme.typography.titleMedium) }
        item {
            Button(onClick = onConjugationReviewClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (countConjugation > 0) "🇩🇪 ${strings.reviewConjugation} ($countConjugation)"
                    else "🇩🇪 ${strings.reviewConjugation}"
                )
            }
        }
    }
}

@Composable
private fun LowGradeTab(
    lowGradeSentences: List<LowGradeSentenceItem>,
    lowGradeWords: List<LowGradeWordItem>,
    lowGradeConjugations: List<LowGradeConjugationItem>,
    strings: Strings
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (lowGradeSentences.isNotEmpty()) {
            item {
                Text(text = strings.reviewSentences, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp))
            }
            items(lowGradeSentences) { item ->
                LowGradeSentenceRow(item = item)
            }
        }
        if (lowGradeWords.isNotEmpty()) {
            item {
                Text(text = strings.reviewWords, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(lowGradeWords) { item ->
                LowGradeWordRow(item = item)
            }
        }
        if (lowGradeConjugations.isNotEmpty()) {
            item {
                Text(text = strings.reviewConjugation, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(lowGradeConjugations) { item ->
                LowGradeConjugationRow(item = item)
            }
        }
    }
}

@Composable
private fun ScheduleTab(
    upcomingGroups: List<com.example.myapplication.data.UpcomingGroup>,
    upcomingWordItems: List<UpcomingWordItem>,
    upcomingConjugationItems: List<UpcomingConjugationItem>,
    strings: Strings
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp)) }
        items(upcomingGroups) { group ->
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
        items(upcomingConjugationItems) { item ->
            UpcomingConjugationRow(item = item, strings = strings)
        }
    }
}

@Composable
private fun LowGradeSentenceRow(item: LowGradeSentenceItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${localeToFlag(item.sourceLocale)} ${item.sourceText}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "${localeToFlag(item.targetLocale)} ${item.targetText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = "★${item.grade}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LowGradeWordRow(item: LowGradeWordItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${localeToFlag(item.wordLocale)} ${item.lemmaWithArticle}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "${localeToFlag(item.translationLocale)} ${item.translationWithArticle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "★${item.grade}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LowGradeConjugationRow(item: LowGradeConjugationItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "🇩🇪 ${item.lemma}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = item.tenseLabel + if (item.pronouns != null) " (${item.pronouns})" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "★${item.grade}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun UpcomingWordRow(item: UpcomingWordItem, strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun UpcomingConjugationRow(item: UpcomingConjugationItem, strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "🇩🇪 ${item.lemma}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = item.tenseLabel + if (item.pronouns != null) " (${item.pronouns})" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "★${item.grade}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            text = strings.reviewUpcomingIn(item.hoursUntilDue),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
