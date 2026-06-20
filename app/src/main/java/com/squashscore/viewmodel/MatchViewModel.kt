package com.squashscore.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.squashscore.data.MatchRepository
import com.squashscore.data.WearDataSync
import com.squashscore.gesture.GestureScorer
import com.squashscore.model.GameState
import com.squashscore.model.Match
import com.squashscore.service.MatchForegroundService
import com.squashscore.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MatchRepository(application)
    val tts = TtsManager(application)
    private val wearSync = WearDataSync(application)
    private val gestureScorer = GestureScorer(
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
        getVibrator(application)
    )
    private val hapticVibrator = getVibrator(application)

    // Foreground service keeps the CPU awake during matches.
    private val app = application

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(gyroAvailable = gestureScorer.isAvailable) }
    }

    data class UiState(
        val match: Match = Match(),
        val history: List<Match> = emptyList(),
        val screen: Screen = Screen.SETUP,
        val restSeconds: Int = 0,
        val voiceEnabled: Boolean = true,
        val gestureScoring: Boolean = false,
        val gyroAvailable: Boolean = false,
        // Setup options persisted across matches
        val lastSimpleMode: Boolean = true,
        val lastIndefinite: Boolean = true
    )

    enum class Screen { SETUP, PLAYING, SUMMARY, HISTORY }

    // ── Persistent settings ──

    private val prefs = application.getSharedPreferences("squashscore", Context.MODE_PRIVATE)

    fun loadPlayer1Name(): String {
        return prefs.getString("player1_name", "") ?: ""
    }

    fun savePlayer1Name(name: String) {
        prefs.edit().putString("player1_name", name).apply()
    }

    fun loadLastSport(): com.squashscore.model.Sport {
        val name = prefs.getString("last_sport", null) ?: return com.squashscore.model.Sport.SQUASH
        return com.squashscore.model.Sport.fromName(name)
    }

    fun saveLastSport(sport: com.squashscore.model.Sport) {
        prefs.edit().putString("last_sport", sport.name).apply()
    }

    fun loadSimpleMode(): Boolean = prefs.getBoolean("simple_mode", true)
    fun saveSimpleMode(value: Boolean) = prefs.edit().putBoolean("simple_mode", value).apply()

    // ── Setup ──

    fun setupTwoPlayer(
        playerA: String, playerB: String,
        indefinite: Boolean = false,
        simpleMode: Boolean = false,
        sportName: String = com.squashscore.model.Sport.SQUASH.name
    ) {
        savePlayer1Name(playerA)
        saveSimpleMode(simpleMode)
        _uiState.update { it.copy(match = Match().setupTwoPlayer(playerA, playerB, indefinite = indefinite, simpleMode = simpleMode, sportName = sportName)) }
        startMatch()
    }

    fun setupThreePlayer(
        playerA: String, playerB: String, playerC: String,
        selfScoreOnly: Boolean = true,
        indefinite: Boolean = false,
        simpleMode: Boolean = true,
        sportName: String = com.squashscore.model.Sport.SQUASH.name
    ) {
        savePlayer1Name(playerA)
        _uiState.update { it.copy(match = Match().setupThreePlayer(playerA, playerB, playerC, selfScoreOnly = selfScoreOnly, indefinite = indefinite, simpleMode = simpleMode, sportName = sportName)) }
        startMatch()
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _uiState.update { it.copy(voiceEnabled = enabled) }
        tts.enabled = enabled
    }

    fun setGestureScoring(enabled: Boolean) {
        _uiState.update { it.copy(gestureScoring = enabled) }
        if (enabled && _uiState.value.screen == Screen.PLAYING) {
            startGestureScoring()
        } else {
            gestureScorer.stop()
        }
    }

    private fun startMatch() {
        _uiState.update { it.copy(
            match = it.match.startMatch(),
            screen = Screen.PLAYING,
            restSeconds = 0
        ) }
        if (_uiState.value.gestureScoring) {
            startGestureScoring()
        }
        MatchForegroundService.start(app)
    }

    private fun startGestureScoring() {
        if (!gestureScorer.isAvailable) return
        gestureScorer.start { scorePoint(0) }
    }

    // ── Haptic feedback ──

    private fun tapHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hapticVibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                hapticVibrator?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    // ── Scoring ──

    fun scorePoint(playerIndex: Int) {
        try {
            val current = _uiState.value.match

            if (current.state != GameState.PLAYING) return
            if (current.players.isEmpty() || playerIndex >= current.players.size) return

            val updated = current.awardPoint(playerIndex) ?: return

            // Haptic feedback on every point
            tapHaptic()

            val scorer = current.players.getOrNull(playerIndex) ?: return
            val newScorerScore = scorer.score + 1
            val isThreePlayer = current.isThreePlayer
            val silent = current.selfScoreOnly
            val opponentScore = if (!isThreePlayer) current.players.getOrNull(1 - playerIndex)?.score ?: 0 else 0

            when (updated.state) {
                GameState.FINISHED -> {
                    if (_uiState.value.voiceEnabled && !silent) {
                        try { tts.speak("${newScorerScore}-${opponentScore}.") } catch (_: Exception) {}
                    }
                    finishMatch(updated)
                }
                GameState.BETWEEN_GAMES -> {
                    if (_uiState.value.voiceEnabled && !silent) {
                        try { tts.speak("${newScorerScore}-${opponentScore}.") } catch (_: Exception) {}
                    }
                    val gameWinner = updated.completedGames.lastOrNull() ?: return
                    if (_uiState.value.voiceEnabled) {
                        val name = updated.players.getOrNull(gameWinner.winnerIndex)?.name ?: return
                        val won = updated.players.getOrNull(gameWinner.winnerIndex)?.gamesWon ?: 0
                        val lost = updated.completedGames.size - won
                        tts.announceGameWon(name, won, lost)
                    }
                    _uiState.update { it.copy(match = updated) }
                }
                else -> {
                    _uiState.update { it.copy(match = updated) }
                    if (_uiState.value.voiceEnabled && !silent) {
                        if (!isThreePlayer && updated.serverIndex != current.serverIndex) {
                            val srv = updated.server
                            val rcv = updated.receiver
                            if (srv != null && rcv != null) {
                                try { tts.speak("${srv.name} serving. ${srv.score}-${rcv.score}.") } catch (_: Exception) {}
                            }
                        } else {
                            try { tts.speak("${newScorerScore}-${opponentScore}.") } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WearScore", "scorePoint crashed", e)
        }
    }

    fun undo() {
        val updated = _uiState.value.match.undoPoint() ?: return
        tapHaptic()
        _uiState.update { it.copy(match = updated, restSeconds = 0) }
    }

    // ── End game at any time ──

    fun endGame() {
        gestureScorer.stop()
        MatchForegroundService.stop(app)
        try {
            val ended = _uiState.value.match.endManually()
            _uiState.update { it.copy(match = ended, screen = Screen.SUMMARY) }
            try { saveMatch(ended) } catch (e: Exception) {
                android.util.Log.e("WearScore", "Failed to save match", e)
            }
            try { wearSync.syncMatch(ended) } catch (_: Exception) {}
        } catch (e: Exception) {
            _uiState.update { it.copy(screen = Screen.SUMMARY) }
        }
    }

    // ── Continue between games ──

    fun continueGame() {
        _uiState.update { it.copy(match = it.match.startMatch()) }
    }

    // ── Rematch with same settings ──

    fun rematch() {
        val prev = _uiState.value.match
        gestureScorer.stop()
        MatchForegroundService.stop(app)
        val newMatch = Match().let {
            if (prev.isThreePlayer) {
                val names = prev.players.map { p -> p.name }
                it.setupThreePlayer(
                    names.getOrElse(0) { "You" },
                    names.getOrElse(1) { "Player 2" },
                    names.getOrElse(2) { "Player 3" },
                    selfScoreOnly = prev.selfScoreOnly,
                    indefinite = prev.indefinite,
                    simpleMode = prev.simpleMode,
                    sportName = prev.sportName
                )
            } else {
                val names = prev.players.map { p -> p.name }
                it.setupTwoPlayer(
                    names.getOrElse(0) { "You" },
                    names.getOrElse(1) { "Player 2" },
                    indefinite = prev.indefinite,
                    simpleMode = prev.simpleMode,
                    sportName = prev.sportName
                )
            }
        }
        _uiState.update { it.copy(match = newMatch) }
        startMatch()
    }

    private fun finishMatch(match: Match) {
        gestureScorer.stop()
        MatchForegroundService.stop(app)
        if (_uiState.value.voiceEnabled) {
            tts.announceMatchWon(match.winner?.name ?: "")
        }
        _uiState.update { it.copy(match = match, screen = Screen.SUMMARY) }
        try { saveMatch(match) } catch (e: Exception) {
            android.util.Log.e("WearScore", "Failed to save match", e)
        }
        try { wearSync.syncMatch(match) } catch (_: Exception) {}
    }

    // ── Navigation ──

    fun newMatch() {
        gestureScorer.stop()
        MatchForegroundService.stop(app)
        _uiState.update { UiState(voiceEnabled = _uiState.value.voiceEnabled, gestureScoring = _uiState.value.gestureScoring, gyroAvailable = _uiState.value.gyroAvailable) }
    }

    fun showHistory() {
        _uiState.update { it.copy(
            history = repo.loadMatches(),
            screen = Screen.HISTORY
        ) }
    }

    fun backToSetup() {
        _uiState.update { it.copy(screen = Screen.SETUP) }
    }

    // ── Persistence ──

    private fun saveMatch(match: Match) {
        repo.saveMatch(match)
    }

    fun deleteMatch(match: Match) {
        repo.deleteMatch(match)
        _uiState.update { it.copy(history = repo.loadMatches()) }
    }

    override fun onCleared() {
        super.onCleared()
        gestureScorer.stop()
        MatchForegroundService.stop(app)
        tts.shutdown()
    }

    companion object {
        private fun getVibrator(context: Context): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}
