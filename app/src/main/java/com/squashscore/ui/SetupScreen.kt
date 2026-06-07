package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.squashscore.model.Sport

@Composable
fun SetupScreen(
    onStartTwoPlayer: (String, String, Boolean, Boolean, String) -> Unit,
    onStartThreePlayer: (String, String, String, Boolean, Boolean, Boolean, String) -> Unit,
    onShowHistory: () -> Unit,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    savedPlayer1Name: String = "",
    currentSport: Sport = Sport.SQUASH,
    onSportChanged: (Sport) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showSportPicker by remember { mutableStateOf(false) }
    var playerCount by remember { mutableIntStateOf(2) }
    var playerA by remember { mutableStateOf(savedPlayer1Name) }
    var playerB by remember { mutableStateOf("") }
    var playerC by remember { mutableStateOf("") }
    var selfScoreOnly by remember { mutableStateOf(true) }
    var indefinite by remember { mutableStateOf(true) }

    if (showSettings) {
        SettingsScreen(
            currentSport = currentSport,
            onSportChanged = { s ->
                onSportChanged(s)
                showSettings = false
            },
            playerCount = playerCount,
            onPlayerCountChange = { playerCount = it },
            playerA = playerA,
            onPlayerAChange = { playerA = it },
            playerB = playerB,
            onPlayerBChange = { playerB = it },
            playerC = playerC,
            onPlayerCChange = { playerC = it },
            selfScoreOnly = selfScoreOnly,
            onSelfScoreOnlyChange = { selfScoreOnly = it },
            indefinite = indefinite,
            onIndefiniteChange = { indefinite = it },
            voiceEnabled = voiceEnabled,
            onVoiceEnabledChanged = onVoiceEnabledChanged,
            onStart = { p1, p2, p3, selfOnly, indf ->
                if (playerCount == 3) {
                    onStartThreePlayer(p1, p2, p3, selfOnly, indf, selfOnly, currentSport.name)
                } else {
                    onStartTwoPlayer(p1, p2, indf, false, currentSport.name)
                }
            },
            onBack = { showSettings = false }
        )
    } else if (showSportPicker) {
        SportPickerScreen(
            current = currentSport,
            onSelect = { s ->
                onSportChanged(s)
                showSportPicker = false
            },
            onBack = { showSportPicker = false }
        )
    } else {
        QuickStartScreen(
            sport = currentSport,
            playerCount = playerCount,
            onPlayerCountChange = { playerCount = it },
            onSelectSport = { showSportPicker = true },
            onStart = {
                val p1 = playerA.ifBlank { "Player 1" }
                val p2 = playerB.ifBlank { "Player 2" }
                if (playerCount == 3) {
                    val p3 = playerC.ifBlank { "Player 3" }
                    onStartThreePlayer(p1, p2, p3, selfScoreOnly, true, true, currentSport.name)
                } else {
                    onStartTwoPlayer(p1, p2, true, true, currentSport.name)
                }
            },
            onSettings = { showSettings = true },
            onHistory = onShowHistory
        )
    }
}

// ── Quick start (minimal) view ──

@Composable
private fun QuickStartScreen(
    sport: Sport,
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    onSelectSport: () -> Unit,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Chip(
                onClick = onSelectSport,
                label = { Text(sport.displayName, style = MaterialTheme.typography.title3) },
                colors = ChipDefaults.primaryChipColors()
            )
        }

        item {
            Chip(
                onClick = { onPlayerCountChange(if (playerCount == 2) 3 else 2) },
                label = { Text("${playerCount} Players") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 8.dp)
            ) {
                Text("Start", style = MaterialTheme.typography.title2)
            }
        }

        item {
            Button(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("Settings", style = MaterialTheme.typography.caption1)
            }
        }

        item {
            Button(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(Icons.Default.History, contentDescription = "History")
                Text("History", style = MaterialTheme.typography.caption1)
            }
        }
    }
}

// ── Sport picker (scrolling list) ──

@Composable
private fun SportPickerScreen(
    current: Sport,
    onSelect: (Sport) -> Unit,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("Select Sport", style = MaterialTheme.typography.title3)
        }

        Sport.entries.forEach { sport ->
            item {
                Chip(
                    onClick = { onSelect(sport) },
                    label = {
                        if (sport == current) {
                            Text("${sport.displayName}  ", style = MaterialTheme.typography.title2)
                        } else {
                            Text(sport.displayName, style = MaterialTheme.typography.caption1)
                        }
                    },
                    icon = if (sport == current) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) }
                    } else null,
                    colors = if (sport == current) ChipDefaults.primaryChipColors()
                        else ChipDefaults.secondaryChipColors()
                )
            }
        }

        item {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("Back")
            }
        }
    }
}

// ── Full settings view ──

@Composable
private fun SettingsScreen(
    currentSport: Sport,
    onSportChanged: (Sport) -> Unit,
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    playerA: String,
    onPlayerAChange: (String) -> Unit,
    playerB: String,
    onPlayerBChange: (String) -> Unit,
    playerC: String,
    onPlayerCChange: (String) -> Unit,
    selfScoreOnly: Boolean,
    onSelfScoreOnlyChange: (Boolean) -> Unit,
    indefinite: Boolean,
    onIndefiniteChange: (Boolean) -> Unit,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onStart: (String, String, String, Boolean, Boolean) -> Unit,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.title3)
        }

        // Sport
        item {
            Chip(
                onClick = { onSportChanged(Sport.fromIndex((currentSport.ordinal + 1) % Sport.entries.size)) },
                label = { Text(currentSport.displayName) },
                colors = ChipDefaults.primaryChipColors()
            )
        }

        // Player count
        item {
            Chip(
                onClick = { onPlayerCountChange(if (playerCount == 2) 3 else 2) },
                label = { Text("${playerCount} Players") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        // Names
        item {
            PlayerNameField(value = playerA, onValueChange = onPlayerAChange, placeholder = "Player 1")
        }
        item {
            PlayerNameField(value = playerB, onValueChange = onPlayerBChange, placeholder = "Player 2")
        }
        if (playerCount == 3) {
            item {
                PlayerNameField(value = playerC, onValueChange = onPlayerCChange, placeholder = "Player 3")
            }
            item {
                ToggleChip(
                    checked = selfScoreOnly,
                    onCheckedChange = onSelfScoreOnlyChange,
                    label = { Text(if (selfScoreOnly) "Count own score" else "Track all") },
                    toggleControl = { Switch(checked = selfScoreOnly) }
                )
            }
        }

        // Indefinite scoring
        item {
            ToggleChip(
                checked = indefinite,
                onCheckedChange = onIndefiniteChange,
                label = { Text(if (indefinite) "Indefinite" else "To 11") },
                toggleControl = { Switch(checked = indefinite) }
            )
        }

        // Voice toggle
        item {
            ToggleChip(
                checked = voiceEnabled,
                onCheckedChange = onVoiceEnabledChanged,
                label = { Text("Voice") },
                toggleControl = { Switch(checked = voiceEnabled) }
            )
        }

        // Start
        item {
            Button(
                onClick = {
                    val p1 = playerA.ifBlank { "Player 1" }
                    val p2 = playerB.ifBlank { "Player 2" }
                    val p3 = playerC.ifBlank { "Player 3" }
                    onStart(p1, p2, p3, selfScoreOnly, indefinite)
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("Start")
            }
        }

        // Back
        item {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("Back")
            }
        }
    }
}

// ── Shared name field ──

@Composable
private fun PlayerNameField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.Gray,
                style = MaterialTheme.typography.caption1
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
