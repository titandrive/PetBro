package local.petlibrocompanion

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

enum class Repeat { DAILY, WEEKLY, MONTHLY }

/** One meal on the schedule: when it repeats, at what time, and how much. */
data class Meal(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Breakfast",
    val enabled: Boolean = true,
    val repeat: Repeat = Repeat.DAILY,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    /** 1–31, or [LAST_DAY]. Days past a short month's end feed on its last day instead. */
    val dayOfMonth: Int = 1,
    val hour: Int = 8,
    val minute: Int = 0,
    val portions: Int = 1,
) {
    /** The next feeding strictly after [after], or null if nothing can match (weekly with no days picked). */
    fun nextRun(after: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val time = LocalTime.of(hour, minute)
        for (offset in 0L..62L) {
            val date = after.toLocalDate().plusDays(offset)
            val candidate = date.atTime(time)
            if (!candidate.isAfter(after)) continue
            val matches = when (repeat) {
                Repeat.DAILY -> true
                Repeat.WEEKLY -> date.dayOfWeek in daysOfWeek
                Repeat.MONTHLY -> date.dayOfMonth ==
                    if (dayOfMonth == LAST_DAY) date.lengthOfMonth() else dayOfMonth.coerceAtMost(date.lengthOfMonth())
            }
            if (matches) return candidate
        }
        return null
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("enabled", enabled)
        .put("repeat", repeat.name)
        .put("daysOfWeek", daysOfWeek.joinToString(",") { it.name })
        .put("dayOfMonth", dayOfMonth)
        .put("hour", hour)
        .put("minute", minute)
        .put("portions", portions)

    companion object {
        const val LAST_DAY = 0

        fun fromJson(o: JSONObject): Meal {
            val d = Meal()
            return Meal(
                id = o.optString("id").ifEmpty { d.id },
                name = o.optString("name", d.name),
                enabled = o.optBoolean("enabled", d.enabled),
                repeat = Repeat.entries.firstOrNull { it.name == o.optString("repeat") } ?: Repeat.DAILY,
                daysOfWeek = o.optString("daysOfWeek").split(',')
                    .mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }
                    .toSet(),
                dayOfMonth = o.optInt("dayOfMonth", d.dayOfMonth).coerceIn(LAST_DAY, 31),
                hour = o.optInt("hour", d.hour).coerceIn(0, 23),
                minute = o.optInt("minute", d.minute).coerceIn(0, 59),
                portions = o.optInt("portions", d.portions).coerceIn(1, Petlibro.MAX_PORTIONS),
            )
        }

        /** A name for a new meal by its time of day. */
        fun nameFor(hour: Int) = when (hour) {
            in 4..10 -> "Breakfast"
            in 11..14 -> "Lunch"
            in 15..16 -> "Snack"
            in 17..21 -> "Dinner"
            else -> "Late snack"
        }
    }
}

/** The saved meals, and whether the schedule as a whole is on. */
object Meals {
    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private fun prefs(context: Context) = context.getSharedPreferences("meals", Context.MODE_PRIVATE)

    fun init(context: Context) {
        migrate(context)
        _meals.value = load(context)
        _enabled.value = prefs(context).getBoolean("enabled", false)
    }

