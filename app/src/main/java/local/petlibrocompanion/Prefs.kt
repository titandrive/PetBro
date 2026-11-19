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
    private val _alertMinutes = MutableStateFlow(30)
    /** How often feeder alerts check, in minutes. */
    val alertMinutes: StateFlow<Int> = _alertMinutes

    fun alertMinutes(context: Context) = prefs(context).getInt("alertMinutes", 30)

    fun setAlertMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt("alertMinutes", minutes).apply()
        _alertMinutes.value = minutes
        FeederAlerts.schedule(context)
    }

    private val _alertKinds = MutableStateFlow<Set<FeederAlerts.Problem>>(emptySet())
    /** Which kinds of feeder alert are on. */
    val alertKinds: StateFlow<Set<FeederAlerts.Problem>> = _alertKinds

    fun alertsOn(context: Context) = prefs(context).getBoolean("alerts", true)

    fun setAlerts(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("alerts", on).apply()
        _alerts.value = on
        FeederAlerts.schedule(context)
    }

    /** Every kind is on, except desiccant reminders (which used to be their own switch). */
    fun alertKind(context: Context, kind: FeederAlerts.Problem) =
        prefs(context).getBoolean("alert_${kind.name}", if (kind == FeederAlerts.Problem.DESICCANT) prefs(context).getBoolean("desiccantAlerts", false) else true)

    fun setAlertKind(context: Context, kind: FeederAlerts.Problem, on: Boolean) {
        prefs(context).edit().putBoolean("alert_${kind.name}", on).apply()
        _alertKinds.value = FeederAlerts.Problem.entries.filter { alertKind(context, it) }.toSet()
        _alertMinutes.value = alertMinutes(context)
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
        _alertKinds.value = FeederAlerts.Problem.entries.filter { alertKind(context, it) }.toSet()
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
