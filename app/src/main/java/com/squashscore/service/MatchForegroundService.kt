package com.squashscore.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service that keeps WearScore alive during an active match.
 *
 * On Wear OS, a PARTIAL_WAKE_LOCK alone is NOT sufficient — the system
 * can still kill the app process. A foreground service with a persistent
 * notification is the only reliable way to prevent the CPU from sleeping
 * mid-match.
 *
 * State-of-the-art implementation:
 * - ServiceCompat.startForeground with explicit FOREGROUND_SERVICE_TYPE_SPECIAL_USE
 *   (required on API 34+ / Android 14+)
 * - Wake lock with 4-hour safety timeout (prevents indefinite lock if app crashes)
 * - onTaskRemoved: stop service when user swipes away
 * - stopForeground(STOP_FOREGROUND_REMOVE) in onDestroy for clean notification removal
 * - START_STICKY for restart after system kill
 */
class MatchForegroundService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        private const val CHANNEL_ID = "wearscore_match"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L  // 4 hours safety net

        fun start(context: Context) {
            val intent = Intent(context, MatchForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MatchForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "wearscore:match:foreground"
        )
        // Safety timeout: if the app crashes or the service is killed without
        // calling onDestroy, the wake lock auto-releases after 4 hours.
        // Under normal operation, released explicitly in onDestroy.
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Match in progress")
            .setContentText("WearScore is tracking your match")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        // Use ServiceCompat.startForeground with explicit type for API 34+ compatibility.
        // On API 34+ (Android 14), startForeground must declare the foreground service type.
        // ServiceCompat handles the version check internally.
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User swiped the app away from recents — stop the service.
        // On Wear OS this is rare but can happen via the app drawer.
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Remove the foreground notification cleanly before stopping.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Match tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while a match is in progress"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
