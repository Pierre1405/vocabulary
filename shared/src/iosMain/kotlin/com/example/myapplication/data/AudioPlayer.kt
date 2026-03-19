package com.example.myapplication.data

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle

actual class AudioPlayer {
    private var audioPlayer: AVAudioPlayer? = null

    actual fun play(phraseId: Long, language: String) {
        audioPlayer?.stop()
        val fileName = "phrase_${phraseId}_$language"
        val url = NSBundle.mainBundle.URLForResource(fileName, withExtension = "mp3")
        url?.let {
            audioPlayer = AVAudioPlayer(contentsOfURL = it, error = null)
            audioPlayer?.play()
        }
    }

    actual fun release() {
        audioPlayer?.stop()
        audioPlayer = null
    }
}
