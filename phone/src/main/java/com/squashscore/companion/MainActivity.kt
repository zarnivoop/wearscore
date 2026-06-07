package com.squashscore.companion

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Minimal companion activity.
 * The phone app is just a container for the widget — no full UI needed.
 * Shows the last received match result when opened.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = buildString {
                val prefs = getSharedPreferences(DataReceiverService.PREFS_NAME, MODE_PRIVATE)
                val winner = prefs.getString("lastWinner", null)
                if (winner != null) {
                    appendLine("SquashScore")
                    appendLine()
                    appendLine("Last match: $winner wins")
                    appendLine("${prefs.getInt("lastGamesWon", 0)} - ${prefs.getInt("lastGamesLost", 0)}")
                    val hr = prefs.getFloat("lastAvgHR", -1f)
                    if (hr > 0) appendLine("Avg HR: ${hr.toInt()} bpm")
                    val cal = prefs.getFloat("lastCalories", -1f)
                    if (cal > 0) appendLine("Calories: ${cal.toInt()} kcal")
                } else {
                    appendLine("SquashScore")
                    appendLine()
                    appendLine("No matches yet.")
                    appendLine("Start a match on your watch!")
                }
            }
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }

        setContentView(textView)
    }
}
