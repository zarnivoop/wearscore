package com.squashscore.data

import android.content.Context
import com.squashscore.model.Match
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple JSON-file persistence. No Room/DB needed for a score tracker.
 */
class MatchRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("squashscore", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saveMatch(match: Match) {
        val matches = loadMatches().toMutableList().apply { add(match) }
        if (matches.size > 50) {
            matches.removeAt(0)
        }
        prefs.edit().putString("history", json.encodeToString(matches)).apply()
    }

    fun loadMatches(): List<Match> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Match>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteMatch(match: Match) {
        val matches = loadMatches().filter { it.id != match.id }
        prefs.edit().putString("history", json.encodeToString(matches)).apply()
    }
}
