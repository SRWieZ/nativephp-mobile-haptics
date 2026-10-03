package com.blessedzulu.plugins.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.bridge.BridgeError
import com.nativephp.mobile.bridge.BridgeFunction

/**
 * Haptic feedback functions backed by Android's VibrationEffect API.
 * Namespace: "Haptics.*"
 */
object HapticsFunctions {

    /** Obtain the Vibrator service across API levels. */
    private fun vibrator(activity: FragmentActivity): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // MARK: - Haptics.Impact

    class Impact(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val vibrator = vibrator(activity)
            if (vibrator == null || !vibrator.hasVibrator()) {
                return mapOf("success" to false)
            }

            val (duration, amplitude) = when (parameters["style"] as? String) {
                "light" -> 20L to 40
                "heavy" -> 40L to 255
                "rigid" -> 15L to 200
                "soft" -> 50L to 60
                else -> 30L to 128 // medium
            }

            // Optional repeat: one waveform of `times` pulses, `interval` ms
            // apart (start to start). Separate vibrate() calls would cancel
            // each other, so a burst has to be a single effect.
            val times = ((parameters["times"] as? Number)?.toInt() ?: 1).coerceIn(1, 100)
            val interval = ((parameters["interval"] as? Number)?.toLong() ?: 50L).coerceAtLeast(15L)

            if (times == 1) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                val pulse = duration.coerceAtMost(interval - 5)
                val timings = LongArray(times * 2) { i -> if (i % 2 == 0) (if (i == 0) 0L else interval - pulse) else pulse }
                val amplitudes = IntArray(times * 2) { i -> if (i % 2 == 0) 0 else amplitude }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            return mapOf("success" to true)
        }
    }

    // MARK: - Haptics.Notification

    class Notification(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val vibrator = vibrator(activity)
            if (vibrator == null || !vibrator.hasVibrator()) {
                return mapOf("success" to false)
            }

            val type = parameters["type"] as? String ?: "success"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId = when (type) {
                    "warning" -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    "error" -> VibrationEffect.EFFECT_HEAVY_CLICK
                    else -> VibrationEffect.EFFECT_CLICK // success
                }
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else {
                val (timings, amplitudes) = when (type) {
                    "warning" -> longArrayOf(0, 30, 50, 30) to intArrayOf(0, 180, 0, 180)
                    "error" -> longArrayOf(0, 50, 40, 50, 40, 50) to intArrayOf(0, 255, 0, 200, 0, 255)
                    else -> longArrayOf(0, 30) to intArrayOf(0, 128) // success
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }

            return mapOf("success" to true)
        }
    }

    // MARK: - Haptics.Selection

    class Selection(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val vibrator = vibrator(activity)
            if (vibrator == null || !vibrator.hasVibrator()) {
                return mapOf("success" to false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 60))
            }

            return mapOf("success" to true)
        }
    }

    // MARK: - Haptics.Vibrate

    class Vibrate(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val vibrator = vibrator(activity)
            if (vibrator == null || !vibrator.hasVibrator()) {
                return mapOf("success" to false)
            }

            val duration = ((parameters["duration"] as? Number)?.toLong() ?: 200L).coerceIn(1, 5000)
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            return mapOf("success" to true)
        }
    }

    // MARK: - Haptics.Pattern

    class Pattern(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            val vibrator = vibrator(activity)
            if (vibrator == null || !vibrator.hasVibrator()) {
                return mapOf("success" to false)
            }

            @Suppress("UNCHECKED_CAST")
            val raw = parameters["pattern"] as? List<Any>
                ?: throw BridgeError.InvalidParameters("pattern is required")

            val pattern = raw.mapNotNull { (it as? Number)?.toLong()?.coerceIn(1, 5000) }
            if (pattern.isEmpty()) {
                throw BridgeError.InvalidParameters("pattern must contain at least one duration")
            }

            // Android createWaveform expects [delay, vibrate, pause, vibrate, ...].
            // Input is [vibrate, pause, vibrate, ...] - prepend a 0ms delay to align.
            val timings = longArrayOf(0) + pattern.toLongArray()
            val amplitudes = IntArray(timings.size) { index ->
                when {
                    index == 0 -> 0 // initial delay
                    index % 2 == 1 -> VibrationEffect.DEFAULT_AMPLITUDE // vibrate segments
                    else -> 0 // pause segments
                }
            }

            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            return mapOf("success" to true)
        }
    }
}
