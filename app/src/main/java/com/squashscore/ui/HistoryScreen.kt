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
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault()) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Button(onClick = onBack) {
                Text("← Back")
            }
        }
        item {
            Text("History", style = MaterialTheme.typography.title3)
        }

        if (matches.isEmpty()) {
            item {
                Text("No matches yet", style = MaterialTheme.typography.caption1,
                    color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            val sorted = matches.sortedByDescending { it.createdAt }
            sorted.forEach { match ->
                item {
                    Card(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                match.players.map { it.name }.joinToString(" vs "),
                                style = MaterialTheme.typography.caption2,
                                maxLines = 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${match.winner?.name ?: "?"} won",
                                    style = MaterialTheme.typography.caption3,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    formatter.format(match.createdAt),
                                    style = MaterialTheme.typography.caption3,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
