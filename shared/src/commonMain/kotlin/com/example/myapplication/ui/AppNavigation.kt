package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object StoriesRoute

@Serializable
object ReviewSelectionRoute

@Serializable
data class DictionaryRoute(val initialQuery: String = "")

@Serializable
data class DictionaryDetailRoute(val entryId: Long)

@Serializable
data class SentencesRoute(val storyId: Long)

@Serializable
data class ReviewRoute(val sourceLocale: String, val targetLocale: String, val sourceBlurred: Boolean = false)

@Composable
fun AppNavigation(
    repository: VocabularyRepository,
    learningRepository: LearningRepository,
    dictionaryRepository: DictionaryRepository,
    audioPlayer: AudioPlayer,
    speechRecognizer: SpeechRecognizer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onLectureClick = { navController.navigate(StoriesRoute) },
                onRevisionClick = { navController.navigate(ReviewSelectionRoute) },
                onDictionnaireClick = { navController.navigate(DictionaryRoute()) }
            )
        }
        composable<StoriesRoute> {
            StoryListScreen(
                repository = repository,
                learningRepository = learningRepository,
                onStoryClick = { storyId -> navController.navigate(SentencesRoute(storyId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ReviewSelectionRoute> {
            ReviewSelectionScreen(
                repository = repository,
                learningRepository = learningRepository,
                onReviewClick = { source, target, sourceBlurred -> navController.navigate(ReviewRoute(source, target, sourceBlurred)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<DictionaryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DictionaryRoute>()
            DictionaryScreen(
                dictionaryRepository = dictionaryRepository,
                initialQuery = route.initialQuery,
                onEntryClick = { entryId -> navController.navigate(DictionaryDetailRoute(entryId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<DictionaryDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DictionaryDetailRoute>()
            DictionaryDetailScreen(
                dictionaryRepository = dictionaryRepository,
                learningRepository = learningRepository,
                entryId = route.entryId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<SentencesRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SentencesRoute>()
            SentenceScreen(
                repository = repository,
                learningRepository = learningRepository,
                audioPlayer = audioPlayer,
                storyId = route.storyId,
                onWordClick = { word -> navController.navigate(DictionaryRoute(word)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ReviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReviewRoute>()
            ReviewScreen(
                repository = repository,
                learningRepository = learningRepository,
                audioPlayer = audioPlayer,
                speechRecognizer = speechRecognizer,
                sourceLocale = route.sourceLocale,
                targetLocale = route.targetLocale,
                sourceBlurred = route.sourceBlurred,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
