package com.squashscore.ui

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

    // Track which match is in "confirm delete" state
    var pendingDelete by remember { mutableStateOf<Match?>(null) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("History", style = MaterialTheme.typography.title3)
        }

        if (pendingDelete != null) {
            // Delete confirmation
            item {
                val match = pendingDelete!!
                Card(
                    onClick = {
                        onDelete(match)
                        pendingDelete = null
                    },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Delete this match?",
                            style = MaterialTheme.typography.caption1,
                            color = Color(0xFFEF5350)
                        )
                        Text(
                            dateFormatter.format(match.createdAt),
                            style = MaterialTheme.typography.caption3,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            "Tap to confirm",
                            style = MaterialTheme.typography.caption3,
                            color = Color(0xFFEF5350)
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { pendingDelete = null },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("Cancel", style = MaterialTheme.typography.caption2)
                }
            }
        } else {
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
                    val winnerName = match.winner?.name?.ifBlank { "?" } ?: "?"
                    val isThreePlayer = match.isThreePlayer
                    val players = if (isThreePlayer) {
                        match.players.map { it.name.ifBlank { "?" } }.joinToString(", ")
                    } else {
                        match.players.map { it.name.ifBlank { "?" } }.joinToString(" vs ")
                    }

                    item {
                        // Long press to initiate delete, tap does nothing (prevents accidental delete)
                        Card(
                            onClick = { pendingDelete = match },
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
                                Text(
                                    "Tap to delete",
                                    style = MaterialTheme.typography.caption3,
                                    color = Color.White.copy(alpha = 0.25f)
                                )
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
