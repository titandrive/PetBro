package local.petlibrocompanion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Where the account stands, for the screens. */
sealed interface AccountState {
    data object SignedOut : AccountState
    data class Checking(val message: String) : AccountState
    data class SignedIn(val email: String?, val devices: List<Device>, val feeder: Device?) : AccountState
    /** Saved sign-in that couldn't be checked (no network, say): feeding may still work. */
    data class Offline(val email: String?, val message: String) : AccountState
}

/** The account and feeder, shared by every screen. */
object Feeder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _account = MutableStateFlow<AccountState>(AccountState.SignedOut)
    val account: StateFlow<AccountState> = _account

    /** A sign-in problem to show under the form. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _feeding = MutableStateFlow(false)
    val feeding: StateFlow<Boolean> = _feeding

    /** A background check of the devices is going (the page shows what was saved meanwhile). */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _pets = MutableStateFlow<List<String>>(emptyList())
    /** The names of the pets bound to the feeder, as last read. */
    val pets: StateFlow<List<String>> = _pets

    private val _today = MutableStateFlow<TodayStats?>(null)
    /** Today's numbers from the feeder (times the pet ate, meals out), as last read. */
    val today: StateFlow<TodayStats?> = _today

    /** Why the last background check failed, while the saved devices are still shown. */
    private val _stale = MutableStateFlow<String?>(null)
    val stale: StateFlow<String?> = _stale

    private var started = false

    /** Restores a saved sign-in once per process. */
    fun init(context: Context) {
        if (started) return
        started = true
        FeedLog.init(context)
        _today.value = Session.today(context)
        _pets.value = Session.pets(context)
        if (!Session.hasToken(context)) return
        // Show the devices saved last time straight away, then check them in the background.
        Session.devices(context)?.let { setDevices(context, it, save = false) }
        refresh(context.applicationContext)
    }

    /** True when Feed Now can go: signed in with a feeder picked. */
    fun ready(context: Context) = Session.hasToken(context) && Session.feeder(context) != null

    fun signIn(context: Context, email: String, password: String) {
        val app = context.applicationContext
        _error.value = null
        _account.value = AccountState.Checking("Signing in…")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val token = Petlibro.login(email, password)
                    val devices = Petlibro.devices(token)
                    if (!Session.saveToken(app, token)) throw IOException("The sign-in couldn't be saved.")
                    Session.setEmail(app, email)
                    devices
                }
            }
            result.onSuccess {
                setDevices(app, it)
                FeederAlerts.schedule(app)
                refreshToday(app)
            }.onFailure {
                _account.value = AccountState.SignedOut
                _error.value = message(it)
            }
        }
    }

    /**
     * Re-reads the account's devices with the saved session. With devices already on screen it
     * checks quietly, leaving them up; otherwise the page says it's checking.
     */
    fun refresh(context: Context) {
        val app = context.applicationContext
        if (_refreshing.value) return
        val quiet = _account.value is AccountState.SignedIn
        if (!quiet) _account.value = AccountState.Checking("Checking Petlibro…")
        _refreshing.value = true
        scope.launch {
            try { check(app, quiet) } finally { _refreshing.value = false }
        }
    }

    private suspend fun check(app: Context, quiet: Boolean) {
        run {
            val token = Session.token(app)
            if (token == null) {
                Session.clear(app)
                _account.value = AccountState.SignedOut
                _error.value = "The saved sign-in couldn't be restored. Sign in again."
                return@run
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val devices = Petlibro.devices(token)
                    // Sign-ins from the first PetBro didn't save the email: ask Petlibro for it.
                    if (Session.email(app) == null) {
                        runCatching { Petlibro.accountName(token) }.getOrNull()?.let { Session.setEmail(app, it) }
                    }
                    devices
                }
            }
            result.onSuccess {
                setDevices(app, it)
                refreshToday(app)
            }.onFailure {
                if (it is SessionExpired) {
                    signOut(app)
                    _error.value = message(it)
                } else if (quiet) {
                    _stale.value = message(it)
                } else {
                    _account.value = AccountState.Offline(Session.email(app), message(it))
                }
            }
        }
    }

    /** Re-reads today's feeder numbers in the background; failures keep the last ones. */
    fun refreshToday(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val t = withContext(Dispatchers.IO) { readToday(app) } ?: return@launch
            _today.value = t
            FeedWidget.updateAll(app)
        }
    }

    /** Blocking: reads and saves today's feeder numbers, or null if they couldn't be read. */
    fun readToday(context: Context): TodayStats? {
        val token = Session.token(context) ?: return null
        val serial = Session.feeder(context) ?: return null
        // The pets' names rarely change, but they're one small request: read them alongside.
        runCatching { Petlibro.pets(token, serial) }.getOrNull()?.let {
            Session.savePets(context, it)
            _pets.value = it
        }
        runCatching { Petlibro.records(token, serial) }.getOrNull()?.let { FeedLog.setFeeder(context, it) }
        return runCatching { Petlibro.today(token, serial) }.getOrNull()?.also {
            Session.saveToday(context, it)
            _today.value = it
        }
    }

    private fun setDevices(context: Context, devices: List<Device>, save: Boolean = true) {
        if (save) {
            Session.saveDevices(context, devices)
            _stale.value = null
        }
        @Suppress("NAME_SHADOWING")
        val devices = devices.map { it.copy(nickname = Session.nickname(context, it.serial)) }
        val feeders = devices.filter { it.canFeed }
        // Keep the feeder picked before, if it's still on the account; else the only one there is.
        val saved = Session.feeder(context)
        val feeder = feeders.firstOrNull { it.serial == saved } ?: feeders.singleOrNull()
        Session.setFeeder(context, feeder?.serial)
        _account.value = AccountState.SignedIn(Session.email(context), devices, feeder)
    }

    /** Names the feeder in PetBro only; blank goes back to Petlibro's name. */
    fun rename(context: Context, device: Device, name: String?) {
        Session.setNickname(context, device.serial, name)
        val nick = Session.nickname(context, device.serial)
        (_account.value as? AccountState.SignedIn)?.let { a ->
            _account.value = a.copy(
                devices = a.devices.map { if (it.serial == device.serial) it.copy(nickname = nick) else it },
                feeder = a.feeder?.let { if (it.serial == device.serial) it.copy(nickname = nick) else it },
            )
        }
        FeedWidget.updateAll(context)
    }

    fun pickFeeder(context: Context, device: Device) {
        Session.setFeeder(context, device.serial)
        (_account.value as? AccountState.SignedIn)?.let { _account.value = it.copy(feeder = device) }
    }

    fun signOut(context: Context) {
        Session.clear(context)
        _account.value = AccountState.SignedOut
        _error.value = null
        MealScheduler.reschedule(context)
        FeederAlerts.schedule(context)
    }

    /** Feed Now: dispenses [portions] and reports back in [onDone]. */
    fun feedNow(context: Context, portions: Int, source: FeedSource = FeedSource.MANUAL, onDone: (FeedEntry) -> Unit) {
        if (_feeding.value) return
        val app = context.applicationContext
        _feeding.value = true
        scope.launch {
            val entry = withContext(Dispatchers.IO) { dispense(app, portions, source) }
            _feeding.value = false
            if (entry.message.contains("Sign in again")) signOut(app)
            onDone(entry)
        }
    }

    /**
     * Sends one feed with the saved session and logs it. Blocking: call it off the main thread.
     * Used by Feed Now, scheduled meals and the test.
     */
    fun dispense(context: Context, portions: Int, source: FeedSource, requestId: String? = null, meal: String? = null): FeedEntry {
        val token = Session.token(context)
        val serial = Session.feeder(context)
        val (ok, message) = when {
            token == null -> false to "Not signed in. Sign in again."
            serial == null -> false to "No feeder picked."
            else -> try {
                if (requestId != null) Petlibro.feed(token, serial, portions, requestId) else Petlibro.feed(token, serial, portions)
                true to "Petlibro accepted the request."
            } catch (e: SessionExpired) {
                false to "Petlibro ended the session. Sign in again."
            } catch (e: Exception) {
                false to "Couldn't confirm the feed. Check the feeder's log."
            }
        }
        return FeedLog.add(context, FeedEntry(System.currentTimeMillis(), portions, source, ok, message, meal))
    }

    private fun message(e: Throwable): String = when (e) {
        is IOException -> e.message ?: "Couldn't reach Petlibro."
        else -> "Petlibro returned an unexpected response."
    }
}

