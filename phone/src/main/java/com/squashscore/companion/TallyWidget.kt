package com.squashscore.companion

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phone home screen widget showing latest match results.
 * Updated automatically when the watch sends match data.
 */
class TallyWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val widgetName = ComponentName(context, TallyWidget::class.java)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(widgetName)
            for (id in ids) {
                val views = buildRemoteViews(context)
                manager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val prefs = context.getSharedPreferences(DataReceiverService.PREFS_NAME, Context.MODE_PRIVATE)

            val winner = prefs.getString("lastWinner", null)
            if (winner != null) {
                val gamesWon = prefs.getInt("lastGamesWon", 0)
                val gamesLost = prefs.getInt("lastGamesLost", 0)
                val time = prefs.getLong("lastMatchTime", System.currentTimeMillis())
                val avgHr = prefs.getFloat("lastAvgHR", -1f)
                val calories = prefs.getFloat("lastCalories", -1f)

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
                views.setTextViewText(R.id.widget_winner, "$winner wins $gamesWon-$gamesLost")
                views.setTextViewText(R.id.widget_time, timeStr)

                if (avgHr > 0 && calories > 0) {
                    views.setTextViewText(R.id.widget_health, "♥ ${avgHr.toInt()} bpm  |  ${calories.toInt()} kcal")
                } else if (avgHr > 0) {
                    views.setTextViewText(R.id.widget_health, "♥ ${avgHr.toInt()} bpm")
                } else if (calories > 0) {
                    views.setTextViewText(R.id.widget_health, "${calories.toInt()} kcal")
                } else {
                    views.setTextViewText(R.id.widget_health, "")
                }
            } else {
                views.setTextViewText(R.id.widget_winner, "Tally")
                views.setTextViewText(R.id.widget_time, "Start a match")
                views.setTextViewText(R.id.widget_health, "on your watch")
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            return views
        }
    }
}
