package local.petlibrocompanion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Deliberate vibrations for feeding. These go through the
 * vibrator directly at full strength, because the touch-feedback ones are easy to miss.
 */
object Haptics {

    /** A light click for flipping a toggle or picking an option. */
    fun tick(context: Context) {
        if (!Prefs.haptics(context)) return
        val vibrator = vibrator(context) ?: return
        runCatching {
            vibrator.vibrate(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                else VibrationEffect.createOneShot(10, 60)
            )
        }
    }

    /** One firm pulse: a feed was sent. */
    fun press(context: Context) = vibrate(context, longArrayOf(0, 70), intArrayOf(0, 255))

    /** Two strong pulses: a feed went through. */
    fun finished(context: Context) = vibrate(context, longArrayOf(0, 120, 110, 220), intArrayOf(0, 255, 0, 255))

    /** Set while PetBro is on screen. */
    @Volatile
    var appVisible = false

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        if (!Prefs.haptics(context)) return
        val vibrator = vibrator(context) ?: return
        // Phones without amplitude control just use the timings at their fixed strength.
        val effect = if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
        else VibrationEffect.createWaveform(timings, -1)
        runCatching {
            if (appVisible) {
                vibrator.vibrate(effect)
            } else {
                // From the widget, tile, shortcut or automation PetBro runs in the background,
                // where Android drops ordinary vibrations. Notification vibrations still play.
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else context.getSystemService(Vibrator::class.java)
}
