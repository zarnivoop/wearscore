package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.squashscore.model.Sport

/**
 * Single-screen setup using ScalingLazyColumn for scrollability.
 * On round Wear OS screens the content overflows — must be scrollable.
 */
@Composable
fun SetupScreen(
    onStartTwoPlayer: (String, String, Boolean, Boolean, String) -> Unit,
    onStartThreePlayer: (String, String, String, Boolean, Boolean, Boolean, String) -> Unit,
    onShowHistory: () -> Unit,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    gestureScoring: Boolean = false,
    onGestureScoringChanged: (Boolean) -> Unit = {},
    gyroAvailable: Boolean = true,
    savedPlayer1Name: String = "",
    currentSport: Sport = Sport.SQUASH,
    onSportChanged: (Sport) -> Unit,
    savedSimpleMode: Boolean = true
) {
    var playerCount by remember { mutableIntStateOf(2) }
    var playerA by remember { mutableStateOf(savedPlayer1Name) }
    var playerB by remember { mutableStateOf("") }
    var playerC by remember { mutableStateOf("") }
    var simpleMode by remember { mutableStateOf(savedSimpleMode) }
    var indefinite by remember { mutableStateOf(true) }

    val sports = Sport.entries
    val sportColor = sportAccentColor(currentSport)

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Sport — tap to cycle ──
        item {
            Chip(
                onClick = {
                    val i = sports.indexOf(currentSport)
                    val next = sports[(i + 1) % sports.size]
                    onSportChanged(next)
                },
                label = { Text(currentSport.displayName, style = MaterialTheme.typography.title3) },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = sportColor.copy(alpha = 0.3f),
                    contentColor = Color.White
                )
            )
        }

        // ── Target score / mode display ──
        if (simpleMode && playerCount == 2) {
            item {
                Text(
                    "Counter mode",
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        } else if (!simpleMode && !indefinite && playerCount == 2 && currentSport.usesStandardScoring) {
            item {
                Text(
                    "To ${currentSport.defaultTarget}, best of 5",
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // ── Player count ──
        item {
            Chip(
                onClick = { playerCount = if (playerCount == 2) 3 else 2 },
                label = { Text("${playerCount} players") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        // ── Scoring mode toggle (2-player, standard scoring sports only) ──
        if (playerCount == 2 && currentSport.usesStandardScoring) {
            item {
                Chip(
                    onClick = { simpleMode = !simpleMode },
                    label = { Text(if (simpleMode) "Simple" else "Full scoring") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            // Target toggle — only in Full scoring (Simple is always indefinite)
            if (!simpleMode) {
                item {
                    Chip(
                        onClick = { indefinite = !indefinite },
                        label = { Text(if (indefinite) "Indefinite" else "To ${currentSport.defaultTarget}") },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }

        // ── Names ──
        item {
            PlayerNameField(value = playerA, onValueChange = { playerA = it }, placeholder = "You")
        }
        item {
            PlayerNameField(value = playerB, onValueChange = { playerB = it }, placeholder = "Player 2")
        }
        if (playerCount == 3) {
            item {
                PlayerNameField(value = playerC, onValueChange = { playerC = it }, placeholder = "Player 3")
            }
        }

        // ── START ──
        item {
            Button(
                onClick = {
                    val p1 = playerA.ifBlank { "You" }
                    val p2 = playerB.ifBlank { "Player 2" }
                    if (playerCount == 3) {
                        val p3 = playerC.ifBlank { "Player 3" }
                        onStartThreePlayer(p1, p2, p3, true, true, true, currentSport.name)
                    } else {
                        // Simple mode is always indefinite (counter).
                        // Full scoring respects the indefinite toggle.
                        val effectiveIndefinite = simpleMode || indefinite
                        onStartTwoPlayer(p1, p2, effectiveIndefinite, simpleMode, currentSport.name)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("Start", style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold)
            }
        }

        // ── Bottom toggles ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomToggle(
                    label = if (voiceEnabled) "Voice" else "Muted",
                    active = voiceEnabled,
                    onClick = { onVoiceEnabledChanged(!voiceEnabled) }
                )
                BottomToggle(
                    label = if (!gyroAvailable) "No gyro"
                            else if (gestureScoring) "Twist"
                            else "Gesture",
                    active = gestureScoring,
                    enabled = gyroAvailable,
                    onClick = { if (gyroAvailable) onGestureScoringChanged(!gestureScoring) }
                )
                BottomToggle(
                    label = "History",
                    active = false,
                    onClick = onShowHistory
                )
            }
        }
    }
}

// ── Sport accent color (shared) ──

fun sportAccentColor(sport: Sport): Color = when (sport) {
    Sport.SQUASH -> Color(0xFF4CAF50)
    Sport.BADMINTON -> Color(0xFF64B5F6)
    Sport.TENNIS -> Color(0xFFFFB300)
    Sport.TABLE_TENNIS -> Color(0xFFAB47BC)
    Sport.PICKLEBALL -> Color(0xFF26C6DA)
    Sport.RACQUETBALL -> Color(0xFFEF5350)
    Sport.PADEL -> Color(0xFF66BB6A)
}

fun sportServerBg(sportName: String): Color = when (Sport.fromName(sportName)) {
    Sport.SQUASH -> Color(0xFF1B5E20)
    Sport.BADMINTON -> Color(0xFF0D47A1)
    Sport.TENNIS -> Color(0xFF795548)
    Sport.TABLE_TENNIS -> Color(0xFF4A148C)
    Sport.PICKLEBALL -> Color(0xFF006064)
    Sport.RACQUETBALL -> Color(0xFFB71C1C)
    Sport.PADEL -> Color(0xFF2E7D32)
}

fun sportReceiverBg(sportName: String): Color = when (Sport.fromName(sportName)) {
    Sport.SQUASH -> Color(0xFF0D47A1)
    Sport.BADMINTON -> Color(0xFF4A148C)
    Sport.TENNIS -> Color(0xFFF57F17)
    Sport.TABLE_TENNIS -> Color(0xFF1565C0)
    Sport.PICKLEBALL -> Color(0xFF00838F)
    Sport.RACQUETBALL -> Color(0xFF1B5E20)
    Sport.PADEL -> Color(0xFF558B2F)
}

fun sportAccent(sportName: String): Color = sportAccentColor(Sport.fromName(sportName))

// ── Small circular toggle used in the bottom row ──

@Composable
private fun BottomToggle(
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !enabled -> Color(0xFF2A2A2A)
                        active -> Color(0xFF1B5E20)
                        else -> Color.White.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (active) "\u2713" else "",
                color = if (active) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = when {
                !enabled -> Color(0xFFCF6679).copy(alpha = 0.7f)
                active -> Color.White
                else -> Color.White.copy(alpha = 0.4f)
            }
        )
    }
}

// ── Name field ──

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
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
