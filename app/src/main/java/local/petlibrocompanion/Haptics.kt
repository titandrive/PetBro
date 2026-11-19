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

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        if (!Prefs.haptics(context)) return
        val vibrator = vibrator(context) ?: return
        runCatching {
            vibrator.vibrate(
                // Phones without amplitude control just use the timings at their fixed strength.
                if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
                else VibrationEffect.createWaveform(timings, -1)
            )
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else context.getSystemService(Vibrator::class.java)
}
