package com.squashscore.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.squashscore.model.Sport
import com.squashscore.viewmodel.MatchViewModel

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private val vm: MatchViewModel by viewModels()
    private lateinit var ambientController: AmbientModeSupport.AmbientController
    private val isAmbient = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ambientController = AmbientModeSupport.attach(this)

        setContent {
            val state by vm.uiState.collectAsState()
            var currentSport = vm.loadLastSport()

            // Only keep CPU alive during an active game — setup/summary
            // screens should let the watch dim and sleep normally.
            val keepScreenOn = state.screen == MatchViewModel.Screen.PLAYING
            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            when (state.screen) {
                MatchViewModel.Screen.SETUP -> {
                    SetupScreen(
                        onStartTwoPlayer = { a, b, indefinite, simpleMode, sportName ->
                            vm.saveLastSport(Sport.fromName(sportName))
                            vm.setupTwoPlayer(a, b, indefinite, simpleMode, sportName)
                        },
                        onStartThreePlayer = { a, b, c, selfOnly, indefinite, simpleMode, sportName ->
                            vm.saveLastSport(Sport.fromName(sportName))
                            vm.setupThreePlayer(a, b, c, selfScoreOnly = selfOnly, indefinite = indefinite, simpleMode = simpleMode, sportName = sportName)
                        },
                        onShowHistory = { vm.showHistory() },
                        voiceEnabled = state.voiceEnabled,
                        onVoiceEnabledChanged = { vm.setVoiceEnabled(it) },
                        savedPlayer1Name = vm.loadPlayer1Name(),
                        currentSport = currentSport,
                        onSportChanged = { s ->
                            currentSport = s
                            vm.saveLastSport(s)
                        }
                    )
                }
                MatchViewModel.Screen.PLAYING -> {
                    if (state.match.isThreePlayer) {
                        ThreePlayerScreen(
                            match = state.match,
                            voiceEnabled = state.voiceEnabled,
                            onVoiceEnabledChanged = { vm.setVoiceEnabled(it) },
                            onScore = { vm.scorePoint(it) },
                            onUndo = { vm.undo() },
                            onEndGame = { vm.endGame() },
                            isAmbient = isAmbient.value
                        )
                    } else {
                        ScoreScreen(
                            match = state.match,
                            voiceEnabled = state.voiceEnabled,
                            onVoiceEnabledChanged = { vm.setVoiceEnabled(it) },
                            onScore = { vm.scorePoint(it) },
                            onUndo = { vm.undo() },
                            onEndGame = { vm.endGame() },
                            onContinue = { vm.continueGame() },
                            isAmbient = isAmbient.value
                        )
                    }
                }
                MatchViewModel.Screen.SUMMARY -> {
                    MatchSummaryScreen(
                        match = state.match,
                        onNewMatch = { vm.newMatch() }
                    )
                }
                MatchViewModel.Screen.HISTORY -> {
                    HistoryScreen(
                        matches = state.history,
                        onBack = { vm.backToSetup() },
                        onDelete = { vm.deleteMatch(it) }
                    )
                }
            }
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                isAmbient.value = true
            }
            override fun onExitAmbient() {
                isAmbient.value = false
            }
            override fun onUpdateAmbient() {
                // Periodic ambient refresh — keep the score visible
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.tts.shutdown()
    }
}
