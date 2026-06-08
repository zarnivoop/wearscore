package com.squashscore.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.squashscore.model.Match
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Sends match results to the phone companion app via Wearable Data Layer.
 * Uses DataClient for reliable delivery. Gracefully degrades if no phone is paired.
 */
class WearDataSync(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Sync a completed match to the phone companion */
    fun syncMatch(match: Match) {
        if (!match.isMatchOver) return
        try {
            val dataMapReq = PutDataMapRequest.create(MATCH_PATH).apply {
                dataMap.putString("matchId", match.id)
                dataMap.putString("matchJson", json.encodeToString(match))
                dataMap.putLong("createdAt", match.createdAt.toEpochMilli())
                dataMap.putString("winner", match.winner?.name ?: "?")
                dataMap.putInt("gamesWon", match.winner?.gamesWon ?: 0)
                dataMap.putInt("gamesLost", match.completedGames.size - (match.winner?.gamesWon ?: 0))
            }
            Wearable.getDataClient(context).putDataItem(dataMapReq.asPutDataRequest())
                .addOnSuccessListener {
                    Log.i(TAG, "Match synced to phone")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Phone sync skipped (no paired phone?): ${e.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "Sync failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "WearDataSync"
        private const val MATCH_PATH = "/tally/match-result"
    }
}
