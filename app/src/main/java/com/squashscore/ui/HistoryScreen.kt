package com.squashscore.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.squashscore.model.Match
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    matches: List<Match>,
    onBack: () -> Unit,
    onDelete: (Match) -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("History", style = MaterialTheme.typography.title3)
        }

        if (matches.isEmpty()) {
            item {
                Text(
                    "No matches yet",
                    style = MaterialTheme.typography.caption1,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            val sorted = matches.sortedByDescending { it.createdAt }
            sorted.forEach { match ->
                val scoreLine = buildScoreLine(match)
                val winnerName = match.winner?.name ?: "?"
                val isThreePlayer = match.isThreePlayer
                val players = if (isThreePlayer) {
                    match.players.map { it.name }.joinToString(", ")
                } else {
                    match.players.map { it.name }.joinToString(" vs ")
                }

                item {
                    Card(
                        onClick = { onDelete(match) },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                dateFormatter.format(match.createdAt),
                                style = MaterialTheme.typography.caption3,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                players,
                                style = MaterialTheme.typography.caption2,
                                maxLines = 1
                            )
                            Text(
                                scoreLine,
                                style = MaterialTheme.typography.caption1,
                                color = Color(0xFF4CAF50)
                            )
                            if (match.completedGames.isNotEmpty()) {
                                Text(
                                    "$winnerName won ${match.winner?.gamesWon ?: 0}-${match.completedGames.size - (match.winner?.gamesWon ?: 0)}",
                                    style = MaterialTheme.typography.caption3,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
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

private fun buildScoreLine(match: Match): String {
    return if (match.completedGames.isNotEmpty()) {
        match.completedGames.joinToString(", ") { game ->
            game.scores.joinToString("-")
        }
    } else {
        match.players.joinToString(" - ") { it.score.toString() }
    }
}
