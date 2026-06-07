package com.squashscore.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Optional voice announcement of scoring events.
 * Uses built-in Android TTS — no internet needed.
 * Falls back to silence if TTS is unavailable.
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var initialized = false

    var enabled = true

    init {
        try {
            tts = TextToSpeech(context) { status ->
                initialized = status == TextToSpeech.SUCCESS
                if (initialized) {
                    tts?.language = Locale.US
                    Log.i(TAG, "TTS ready")
                } else {
                    Log.w(TAG, "TTS initialization failed: $status")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS not available: ${e.message}")
        }
    }

    fun speak(text: String) {
        if (!enabled || !initialized) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "score_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.d(TAG, "TTS speak failed: ${e.message}")
        }
    }

    fun announcePoint(scorerName: String, serverScore: Int, receiverScore: Int) {
        speak("$scorerName. ${serverScore}-${receiverScore}.")
    }

    fun announceGameWon(winnerName: String, gamesWon: Int, gamesLost: Int) {
        speak("Game $winnerName. $gamesWon games to $gamesLost.")
    }

    fun announceMatchWon(winnerName: String) {
        speak("Match $winnerName!")
    }

    fun announceServerChange(newServerName: String) {
        speak("$newServerName serving.")
    }

    fun announceRest(seconds: Int) {
        speak("$seconds seconds.")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
