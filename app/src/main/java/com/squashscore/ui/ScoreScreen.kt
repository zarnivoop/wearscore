package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
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
    restSeconds: Int,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onScore: (playerIndex: Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    onContinue: () -> Unit
) {
    val server = match.server
    val receiver = match.receiver
    val isResting = match.state == GameState.BETWEEN_GAMES

    Box(modifier = Modifier.fillMaxSize()) {
        if (isResting) {
            RestOverlay(seconds = restSeconds, onContinue = onContinue)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Server half (top)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20).copy(alpha = 0.6f))
                    .clickable(enabled = !isResting) { onScore(match.serverIndex) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        server.name,
                        style = MaterialTheme.typography.caption1,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        server.score.toString(),
                        style = MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        if (match.simpleMode) "Player 1" else "SERVING",
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFF4CAF50)
                    )
                    if (!match.simpleMode) {
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
                                if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
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
                    .background(Color(0xFF0D47A1).copy(alpha = 0.4f))
                    .clickable(enabled = !isResting) { onScore(match.receiverIndex) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        receiver.name,
                        style = MaterialTheme.typography.caption1,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        receiver.score.toString(),
                        style = MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        if (match.simpleMode) "Player 2" else "RECEIVING",
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFF64B5F6)
                    )
                    if (!match.simpleMode) {
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
private fun RestOverlay(seconds: Int, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Rest",
                style = MaterialTheme.typography.title2,
                color = Color(0xFFFFA726)
            )
            Text(
                "${seconds}s",
                style = MaterialTheme.typography.display2,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    }
}
