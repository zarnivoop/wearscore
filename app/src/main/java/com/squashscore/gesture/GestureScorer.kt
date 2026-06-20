package com.squashscore.gesture

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlin.math.sqrt

/**
 * Detects a wrist-twist gesture using the gyroscope and fires a callback.
 *
 * Gesture: quickly rotate your wrist like turning a doorknob
 * (palm up → palm down, or reverse).  Uses the gyroscope vector
 * magnitude so it works regardless of arm orientation.
 *
 * All side-effects (vibration, scoring callback) are posted to
 * the main thread — sensor callbacks run on a background thread
 * and must not touch TTS, Vibrator, or UI state directly.
 *
 * Debounced to prevent double-fires (1.5 s cooldown).
 * Only active during a match — start/stop controls sensor registration.
 */
class GestureScorer(
    private val sensorManager: SensorManager,
    private val vibrator: Vibrator?
) {

    private val gyroSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var enabled = false
    private var onScore: (() -> Unit)? = null
    private var twistActive = false
    private var lastFireMs = 0L
    private var eventCount = 0
    private var lastLogMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private val debounceMs = 1200L
    // ~46 deg/s — a relaxed wrist flick. Fires on the rising edge:
    // once gyro magnitude crosses this, score immediately (debounced).
    private val thresholdRadPerSec = 0.8f
    // gyro must drop below this before another twist can be recognized
    private val resetThreshold = 0.5f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!enabled) return

            // Use the gyroscope vector magnitude so a twist registers
            // regardless of how the user holds their arm.
            val mag = sqrt(
                event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
            )

            val now = System.currentTimeMillis()

            // Log a sample every ~2 seconds so we can see gyro values in logcat
            eventCount++
            if (now - lastLogMs > 2000) {
                Log.d(TAG, "gyro mag=%.3f  events=$eventCount  twistActive=$twistActive".format(mag))
                lastLogMs = now
                eventCount = 0
            }

            if (mag > thresholdRadPerSec && !twistActive && now - lastFireMs > debounceMs) {
                Log.i(TAG, "TWIST DETECTED — firing score  mag=%.3f".format(mag))
                twistActive = true
                lastFireMs = now
                // Post ALL side-effects to the main thread.
                // Sensor callbacks run on a background thread;
                // Vibrator and TTS both require the main looper.
                mainHandler.post {
                    try {
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(
                                50, VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                        onScore?.invoke()
                    } catch (_: Exception) {
                        // Guard against any remaining platform quirks
                    }
                }
            }

            // Reset the twist gate once gyro settles down
            if (twistActive && mag < resetThreshold) {
                twistActive = false
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /** Returns true if the device has a gyroscope. */
    val isAvailable: Boolean get() = gyroSensor != null

    /** Begin listening. Safe to call even if no gyroscope is present. */
    fun start(onScore: () -> Unit) {
        this.onScore = onScore
        enabled = true
        if (gyroSensor == null) {
            Log.w(TAG, "No gyroscope sensor on this device — gesture scoring unavailable")
            return
        }
        Log.d(TAG, "Registering gyro listener (threshold=${thresholdRadPerSec} rad/s)")
        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Stop listening and release the sensor. */
    fun stop() {
        enabled = false
        sensorManager.unregisterListener(listener)
        // Remove any pending callbacks that haven't fired yet
        mainHandler.removeCallbacksAndMessages(null)
        onScore = null
        twistActive = false
    }

    companion object {
        private const val TAG = "WearScore.Gesture"
    }
}
