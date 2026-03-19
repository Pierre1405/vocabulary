package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.db.Story

@Composable
fun StoryListScreen(
    repository: VocabularyRepository,
    onStoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: StoryViewModel = viewModel { StoryViewModel(repository) }
    val stories by viewModel.stories.collectAsState()

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

        if (stories.isEmpty()) {
            Text(text = "Aucune histoire trouvée.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stories) { story ->
                    StoryCard(
                        story = story,
                        onClick = { onStoryClick(story.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = story.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