enum class FeedSource(val label: String) {
    MANUAL("Feed now"), SCHEDULED("Scheduled"), TEST("Test"), QUICK("Tile or shortcut"), AUTOMATION("Automation"),
    /** Only in the feeder's own record: the Petlibro app, Petlibro's schedule or the feeder's button. */
    FEEDER("Feeder")
}

data class FeedEntry(
    val time: Long,
    val portions: Int,
    val source: FeedSource,
    val ok: Boolean,
    val message: String,
    /** The meal's name, for scheduled feeds. */
    val meal: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time).put("portions", portions).put("source", source.name)
        .put("ok", ok).put("message", message).put("meal", meal ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject) = FeedEntry(
            time = o.getLong("time"),
            portions = o.optInt("portions", 1),
            source = FeedSource.entries.firstOrNull { it.name == o.optString("source") } ?: FeedSource.MANUAL,
            ok = o.optBoolean("ok"),
            message = o.optString("message"),
            meal = o.optString("meal").takeIf { it.isNotEmpty() && it != "null" },
        )
    }
}

/** The last [LIMIT] feeds, newest first. */
object FeedLog {
    private const val LIMIT = 200
    private val _entries = MutableStateFlow<List<FeedEntry>>(emptyList())
    val entries: StateFlow<List<FeedEntry>> = _entries

