package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.VocabularyRepository

@Composable
fun StoryListScreen(
    repository: VocabularyRepository,
    onStoryClick: (Long) -> Unit,
    onReviewClick: (sourceLocale: String, targetLocale: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: StoryViewModel = viewModel { StoryViewModel(repository) }
    val stories by viewModel.stories.collectAsState()
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val learnedLanguage by viewModel.learnedLanguage.collectAsState()
    val countNativeToLearned by viewModel.countNativeToLearned.collectAsState()
    val countLearnedToNative by viewModel.countLearnedToNative.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Histoires",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onReviewClick(nativeLanguage, learnedLanguage) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Réviser ${localeToFlag(nativeLanguage)} → ${localeToFlag(learnedLanguage)} ($countNativeToLearned)")
            }
            Button(
                onClick = { onReviewClick(learnedLanguage, nativeLanguage) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Réviser ${localeToFlag(learnedLanguage)} → ${localeToFlag(nativeLanguage)} ($countLearnedToNative)")
            }
        }

        if (stories.isEmpty()) {
            Text(text = "Aucune histoire trouvée.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stories) { story ->
                    StoryCard(
                        story = story,
                        nativeLanguage = nativeLanguage,
                        learnedLanguage = learnedLanguage,
                        onClick = { onStoryClick(story.storyId) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoryCard(story: StoryWithTranslations, nativeLanguage: String, learnedLanguage: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = story.getTranslation(nativeLanguage),
                style = MaterialTheme.typography.bodyLarge
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = story.getTranslation(learnedLanguage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
