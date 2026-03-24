package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.VocabularyRepository

enum class SentenceTab { ALL, DETAIL }

@Composable
fun SentenceScreen(
    repository: VocabularyRepository,
    audioPlayer: AudioPlayer,
    storyId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SentenceViewModel = viewModel(key = storyId.toString()) {
        SentenceViewModel(repository, storyId)
    }

    var currentTab by remember { mutableStateOf(SentenceTab.ALL) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentTab == SentenceTab.ALL) {
                        Button(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("All")
                        }
                        OutlinedButton(
                            onClick = { currentTab = SentenceTab.DETAIL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Detail")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { currentTab = SentenceTab.ALL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("All")
                        }
                        Button(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("Detail")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (currentTab) {
            SentenceTab.ALL -> SentenceListScreen(
                viewModel = viewModel,
                audioPlayer = audioPlayer,
                onBack = onBack,
                modifier = Modifier.padding(innerPadding)
            )
            SentenceTab.DETAIL -> SentenceDetailScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
