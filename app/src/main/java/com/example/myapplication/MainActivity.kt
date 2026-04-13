package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.myapplication.data.AudioPlayer
import com.example.myapplication.data.DatabaseDriverFactory
import com.example.myapplication.data.DictionaryDriverFactory
import com.example.myapplication.data.DictionaryRepository
import com.example.myapplication.data.SpeechRecognizer
import com.example.myapplication.data.VocabularyRepository
import com.example.myapplication.ui.AppNavigation
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val audioPlayer by lazy { AudioPlayer(applicationContext) }
    private val speechRecognizer by lazy { SpeechRecognizer(applicationContext) }

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission accordée ou refusée, le SpeechRecognizer gère l'erreur */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        val repository = VocabularyRepository(DatabaseDriverFactory(applicationContext))
        val dictionaryRepository = DictionaryRepository(DictionaryDriverFactory(applicationContext).createDriver())

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        repository = repository,
                        audioPlayer = audioPlayer,
                        speechRecognizer = speechRecognizer,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        speechRecognizer.release()
    }
}
