package com.squashscore.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import com.squashscore.data.MatchRepository
import com.squashscore.data.WearDataSync
import com.squashscore.gesture.GestureScorer
import com.squashscore.model.GameState
import com.squashscore.model.Match
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
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    )

    private val wakeLock: PowerManager.WakeLock by lazy {
        val pm = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wearscore:match")
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val match: Match = Match(),
        val history: List<Match> = emptyList(),
        val screen: Screen = Screen.SETUP,
        val restSeconds: Int = 0,
        val voiceEnabled: Boolean = true,
        val gestureScoring: Boolean = false
    )

    enum class Screen { SETUP, PLAYING, SUMMARY, HISTORY }

    // ── Persistent player 1 name ──

    private val prefs = application.getSharedPreferences("squashscore", Context.MODE_PRIVATE)

    fun loadPlayer1Name(): String {
        return prefs.getString("player1_name", "") ?: ""
    }

    fun savePlayer1Name(name: String) {
        prefs.edit().putString("player1_name", name).apply()
    }

    // ── Persistent last sport ──

    fun loadLastSport(): com.squashscore.model.Sport {
        val name = prefs.getString("last_sport", null) ?: return com.squashscore.model.Sport.SQUASH
        return com.squashscore.model.Sport.fromName(name)
    }

    fun saveLastSport(sport: com.squashscore.model.Sport) {
        prefs.edit().putString("last_sport", sport.name).apply()
    }

    // ── Setup ──

    fun setupTwoPlayer(
        playerA: String, playerB: String,
        indefinite: Boolean = false,
        simpleMode: Boolean = false,
        sportName: String = com.squashscore.model.Sport.SQUASH.name
    ) {
        savePlayer1Name(playerA)
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
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L) // 10-minute timeout as safety net
        }
    }

    private fun startGestureScoring() {
        if (!gestureScorer.isAvailable) return
        gestureScorer.start { scorePoint(0) }
    }

    // ── Scoring ──

    fun scorePoint(playerIndex: Int) {
        val current = _uiState.value.match
        val updated = current.awardPoint(playerIndex) ?: return

        val newScorerScore = current.players[playerIndex].score + 1
        val isThreePlayer = current.isThreePlayer
        val silent = current.selfScoreOnly  // self-score mode is silent
        val opponentScore = if (!isThreePlayer) current.players[1 - playerIndex].score else 0

        when (updated.state) {
            GameState.FINISHED -> {
                if (_uiState.value.voiceEnabled && !silent) {
                    tts.speak("${newScorerScore}-${opponentScore}.")
                }
                finishMatch(updated)
            }
            GameState.BETWEEN_GAMES -> {
                if (_uiState.value.voiceEnabled && !silent) {
                    tts.speak("${newScorerScore}-${opponentScore}.")
                }
                val gameWinner = updated.completedGames.last()
                if (_uiState.value.voiceEnabled) {
                    val name = updated.players[gameWinner.winnerIndex].name
                    val won = updated.players[gameWinner.winnerIndex].gamesWon
                    val lost = updated.completedGames.size - won
                    tts.announceGameWon(name, won, lost)
                }
                _uiState.update { it.copy(match = updated) }
            }
            else -> {
                _uiState.update { it.copy(match = updated) }
                if (_uiState.value.voiceEnabled && !silent) {
                    if (!isThreePlayer && updated.serverIndex != current.serverIndex) {
                        val srv = updated.server!!
                        val rcv = updated.receiver!!
                        tts.speak("${srv.name} serving. ${srv.score}-${rcv.score}.")
                    } else {
                        tts.speak("${newScorerScore}-${opponentScore}.")
                    }
                }
            }
        }
    }

    fun undo() {
        val updated = _uiState.value.match.undoPoint() ?: return
        _uiState.update { it.copy(match = updated, restSeconds = 0) }
    }

    // ── End game at any time ──

    fun endGame() {
        gestureScorer.stop()
        releaseWakeLock()
        try {
            val ended = _uiState.value.match.endManually()
            _uiState.update { it.copy(match = ended, screen = Screen.SUMMARY) }
            try { saveMatch(ended) } catch (_: Exception) {}
            try { wearSync.syncMatch(ended) } catch (_: Exception) {}
        } catch (e: Exception) {
            // Fallback: force navigation to summary
            _uiState.update { it.copy(screen = Screen.SUMMARY) }
        }
    }

    // ── Continue between games ──

    fun continueGame() {
        _uiState.update { it.copy(match = it.match.startMatch()) }
    }

    private fun finishMatch(match: Match) {
        gestureScorer.stop()
        releaseWakeLock()
        if (_uiState.value.voiceEnabled) {
            tts.announceMatchWon(match.winner?.name ?: "")
        }
        _uiState.update { it.copy(match = match, screen = Screen.SUMMARY) }
        try { saveMatch(match) } catch (_: Exception) {}
        try { wearSync.syncMatch(match) } catch (_: Exception) {}
    }

    // ── Navigation ──

    fun newMatch() {
        gestureScorer.stop()
        releaseWakeLock()
        _uiState.update { UiState(voiceEnabled = _uiState.value.voiceEnabled, gestureScoring = _uiState.value.gestureScoring) }
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
        releaseWakeLock()
        tts.shutdown()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
