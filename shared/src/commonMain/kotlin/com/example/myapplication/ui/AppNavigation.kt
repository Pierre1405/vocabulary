package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.myapplication.data.AiService
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.TtsPlayer
import com.example.myapplication.data.ConversationStore
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.LearningRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.conjugation.ConjugationScreen
import com.example.myapplication.ui.conversation.ConversationScreen
import com.example.myapplication.ui.conversation.ConversationViewModel
import com.example.myapplication.ui.settings.ApiSettingsScreen
import com.example.myapplication.ui.review.ConjugationReviewScreen
import com.example.myapplication.ui.dictionary.DictionaryDetailScreen
import com.example.myapplication.ui.practice.StoryViewModel
import com.example.myapplication.ui.dictionary.DictionaryScreen
import com.example.myapplication.ui.practice.SentenceScreen
import com.example.myapplication.ui.practice.StoryListScreen
import com.example.myapplication.ui.review.ReviewScreen
import com.example.myapplication.ui.review.ReviewSelectionScreen
import com.example.myapplication.ui.review.WordReviewScreen
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
object ConjugationRoute

@Serializable
object ConjugationReviewRoute

@Serializable
data class DictionaryDetailRoute(val entryId: Long, val groupKey: String? = null)

@Serializable
data class SentencesRoute(val storyId: Long)

@Serializable
data class ReviewRoute(val sourceLocale: String, val targetLocale: String, val sourceBlurred: Boolean = false)

@Serializable
data class WordReviewRoute(val sourceLocale: String, val targetLocale: String)

@Serializable
object ConversationRoute

@Serializable
object ApiSettingsRoute

@Composable
fun AppNavigation(
    repository: VocabularyRepository,
    learningRepository: LearningRepository,
    dictionaryRepository: DictionaryRepository,
    audioPlayer: AudioPlayer,
    ttsPlayer: TtsPlayer,
    speechRecognizer: SpeechRecognizer,
    aiService: AiService,
    conversationStore: ConversationStore,
    modifier: Modifier = Modifier
) {
    val appViewModel: StoryViewModel = viewModel { StoryViewModel(repository, learningRepository) }
    val nativeLanguage by appViewModel.nativeLanguage.collectAsState()
    val conversationViewModel: ConversationViewModel = viewModel {
        ConversationViewModel(aiService, conversationStore, repository, learningRepository, dictionaryRepository)
    }

    CompositionLocalProvider(LocalStrings provides stringsForLocale(nativeLanguage)) {
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
                onDictionnaireClick = { navController.navigate(DictionaryRoute()) },
                onConjugaisonsClick = { navController.navigate(ConjugationRoute) },
                onConversationClick = { navController.navigate(ConversationRoute) }
            )
        }
        composable<ConversationRoute> {
            ConversationScreen(
                vm = conversationViewModel,
                onSettingsClick = { navController.navigate(ApiSettingsRoute) },
                onWordClick = { word -> navController.navigate(DictionaryRoute(word)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ApiSettingsRoute> {
            ApiSettingsScreen(
                conversationStore = conversationStore,
                onBack = { navController.popBackStack() }
            )
        }
        composable<ConjugationRoute> {
            ConjugationScreen(
                dictionaryRepository = dictionaryRepository,
                onVerbClick = { entryId ->
                    navController.navigate(DictionaryDetailRoute(entryId))
                },
                onBack = { navController.popBackStack() }
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
                dictionaryRepository = dictionaryRepository,
                onReviewClick = { source, target, sourceBlurred -> navController.navigate(ReviewRoute(source, target, sourceBlurred)) },
                onWordReviewClick = { source, target -> navController.navigate(WordReviewRoute(source, target)) },
                onConjugationReviewClick = { navController.navigate(ConjugationReviewRoute) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ConjugationReviewRoute> {
            ConjugationReviewScreen(
                dictionaryRepository = dictionaryRepository,
                learningRepository = learningRepository,
                ttsPlayer = ttsPlayer,
                speechRecognizer = speechRecognizer,
                onBack = { navController.popBackStack() }
            )
        }
        composable<WordReviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<WordReviewRoute>()
            WordReviewScreen(
                dictionaryRepository = dictionaryRepository,
                learningRepository = learningRepository,
                ttsPlayer = ttsPlayer,
                speechRecognizer = speechRecognizer,
                sourceLocale = route.sourceLocale,
                targetLocale = route.targetLocale,
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
                ttsPlayer = ttsPlayer,
                entryId = route.entryId,
                groupKeyFilter = route.groupKey,
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
    } // CompositionLocalProvider
}
