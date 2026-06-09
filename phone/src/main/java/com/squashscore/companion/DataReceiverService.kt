package com.squashscore.companion

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives match results from the watch via Wearable Data Layer.
 * Updates the phone widget with latest match data.
 */
class DataReceiverService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == MATCH_PATH) {
                handleMatchData(event)
            }
        }
        dataEvents.release()
    }

    private fun handleMatchData(event: DataEvent) {
        try {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("lastWinner", dataMap.getString("winner"))
                putInt("lastGamesWon", dataMap.getInt("gamesWon"))
                putInt("lastGamesLost", dataMap.getInt("gamesLost"))
                putLong("lastMatchTime", dataMap.getLong("createdAt"))
                dataMap.getString("matchJson")?.let { putString("lastMatchJson", it) }
                if (dataMap.containsKey("avgHeartRate")) {
                    putFloat("lastAvgHR", dataMap.getFloat("avgHeartRate"))
                }
                if (dataMap.containsKey("totalCalories")) {
                    putFloat("lastCalories", dataMap.getFloat("totalCalories"))
                }
                apply()
            }

            // Update the widget
            updateWidget()

            Log.i(TAG, "Match data received from watch: ${dataMap.getString("winner")}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to process match data", e)
        }
    }

    private fun updateWidget() {
        val widgetName = ComponentName(this, WearScoreWidget::class.java)
        val manager = AppWidgetManager.getInstance(this)
        manager.notifyAppWidgetViewDataChanged(
            manager.getAppWidgetIds(widgetName),
            android.R.id.widget_frame
        )
    }

    companion object {
        private const val TAG = "DataReceiver"
        private const val MATCH_PATH = "/wearscore/match-result"
        const val PREFS_NAME = "wearscore_widget"
    }
}
