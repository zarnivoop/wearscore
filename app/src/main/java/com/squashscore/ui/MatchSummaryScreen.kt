package com.squashscore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.squashscore.model.Match

@Composable
fun MatchSummaryScreen(
    match: Match,
    onNewMatch: () -> Unit
) {
    val winner = match.winner

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(if (winner != null) "\uD83C\uDFC6" else "?", style = MaterialTheme.typography.display3)
        }
        item {
            Text(
                winner?.name ?: "?",
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

        // New match
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNewMatch,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("New Match")
            }
        }
    }
}
