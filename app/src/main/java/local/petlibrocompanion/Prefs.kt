package local.petlibrocompanion

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/** App-wide choices that aren't the theme or the schedule. */
object Prefs {
    private val _haptics = MutableStateFlow(true)
    val hapticsOn: StateFlow<Boolean> = _haptics
    private val _quick = MutableStateFlow(1)
    /** What a tap on Feed Now, the tile, the shortcut and automation feed. */
    val quickPortionsFlow: StateFlow<Int> = _quick
    private val _alerts = MutableStateFlow(true)
    /** Feeder alerts: a notification when the feeder goes offline, runs low, jams or errors. */
    val alerts: StateFlow<Boolean> = _alerts
    private val _desiccant = MutableStateFlow(false)
    /** Whether feeder alerts include the desiccant falling due. */
    val desiccant: StateFlow<Boolean> = _desiccant

    fun alertsOn(context: Context) = prefs(context).getBoolean("alerts", true)

    fun setAlerts(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("alerts", on).apply()
        _alerts.value = on
        FeederAlerts.schedule(context)
    }

    fun desiccantAlerts(context: Context) = prefs(context).getBoolean("desiccantAlerts", false)

    fun setDesiccantAlerts(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("desiccantAlerts", on).apply()
        _desiccant.value = on
    }

    private val _automation = MutableStateFlow(false)
    /** Whether other apps may feed (by broadcast, or a shortcut made for them). */
    val automation: StateFlow<Boolean> = _automation

    private fun prefs(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        _haptics.value = p.getBoolean("haptics", true)
        _quick.value = quickPortions(context)
        _automation.value = automationOn(context)
        _alerts.value = alertsOn(context)
        _desiccant.value = desiccantAlerts(context)
    }

    fun haptics(context: Context) = prefs(context).getBoolean("haptics", true)

    fun setHaptics(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("haptics", on).apply()
        _haptics.value = on
    }

    fun quickPortions(context: Context) = prefs(context).getInt("quickPortions", 1).coerceIn(1, Petlibro.MAX_PORTIONS)

    fun setQuickPortions(context: Context, n: Int) {
        prefs(context).edit().putInt("quickPortions", n).apply()
        _quick.value = n
        FeedNow.publishShortcut(context)
    }

    fun automationOn(context: Context) = prefs(context).getBoolean("automation", false)

    fun setAutomation(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("automation", on).apply()
        _automation.value = on
    }

    /**
     * A secret only PetBro's own shortcuts and tile carry, so another app can't start a feed by
     * launching the shortcut's activity directly.
     */
    fun feedKey(context: Context): String {
        val p = prefs(context)
        p.getString("feedKey", null)?.let { return it }
        val key = UUID.randomUUID().toString()
        p.edit().putString("feedKey", key).commit()
        return key
    }
}
