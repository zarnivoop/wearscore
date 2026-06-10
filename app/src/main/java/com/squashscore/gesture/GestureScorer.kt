package com.squashscore.gesture

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.abs

/**
 * Detects a wrist-twist gesture using the gyroscope and fires a callback.
 *
 * Gesture: quickly rotate your wrist like turning a doorknob
 * (palm up → palm down, or reverse). The watch-face axis (Z)
 * rotates rapidly, then returns to rest.
 *
 * Vibrates briefly on each detection so you feel the point register.
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

    private val debounceMs = 1500L
    private val thresholdRadPerSec = 1.5f   // ~86 deg/s — deliberate but not violent
    private val releaseThreshold = 0.6f     // must drop below this to register

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!enabled) return

            // event.values[2] = angular velocity around Z (watch-face axis)
            val velZ = abs(event.values[2])

            if (velZ > thresholdRadPerSec && !twistActive) {
                twistActive = true
            }

            if (twistActive && velZ < releaseThreshold) {
                val now = System.currentTimeMillis()
                if (now - lastFireMs > debounceMs) {
                    lastFireMs = now
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    onScore?.invoke()
                }
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
        gyroSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /** Stop listening and release the sensor. */
    fun stop() {
        enabled = false
        sensorManager.unregisterListener(listener)
        onScore = null
        twistActive = false
    }
}
