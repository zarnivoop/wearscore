package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.squashscore.model.GameState
import com.squashscore.model.Match

@Composable
fun ScoreScreen(
    match: Match,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onScore: (playerIndex: Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    onContinue: () -> Unit,
    isAmbient: Boolean = false
) {
    val server = match.server
    val receiver = match.receiver
    val isResting = match.state == GameState.BETWEEN_GAMES
    if (server == null || receiver == null) return

    // Ambient: strip everything but scores — burn-in safe, readable at a glance.
    if (isAmbient) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = server.score.toString(),
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${server.name}  -  ${receiver.name}",
                    style = MaterialTheme.typography.caption1,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = receiver.score.toString(),
                    style = MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isResting) {
            BetweenGamesOverlay(
                onNextSet = onContinue,
                onEndGame = onEndGame,
                gamesPlayed = match.completedGames.size
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Server half (top)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(sportServerBg(match.sportName).copy(alpha = 0.6f))
                    .clickable(enabled = !isResting) { onScore(match.serverIndex) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        server.name.ifBlank { "Player 1" },
                        style = MaterialTheme.typography.caption1,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        server.score.toString(),
                        style = MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!match.simpleMode) {
                        Text(
                            "SERVING",
                            style = MaterialTheme.typography.caption3,
                            color = sportAccent(match.sportName)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(match.gamesNeeded) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (server.gamesWon > i) Color.White
                                            else Color.White.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Controls strip (between players — no space taken from either)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left group: sound + undo
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sound toggle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable(enabled = !isResting) { onVoiceEnabledChanged(!voiceEnabled) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (voiceEnabled) "Mute" else "Unmute",
                                modifier = Modifier.size(14.dp),
                                tint = if (voiceEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        // Undo
                        if (match.pointHistory.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable(enabled = !isResting) { onUndo() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\u21A9", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Center: game info (hide in simple mode)
                    if (!match.simpleMode) {
                        Text(
                            "G${match.currentGame}/${match.bestOf}",
                            style = MaterialTheme.typography.caption3,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // Right: end game
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B0000).copy(alpha = 0.6f))
                            .clickable(enabled = !isResting) { onEndGame() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u2715", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Receiver half (bottom)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(sportReceiverBg(match.sportName).copy(alpha = 0.4f))
                    .clickable(enabled = !isResting) { onScore(match.receiverIndex) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        receiver.name.ifBlank { "Player 2" },
                        style = MaterialTheme.typography.caption1,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        receiver.score.toString(),
                        style = MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!match.simpleMode) {
                        Text(
                            "RECEIVING",
                            style = MaterialTheme.typography.caption3,
                            color = sportAccent(match.sportName).copy(alpha = 0.8f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(match.gamesNeeded) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (receiver.gamesWon > i) Color.White
                                            else Color.White.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BetweenGamesOverlay(
    onNextSet: () -> Unit,
    onEndGame: () -> Unit,
    gamesPlayed: Int = 0
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Set won!",
                style = MaterialTheme.typography.title2,
                color = Color(0xFFFFA726)
            )
            if (gamesPlayed > 0) {
                Text(
                    "Set $gamesPlayed complete",
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNextSet,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Next set", style = MaterialTheme.typography.caption1)
                }
                Button(
                    onClick = onEndGame,
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("End game", style = MaterialTheme.typography.caption1)
                }
            }
        }
    }
}
