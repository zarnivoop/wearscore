package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.squashscore.model.Match
import com.squashscore.model.Sport

// ── Sport color helpers (local copies — keep in sync with ScoreScreen.kt) ──

private fun sportServerBg(sportName: String): Color = when (Sport.fromName(sportName)) {
    Sport.SQUASH -> Color(0xFF1B5E20)
    Sport.BADMINTON -> Color(0xFF0D47A1)
    Sport.TENNIS -> Color(0xFF795548)
    Sport.TABLE_TENNIS -> Color(0xFF4A148C)
    Sport.PICKLEBALL -> Color(0xFF006064)
    Sport.RACQUETBALL -> Color(0xFFB71C1C)
    Sport.PADEL -> Color(0xFF2E7D32)
}

private fun sportAccent(sportName: String): Color = when (Sport.fromName(sportName)) {
    Sport.SQUASH -> Color(0xFF4CAF50)
    Sport.BADMINTON -> Color(0xFF64B5F6)
    Sport.TENNIS -> Color(0xFFFFB300)
    Sport.TABLE_TENNIS -> Color(0xFFAB47BC)
    Sport.PICKLEBALL -> Color(0xFF26C6DA)
    Sport.RACQUETBALL -> Color(0xFFEF5350)
    Sport.PADEL -> Color(0xFF66BB6A)
}

// ── Three player screen ──

/**
 * 3-player cut-throat screen.
 * Normal mode: two active players (server/receiver), one waiting.
 * Self-score mode: big counter, tap anywhere to increment own score.
 */
@Composable
fun ThreePlayerScreen(
    match: Match,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onScore: (playerIndex: Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    isAmbient: Boolean = false
) {
    if (match.selfScoreOnly) {
        SelfScoreScreen(
            score = match.players[0].score,
            name = match.players[0].name,
            sportName = match.sportName,
            hasUndo = match.pointHistory.isNotEmpty(),
            voiceEnabled = voiceEnabled,
            onVoiceEnabledChanged = onVoiceEnabledChanged,
            onScore = { onScore(0) },
            onUndo = onUndo,
            onEndGame = onEndGame,
            isAmbient = isAmbient
        )
    } else {
        CutThroatScreen(
            match = match,
            voiceEnabled = voiceEnabled,
            onVoiceEnabledChanged = onVoiceEnabledChanged,
            onScore = onScore,
            onUndo = onUndo,
            onEndGame = onEndGame,
            isAmbient = isAmbient
        )
    }
}

// ── Self-score mode: simple tap counter ──

@Composable
private fun SelfScoreScreen(
    score: Int,
    name: String,
    sportName: String,
    hasUndo: Boolean,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onScore: () -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    isAmbient: Boolean = false
) {
    // Ambient: just the number — burn-in safe, no interactive elements.
    if (isAmbient) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$name: $score",
                style = MaterialTheme.typography.display2,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Big tap area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sportServerBg(sportName).copy(alpha = 0.6f))
                .clickable { onScore() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    name,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    score.toString(),
                    style = MaterialTheme.typography.display1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "tap to +1",
                    style = MaterialTheme.typography.caption3,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onVoiceEnabledChanged(!voiceEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (voiceEnabled) "Mute" else "Unmute",
                        modifier = Modifier.size(18.dp),
                        tint = if (voiceEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
                    )
                }

                if (hasUndo) {
                    Button(onClick = onUndo, modifier = Modifier.height(36.dp)) {
                        Text("Undo", style = MaterialTheme.typography.caption2)
                    }
                }

                Button(onClick = onEndGame, modifier = Modifier.height(36.dp)) {
                    Text("End", style = MaterialTheme.typography.caption2)
                }
            }
        }
    }
}

// ── Cut-throat mode: full 3-player court ──

@Composable
private fun CutThroatScreen(
    match: Match,
    voiceEnabled: Boolean,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onScore: (playerIndex: Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    isAmbient: Boolean = false
) {
    val server = match.server ?: return
    val receiver = match.receiver ?: return
    val waiting = match.waitingPlayer

    // Ambient: compact scoreboard — no backgrounds, no interactivity.
    if (isAmbient) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = match.players.joinToString("  ·  ") { "${it.name}: ${it.score}" },
                    style = MaterialTheme.typography.title1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${server.name} serving",
                    style = MaterialTheme.typography.caption3,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                match.players.forEachIndexed { _, p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.name, style = MaterialTheme.typography.caption3,
                            color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                        Text(p.score.toString(), style = MaterialTheme.typography.title1,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(sportServerBg(match.sportName).copy(alpha = 0.6f))
                        .clickable { onScore(match.serverIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SERVE", style = MaterialTheme.typography.caption3,
                            color = sportAccent(match.sportName))
                        Text(server.name, style = MaterialTheme.typography.caption1,
                            color = Color.White)
                        Text("+1", style = MaterialTheme.typography.title2,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE65100).copy(alpha = 0.4f))
                        .clickable { onScore(match.receiverIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RECEIVE", style = MaterialTheme.typography.caption3,
                            color = sportAccent(match.sportName).copy(alpha = 0.6f))
                        Text(receiver.name, style = MaterialTheme.typography.caption1,
                            color = Color.White)
                        Text("+1", style = MaterialTheme.typography.title2,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (waiting != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WAITING", style = MaterialTheme.typography.caption3,
                            color = Color.White.copy(alpha = 0.4f))
                        Text(waiting.name, style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onVoiceEnabledChanged(!voiceEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (voiceEnabled) "Mute" else "Unmute",
                        modifier = Modifier.size(18.dp),
                        tint = if (voiceEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
                    )
                }

                if (match.pointHistory.isNotEmpty()) {
                    Button(onClick = onUndo, modifier = Modifier.height(36.dp)) {
                        Text("Undo", style = MaterialTheme.typography.caption2)
                    }
                }

                Button(onClick = onEndGame, modifier = Modifier.height(36.dp)) {
                    Text("End", style = MaterialTheme.typography.caption2)
                }
            }
        }
    }
}
