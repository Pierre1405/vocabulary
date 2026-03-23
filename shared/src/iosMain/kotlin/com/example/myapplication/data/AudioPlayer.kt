package com.example.myapplication.data

import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSBundle
import platform.darwin.NSObject

actual class AudioPlayer {
    private var audioPlayer: AVAudioPlayer? = null
    private var delegate: AudioPlayerDelegate? = null

    actual fun play(sentenceId: Long, language: String, onComplete: () -> Unit) {
        audioPlayer?.stop()
        val fileName = "sentence_${sentenceId}_$language"
        val url = NSBundle.mainBundle.URLForResource(fileName, withExtension = "mp3")
        if (url != null) {
            val d = AudioPlayerDelegate(onComplete)
            delegate = d
            audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
            audioPlayer?.delegate = d
            audioPlayer?.play()
        } else {
            onComplete()
        }
    }

    actual fun release() {
        audioPlayer?.stop()
        audioPlayer = null
        delegate = null
    }
}

private class AudioPlayerDelegate(private val onComplete: () -> Unit) : NSObject(),
    AVAudioPlayerDelegateProtocol {
    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
        onComplete()
    }
}
