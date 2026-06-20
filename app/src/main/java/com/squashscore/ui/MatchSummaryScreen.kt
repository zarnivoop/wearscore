package com.squashscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.squashscore.model.Match

@Composable
fun MatchSummaryScreen(
    match: Match,
    onNewMatch: () -> Unit,
    onRematch: () -> Unit = {}
) {
    val winner = match.winner
    val sportColor = sportAccent(match.sportName)

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Winner indicator — colored ring, no emoji
        item {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(sportColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "W",
                    style = MaterialTheme.typography.title1,
                    fontWeight = FontWeight.Bold,
                    color = sportColor
                )
            }
        }
        item {
            Text(
                winner?.name?.ifBlank { "?" } ?: "?",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                "wins ${winner?.gamesWon ?: 0}-${match.completedGames.size - (winner?.gamesWon ?: 0)}",
                style = MaterialTheme.typography.caption1,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        // Game scores
        if (match.completedGames.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            match.completedGames.forEach { game ->
                item {
                    Text(
                        "G${game.gameNumber}: ${game.scores.joinToString("-")}",
                        style = MaterialTheme.typography.caption2,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Indefinite/simple mode — show final scores
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Text(
                    match.players.joinToString(" - ") { it.score.toString() },
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Rematch button — same settings, new match
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRematch,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Text("Rematch")
            }
        }

        // New match button — back to setup
        item {
            Button(
                onClick = onNewMatch,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("New Match")
            }
        }
    }
}
