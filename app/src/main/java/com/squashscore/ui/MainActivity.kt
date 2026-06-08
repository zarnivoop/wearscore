package com.squashscore.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.squashscore.model.Sport
import com.squashscore.viewmodel.MatchViewModel

import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private val vm: MatchViewModel by viewModels()
    private lateinit var ambientController: AmbientModeSupport.AmbientController
    private val isAmbient = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep CPU + screen alive during ambient — prevents the system
        // from dismissing the activity when the display dims.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ambientController = AmbientModeSupport.attach(this)

        setContent {
            val state by vm.uiState.collectAsState()
            var currentSport = vm.loadLastSport()

            // Pass ambient state so each screen can render a minimal,
            // black-and-white display that Wear OS recognises as ambient-aware.
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
                            restSeconds = state.restSeconds,
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