    fun load(context: Context): List<Meal> = runCatching {
        val array = JSONArray(prefs(context).getString("list", "[]"))
        (0 until array.length()).map { Meal.fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun scheduleOn(context: Context) = prefs(context).getBoolean("enabled", false)

    fun set(context: Context, meals: List<Meal>) {
        prefs(context).edit().putString("list", JSONArray(meals.map { it.toJson() }).toString()).commit()
        _meals.value = meals
        MealScheduler.reschedule(context)
    }

    fun setEnabled(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("enabled", on).commit()
        _enabled.value = on
        MealScheduler.reschedule(context)
    }

    /** The first PetBro had one daily meal in "daily-feed": it becomes the first meal here. */
    private fun migrate(context: Context) {
        val old = context.getSharedPreferences("daily-feed", Context.MODE_PRIVATE)
        if (prefs(context).contains("list") || !old.contains("hour")) return
        val hour = old.getInt("hour", 8)
        val meal = Meal(name = Meal.nameFor(hour), hour = hour, minute = old.getInt("minute", 0), portions = old.getInt("portions", 1))
        prefs(context).edit()
            .putString("list", JSONArray(listOf(meal.toJson())).toString())
            .putBoolean("enabled", old.getBoolean("enabled", false))
            .commit()
        old.edit().clear().apply()
    }

    /** The next meal(s) due across the schedule, at the same minute, or null when nothing is due. */
    fun nextDue(meals: List<Meal>, after: LocalDateTime = LocalDateTime.now()): Pair<LocalDateTime, List<Meal>>? {
        val upcoming = meals.filter { it.enabled }.mapNotNull { m -> m.nextRun(after)?.let { it to m } }
        val first = upcoming.minOfOrNull { it.first } ?: return null
        return first to upcoming.filter { it.first == first }.map { it.second }
    }
}

/**
 * The schedule is one exact alarm, set for the next meal; when it goes off it feeds that meal
 * (or the meals sharing its minute) and sets the one after.
 */
object MealScheduler {
    private const val RUN = "local.petlibrocompanion.MEAL"
    /** A meal more than this late (the phone was off, say) is skipped rather than served late. */
    private const val LATE_MS = 5 * 60_000L

    private fun state(context: Context) = context.getSharedPreferences("meal-alarm", Context.MODE_PRIVATE)

    fun canSchedule(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Meals can be held back in Doze while battery optimization is on. */
    fun batteryExempt(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun reschedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, MealReceiver::class.java).setAction(RUN)
        val next = if (Meals.scheduleOn(context)) Meals.nextDue(Meals.load(context)) else null
        if (next == null) {
            PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { alarms.cancel(it) }
            state(context).edit().remove("next").remove("due").apply()
            return
        }
        val (time, meals) = next
        val whenMs = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        state(context).edit().putLong("next", whenMs).putString("due", meals.joinToString(",") { it.id }).apply()
        intent.putExtra("when", whenMs)
        val pending = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (canSchedule(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        }
    }

    /** Handles the alarm: feeds what's due, after setting the next one. */
    fun fire(context: Context, intent: Intent, pending: BroadcastReceiver.PendingResult) {
        val state = state(context)
        val whenMs = intent.getLongExtra("when", 0)
        val dueIds = state.getString("due", "").orEmpty().split(',').filter { it.isNotEmpty() }
        if (whenMs == 0L || whenMs != state.getLong("next", 0) || !Meals.scheduleOn(context)) {
            reschedule(context)
            pending.finish()
            return
        }
        // Once only, even if the alarm is delivered twice.
        if (state.getLong("fired", 0) == whenMs) {
            pending.finish()
            return
        }
        state.edit().putLong("fired", whenMs).commit()
        val meals = Meals.load(context).filter { it.id in dueIds }
        val late = System.currentTimeMillis() - whenMs > LATE_MS
        // Set the next one first, so a feed that gets killed partway doesn't end the schedule.
        reschedule(context)

        Thread({
            try {
                meals.forEach { meal ->
                    val entry = if (late) {
                        FeedLog.add(
                            context,
                            FeedEntry(System.currentTimeMillis(), meal.portions, FeedSource.SCHEDULED, false,
                                "Skipped: the alarm came more than 5 minutes late.", meal.name),
                        )
                    } else {
                        // The same request id for the same meal and time, so Petlibro can drop a repeat.
                        val requestId = UUID.nameUUIDFromBytes("${meal.id}-$whenMs".toByteArray()).toString()
                        Feeder.dispense(context, meal.portions, FeedSource.SCHEDULED, requestId, meal.name)
                    }
                    FeedNotifications.meal(context, entry)
                }
            } finally {
                pending.finish()
            }
        }, "petbro-meal").start()
    }
}

/** The meal alarm, and the system events after which it has to be set again. */
class MealReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "local.petlibrocompanion.MEAL") {
            MealScheduler.fire(context, intent, goAsync())
        } else {
            MealScheduler.reschedule(context)
        }
    }
}
