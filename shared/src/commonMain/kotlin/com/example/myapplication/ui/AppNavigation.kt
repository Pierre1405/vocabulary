package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository
import kotlinx.serialization.Serializable

@Serializable
object StoriesRoute

@Serializable
data class SentencesRoute(val storyId: Long)

@Serializable
data class ReviewRoute(val sourceLocale: String, val targetLocale: String)

@Composable
fun AppNavigation(
    repository: VocabularyRepository,
    audioPlayer: AudioPlayer,
    speechRecognizer: SpeechRecognizer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = StoriesRoute,
        modifier = modifier
    ) {
        composable<StoriesRoute> {
            StoryListScreen(
                repository = repository,
                onStoryClick = { storyId -> navController.navigate(SentencesRoute(storyId)) },
                onReviewClick = { source, target -> navController.navigate(ReviewRoute(source, target)) }
            )
        }
        composable<SentencesRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SentencesRoute>()
            SentenceScreen(
                repository = repository,
                audioPlayer = audioPlayer,
                storyId = route.storyId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<ReviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReviewRoute>()
            ReviewScreen(
                repository = repository,
                audioPlayer = audioPlayer,
                speechRecognizer = speechRecognizer,
                sourceLocale = route.sourceLocale,
                targetLocale = route.targetLocale,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
