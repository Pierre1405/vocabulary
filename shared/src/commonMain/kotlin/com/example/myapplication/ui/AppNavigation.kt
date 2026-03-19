package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.VocabularyRepository

@Composable
fun AppNavigation(
    repository: VocabularyRepository,
    audioPlayer: AudioPlayer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "stories",
        modifier = modifier
    ) {
        composable("stories") {
            StoryListScreen(
                repository = repository,
                onStoryClick = { storyId -> navController.navigate("phrases/$storyId") }
            )
        }
        composable("phrases/{storyId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId")?.toLong() ?: return@composable
            PhraseListScreen(
                repository = repository,
                audioPlayer = audioPlayer,
                storyId = storyId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
