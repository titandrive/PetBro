package local.petlibrocompanion

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Feeder alerts: about every half hour, a quiet check of the feeder's status, with a
 * notification when something new goes wrong: offline, low on food, the lid blocked, an error
 * (a stuck rotor, say), or, if asked for, the desiccant due. Each problem notifies once, when it
 * starts; once it clears, it can notify again next time.
 */
object FeederAlerts {
    private const val CHANNEL = "alerts"
    private const val CHECK = "local.petlibrocompanion.ALERT_CHECK"

    enum class Problem(val title: String, val setting: String) {
        OFFLINE("Feeder is offline", "Offline"),
        FOOD_LOW("Feeder is low on food", "Low on food"),
        LID_BLOCKED("Feeder lid is blocked", "Lid blocked"),
        ERROR("Feeder needs attention", "Errors"),
        DESICCANT("Time to change the desiccant", "Desiccant due"),
    }

    private fun prefs(context: Context) = context.getSharedPreferences("alerts", Context.MODE_PRIVATE)

    /** Sets the repeating check (as often as set in Settings), or cancels it with alerts off. Inexact, so Android can batch it. */
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 3, Intent(context, AlertReceiver::class.java).setAction(CHECK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(pending)
        if (!Prefs.alertsOn(context) || !Session.hasToken(context)) return
        val every = Prefs.alertMinutes(context) * 60_000L
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + every, every, pending)
    }

    /** Blocking: checks the feeder and notifies about new problems. */
    fun check(context: Context) {
        if (!Prefs.alertsOn(context)) return
        val token = Session.token(context) ?: return
        val serial = Session.feeder(context) ?: return
        val p = runCatching { Petlibro.problems(token, serial) }.getOrNull() ?: return
        val now = buildSet {
            if (p.offline) add(Problem.OFFLINE)
            if (p.foodLow) add(Problem.FOOD_LOW)
            if (p.lidBlocked) add(Problem.LID_BLOCKED)
            if (p.error != null) add(Problem.ERROR)
            if (p.desiccantDue) add(Problem.DESICCANT)
        }.filter { Prefs.alertKind(context, it) }.toSet()
        val before = prefs(context).getStringSet("active", emptySet()).orEmpty()
        prefs(context).edit().putStringSet("active", now.map { it.name }.toSet()).apply()
        (now.filter { it.name !in before }).forEach { problem ->
            notify(context, problem, if (problem == Problem.ERROR) p.error else null)
        }
    }

    private fun notify(context: Context, problem: Problem, detail: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Feeder alerts", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val name = Session.devices(context)?.firstOrNull { it.serial == Session.feeder(context) }
            ?.let { it.copy(nickname = Session.nickname(context, it.serial)).label }
        val text = detail ?: when (problem) {
            Problem.OFFLINE -> "Petlibro can't reach it. Check its power and Wi-Fi."
            Problem.FOOD_LOW -> "Top it up soon."
            Problem.LID_BLOCKED -> "Something may be stuck in the lid."
            Problem.ERROR -> "Open the Petlibro app for details."
            Problem.DESICCANT -> "Put in a fresh pack, then reset the countdown in Configure."
        }
        val notification = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(name?.let { problem.title.replaceFirst("Feeder", it) } ?: problem.title)
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(100 + problem.ordinal, notification) }
    }
}

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread({
            try {
                FeederAlerts.check(context.applicationContext)
            } finally {
                pending.finish()
            }
        }, "petbro-alerts").start()
    }
}
