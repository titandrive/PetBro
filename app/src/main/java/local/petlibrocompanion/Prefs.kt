package local.petlibrocompanion

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App-wide choices that aren't the theme or the schedule. */
object Prefs {
    private val _haptics = MutableStateFlow(true)
    val hapticsOn: StateFlow<Boolean> = _haptics
    private val _portions = MutableStateFlow(1)
    /** Feed Now's last amount, offered again next time. */
    val portions: StateFlow<Int> = _portions

    private fun prefs(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    fun load(context: Context) {
        _haptics.value = prefs(context).getBoolean("haptics", true)
        _portions.value = prefs(context).getInt("portions", 1).coerceIn(1, Petlibro.MAX_PORTIONS)
    }

    fun haptics(context: Context) = prefs(context).getBoolean("haptics", true)

    fun setHaptics(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("haptics", on).apply()
        _haptics.value = on
    }

    fun setPortions(context: Context, n: Int) {
        prefs(context).edit().putInt("portions", n).apply()
        _portions.value = n
    }
}