    private fun prefs(context: Context) = context.getSharedPreferences("feed-log", Context.MODE_PRIVATE)

    private val _feeder = MutableStateFlow<List<FeederRecord>>(emptyList())
    /** The feeder's own record of the last week's meals, as last read. */
    val feeder: StateFlow<List<FeederRecord>> = _feeder

    fun init(context: Context) {
        _entries.value = load(context)
        _feeder.value = loadFeeder(context)
    }

    private fun loadFeeder(context: Context): List<FeederRecord> = runCatching {
        val a = JSONArray(prefs(context).getString("feeder", "[]"))
        (0 until a.length()).map { FeederRecord.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun setFeeder(context: Context, records: List<FeederRecord>) {
        prefs(context).edit().putString("feeder", JSONArray(records.map { it.toJson() }).toString()).apply()
        _feeder.value = records
        FeedWidget.updateAll(context)
    }

    /**
     * PetBro's feeds with the feeder's record merged in, newest first. A feeder record within
     * three minutes of a PetBro feed of the same size is that feed; the rest went out some other way.
     */
    fun merge(own: List<FeedEntry>, feeder: List<FeederRecord>): List<FeedEntry> {
        val window = 3 * 60_000L
        val others = feeder.filter { r ->
            own.none { it.ok && it.portions == r.portions && kotlin.math.abs(it.time - r.time) <= window }
        }.map { FeedEntry(it.time, it.portions, FeedSource.FEEDER, ok = true, message = "", meal = it.label) }
        return (own + others).sortedByDescending { it.time }
    }

    fun merged(context: Context): List<FeedEntry> = merge(load(context), loadFeeder(context))

    private fun load(context: Context): List<FeedEntry> = runCatching {
        val array = JSONArray(prefs(context).getString("entries", "[]"))
        (0 until array.length()).map { FeedEntry.fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(context: Context, entry: FeedEntry): FeedEntry {
        // Read back first: a receiver may have logged one in another process start.
        val all = (listOf(entry) + load(context)).take(LIMIT)
        prefs(context).edit().putString("entries", JSONArray(all.map { it.toJson() }).toString()).commit()
        _entries.value = all
        FeedWidget.updateAll(context)
        return entry
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        _entries.value = emptyList()
        FeedWidget.updateAll(context)
    }

    /** The last feed that went out, read fresh (the widget runs without the app). */
    fun lastFed(context: Context): FeedEntry? = merged(context).firstOrNull { it.ok }

    /** Today's feeds that went out, read fresh. */
    fun todayFed(context: Context): List<FeedEntry> {
        val start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return merged(context).filter { it.ok && it.time >= start }
    }
}

/** A notification when a scheduled meal goes out, or doesn't. */
object FeedNotifications {
    private const val CHANNEL = "meals"
    private const val ID = 1

    fun meal(context: Context, entry: FeedEntry) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Scheduled meals", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // Served: just what went out. Failed: an error saying why.
        val meal = entry.meal ?: "Meal"
        val title = if (entry.ok) "$meal served" else "$meal didn't go out"
        val text = if (entry.ok) portionsLabel(entry.portions) else entry.message
        val notification = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(ID, notification) }
    }
}

fun portionsLabel(n: Int) = if (n == 1) "1 portion" else "$n portions"
