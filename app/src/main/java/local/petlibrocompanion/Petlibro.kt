package local.petlibrocompanion

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

/** Petlibro ended the session; signing in again starts a new one. */
class SessionExpired : IOException("Petlibro ended the session. Sign in again.")

/** A device on the Petlibro account. */
data class Device(
    val serial: String,
    /** The name in the Petlibro app. */
    val name: String,
    val model: String,
    /** A name set in PetBro, shown instead of [name]; Petlibro never sees it. */
    val nickname: String? = null,
    /** Petlibro's name for the model, like "One RFID Smart Feeder". */
    val product: String? = null,
) {
    /** The model as a name, falling back to its number. */
    val modelName: String get() = product ?: if (model == FEEDER_MODEL) "One RFID Smart Feeder" else model

    /** What PetBro calls it. */
    val label: String get() = nickname ?: name

    /** Feeders whose manual feeding PetBro has been tested with. */
    val canFeed: Boolean get() = model == FEEDER_MODEL

    companion object {
        const val FEEDER_MODEL = "PLAF301"
    }
}

/** What Petlibro reports about a feeder right now; null where it didn't say. */
data class FeederDetails(
    val online: Boolean?,
    val foodLow: Boolean?,
    val lidOpen: Boolean?,
    val lidBlocked: Boolean?,
    val dispensing: Boolean?,
    val batteryState: String?,
    val batteryPercent: Int?,
    val wifiName: String?,
    val wifiRssi: Int?,
    val desiccantDaysLeft: Int?,
    val desiccantCycleDays: Int?,
    val feedingPlanOn: Boolean?,
    val firmware: String?,
    val hardware: String?,
    val mac: String?,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        // Nulls are kept as JSON nulls, so they read back as "Petlibro didn't say".
        fun set(k: String, v: Any?) { o.put(k, v ?: JSONObject.NULL) }
        set("online", online); set("foodLow", foodLow); set("lidOpen", lidOpen); set("lidBlocked", lidBlocked)
        set("dispensing", dispensing); set("batteryState", batteryState); set("batteryPercent", batteryPercent)
        set("wifiName", wifiName); set("wifiRssi", wifiRssi); set("desiccantDaysLeft", desiccantDaysLeft)
        set("desiccantCycleDays", desiccantCycleDays); set("feedingPlanOn", feedingPlanOn)
        set("firmware", firmware); set("hardware", hardware); set("mac", mac)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): FeederDetails {
            fun b(k: String) = if (o.isNull(k)) null else o.optBoolean(k)
            fun i(k: String) = if (o.isNull(k)) null else o.optInt(k)
            fun t(k: String) = if (o.isNull(k)) null else o.optString(k)
            return FeederDetails(
                b("online"), b("foodLow"), b("lidOpen"), b("lidBlocked"), b("dispensing"), t("batteryState"), i("batteryPercent"),
                t("wifiName"), i("wifiRssi"), i("desiccantDaysLeft"), i("desiccantCycleDays"), b("feedingPlanOn"),
                t("firmware"), t("hardware"), t("mac"),
            )
        }
    }
}

/** The feeder's own settings, as Petlibro reports them; null where it didn't say. */
data class FeederConfig(
    /** The feeder's own feeding plan (set in the Petlibro app). */
    val planOn: Boolean? = null,
    val childLock: Boolean? = null,
    val lightOn: Boolean? = null,
    val soundOn: Boolean? = null,
    /** Petlibro's master switches for light and sound, beside the on/off ones above. */
    val lightAllowed: Boolean? = null,
    val soundAllowed: Boolean? = null,
    /** The Petlibro app's own notifications for this feeder (read only: there's no known call to change them). */
    val noticeLowBattery: Boolean? = null,
    val noticePowerChange: Boolean? = null,
    val noticeOutletBlocked: Boolean? = null,
    val noticeRefill: Boolean? = null,
    val firmware: String? = null,
    val volume: Int? = null,
    val displayOn: Boolean? = null,
    /** "CUSTOM" opens when the pet comes; "KEEP_OPEN" stays open; "KEEP_CLOSE" stays closed. */
    val lidMode: String? = null,
    /** SLOW, MEDIUM or FAST. */
    val lidSpeed: String? = null,
    val lidCloseSec: Int? = null,
    val desiccantEveryDays: Int? = null,
    val displayText: String? = null,
    /** 5 heart, 6 dog, 7 cat, 8 elk. */
    val displayIcon: Int? = null,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        fun set(k: String, v: Any?) { o.put(k, v ?: JSONObject.NULL) }
        set("planOn", planOn); set("childLock", childLock); set("lightOn", lightOn); set("soundOn", soundOn)
        set("volume", volume); set("displayOn", displayOn); set("lidMode", lidMode); set("lidSpeed", lidSpeed)
        set("lidCloseSec", lidCloseSec); set("desiccantEveryDays", desiccantEveryDays)
        set("displayText", displayText); set("displayIcon", displayIcon)
        set("lightAllowed", lightAllowed); set("soundAllowed", soundAllowed)
        set("noticeLowBattery", noticeLowBattery); set("noticePowerChange", noticePowerChange)
        set("noticeOutletBlocked", noticeOutletBlocked); set("noticeRefill", noticeRefill); set("firmware", firmware)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): FeederConfig {
            fun b(k: String) = if (!o.has(k) || o.isNull(k)) null else o.optBoolean(k)
            fun i(k: String) = if (!o.has(k) || o.isNull(k)) null else o.optInt(k)
            fun t(k: String) = if (!o.has(k) || o.isNull(k)) null else o.optString(k)
            return FeederConfig(
                planOn = b("planOn"), childLock = b("childLock"), lightOn = b("lightOn"), soundOn = b("soundOn"),
                lightAllowed = b("lightAllowed"), soundAllowed = b("soundAllowed"),
                noticeLowBattery = b("noticeLowBattery"), noticePowerChange = b("noticePowerChange"),
                noticeOutletBlocked = b("noticeOutletBlocked"), noticeRefill = b("noticeRefill"), firmware = t("firmware"),
                volume = i("volume"), displayOn = b("displayOn"), lidMode = t("lidMode"), lidSpeed = t("lidSpeed"),
                lidCloseSec = i("lidCloseSec"), desiccantEveryDays = i("desiccantEveryDays"),
                displayText = t("displayText"), displayIcon = i("displayIcon"),
            )
        }
    }
}

/** Today's feeder numbers, for [day] (ISO date); -1 where Petlibro didn't say. */
data class TodayStats(val day: String, val eatingTimes: Int, val feedingTimes: Int, val feedingPortions: Int, val eatingSeconds: Int = -1) {
    val isToday: Boolean get() = day == java.time.LocalDate.now().toString()

    /** "7 times · 14 min", or just the count when Petlibro gave no time. */
    fun ateLabel(): String {
        val times = if (eatingTimes == 1) "once" else "$eatingTimes times"
        val time = when {
            eatingSeconds < 0 -> null
            eatingSeconds < 60 -> "$eatingSeconds sec"
            eatingSeconds < 3600 -> "${(eatingSeconds + 30) / 60} min"
            else -> "${eatingSeconds / 3600} h ${(eatingSeconds % 3600) / 60} min"
        }
        return if (time != null) "$times · $time" else times
    }
}

/** A meal the feeder dispensed. [kind] is Petlibro's event type, like MANUAL_FEEDING_SUCCESS. */
data class FeederRecord(val time: Long, val portions: Int, val kind: String) {
    fun toJson(): JSONObject = JSONObject().put("time", time).put("portions", portions).put("kind", kind)

    /** Where Petlibro says it came from. */
    val label: String get() = when {
        kind.contains("PLAN") -> "Petlibro schedule"
        kind.contains("MANUAL") -> "Petlibro app"
        else -> "Feeder"
    }

    companion object {
        fun fromJson(o: JSONObject) = FeederRecord(o.getLong("time"), o.optInt("portions"), o.optString("kind"))
    }
}

data class FirmwareUpdate(val jobItemId: String, val version: String?, val notes: String?, val progress: Int?)

data class FeederProblems(
    val offline: Boolean,
    val foodLow: Boolean,
    val lidBlocked: Boolean,
    val error: String?,
    val desiccantDue: Boolean,
)

/** One change to the feeder's settings. */
sealed interface Setting {
    data class Plan(val on: Boolean) : Setting
    data class ChildLock(val on: Boolean) : Setting
    data class Light(val on: Boolean) : Setting
    data class Sound(val on: Boolean) : Setting
    data class LightAllowed(val on: Boolean) : Setting
    data class SoundAllowed(val on: Boolean) : Setting
    data class Volume(val percent: Int) : Setting
    data class Display(val on: Boolean) : Setting
    /** Only the non-null parts change. */
    data class Lid(val mode: String? = null, val speed: String? = null, val closeSec: Int? = null) : Setting
    data class DesiccantEvery(val days: Int) : Setting
    data object DesiccantReset : Setting
    data object OpenLid : Setting
    data class DisplayText(val text: String) : Setting
    data class DisplayIcon(val id: Int) : Setting
}

/** Petlibro's US cloud API: the same calls its Android app makes. */
object Petlibro {
    const val MAX_PORTIONS = 48
    private const val HOST = "https://api.us.petlibro.com"

    fun login(email: String, password: String): String {
        val zone = TimeZone.getDefault().id
        val hash = MessageDigest.getInstance("MD5").digest(password.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val body = JSONObject()
            .put("appId", 1)
            .put("appSn", "c35772530d1041699c87fe62348507a8")
            .put("country", "US")
            .put("email", email)
            .put("password", hash)
            .put("phoneBrand", "")
            .put("phoneSystemVersion", "")
            .put("timezone", zone)
            .put("thirdId", JSONObject.NULL)
            .put("type", JSONObject.NULL)
        val token = (request("/member/auth/login", body, null) as JSONObject).getString("token")
        if (token.isEmpty()) throw IOException("Petlibro sign-in returned an empty session.")
        return token
    }

    fun devices(token: String): List<Device> {
        val list = request("/device/device/list", JSONObject(), token) as JSONArray
        return (0 until list.length()).map { i ->
            val d = list.getJSONObject(i)
            Device(
                serial = d.optString("deviceSn"),
                name = d.optString("name", d.optString("deviceName", "Petlibro device")).ifBlank { "Petlibro device" },
                model = d.optString("productIdentifier"),
                product = d.optString("productName").takeIf { it.isNotBlank() && it != "null" },
            )
        }
    }

    /** The account's email (or nickname, if it has no email), for showing who's signed in. */
    fun accountName(token: String): String? {
        val info = request("/member/member/info", JSONObject(), token) as? JSONObject ?: return null
        return listOf("email", "nickname", "nickName")
            .map { info.optString(it) }
            .firstOrNull { it.isNotBlank() && it != "null" }
    }

    /**
     * Today's numbers from the feeder: how many times the pet ate (the RFID feeder counts each
     * visit) and how many meals went out. Petlibro gives counts, not the time of the last visit.
     */
    fun today(token: String, serial: String): TodayStats {
        val d = request("/device/data/grainStatus", JSONObject().put("id", serial).put("deviceSn", serial), token) as? JSONObject
            ?: throw IOException("Petlibro returned no feeder stats.")
        return TodayStats(
            day = java.time.LocalDate.now().toString(),
            eatingTimes = d.optInt("todayEatingTimes", -1),
            feedingTimes = d.optInt("todayFeedingTimes", -1),
            feedingPortions = d.optInt("todayFeedingQuantity", -1),
            // Today's total time at the bowl; the Home Assistant integration reads it as seconds.
            eatingSeconds = d.optInt("petEatingTime", -1),
        )
    }

    /**
     * Names of the pets bound to the feeder (its RFID tags). The integration this is taken from
     * doesn't name the field, so the likely ones are tried in turn.
     */
    fun pets(token: String, serial: String): List<String> {
        val list = request("/device/devicePetRelation/getBoundPets", JSONObject().put("deviceSn", serial), token) as? JSONArray
            ?: return emptyList()
        return (0 until list.length()).mapNotNull { i ->
            val pet = list.optJSONObject(i) ?: return@mapNotNull null
            listOf("name", "petName", "nickname", "nickName").map { pet.optString(it) }.firstOrNull { it.isNotBlank() && it != "null" }
        }
    }

    /** Live status and device info, from the same two calls the Petlibro app's device page makes. */
    fun details(token: String, serial: String): FeederDetails {
        val body = { JSONObject().put("id", serial).put("deviceSn", serial) }
        val real = request("/device/device/realInfo", body(), token) as? JSONObject ?: JSONObject()
        val base = runCatching { request("/device/device/baseInfo", body(), token) as? JSONObject }.getOrNull() ?: JSONObject()
        fun bool(o: JSONObject, k: String) = if (o.has(k) && !o.isNull(k)) o.optBoolean(k) else null
        fun int(o: JSONObject, k: String) = if (o.has(k) && !o.isNull(k)) o.optDouble(k).takeIf { !it.isNaN() }?.toInt() else null
        fun str(o: JSONObject, k: String) = o.optString(k).takeIf { it.isNotBlank() && it != "null" }
        fun any(k: String) = if (real.has(k)) real else base
        return FeederDetails(
            online = bool(any("online"), "online"),
            // surplusGrain is true while there's enough food.
            foodLow = bool(real, "surplusGrain")?.not(),
            lidOpen = bool(real, "barnDoorState"),
            lidBlocked = bool(real, "barnDoorError"),
            dispensing = str(real, "runningState")?.let { it == "RUNNING" },
            batteryState = str(real, "batteryState"),
            batteryPercent = int(real, "electricQuantity"),
            wifiName = str(any("wifiSsid"), "wifiSsid"),
            wifiRssi = int(any("wifiRssi"), "wifiRssi"),
            desiccantDaysLeft = int(any("remainingDesiccantDays"), "remainingDesiccantDays"),
            desiccantCycleDays = int(any("changeDesiccantFrequency"), "changeDesiccantFrequency"),
            feedingPlanOn = bool(any("enableFeedingPlan"), "enableFeedingPlan"),
            firmware = str(base, "softwareVersion") ?: str(real, "softwareVersion"),
            hardware = str(base, "hardwareVersion") ?: str(real, "hardwareVersion"),
            mac = str(any("mac"), "mac"),
        )
    }

    /** The feeder's settings, from its live info, its settings and its display. */
    fun config(token: String, serial: String): FeederConfig {
        val body = { JSONObject().put("id", serial).put("deviceSn", serial) }
        val real = request("/device/device/realInfo", body(), token) as? JSONObject ?: JSONObject()
        val attr = runCatching { request("/device/setting/getAttributeSetting", body(), token) as? JSONObject }.getOrNull() ?: JSONObject()
        val matrix = runCatching { request("/device/device/getDefaultMatrix", body(), token) as? JSONObject }.getOrNull() ?: JSONObject()
        // The feeding plan switch is in the device's base info, not its live info.
        val base = runCatching { request("/device/device/baseInfo", body(), token) as? JSONObject }.getOrNull() ?: JSONObject()
        fun bool(o: JSONObject, k: String) = if (o.has(k) && !o.isNull(k)) o.optBoolean(k) else null
        fun int(o: JSONObject, k: String) = if (o.has(k) && !o.isNull(k)) o.optDouble(k).takeIf { !it.isNaN() }?.toInt() else null
        fun str(o: JSONObject, k: String) = o.optString(k).takeIf { it.isNotBlank() && it != "null" }
        return FeederConfig(
            planOn = bool(real, "enableFeedingPlan") ?: bool(base, "enableFeedingPlan"),
            childLock = bool(real, "childLockSwitch"),
            lightOn = bool(real, "lightSwitch"),
            soundOn = bool(real, "soundSwitch"),
            lightAllowed = bool(real, "enableLight"),
            soundAllowed = bool(real, "enableSound"),
            noticeLowBattery = bool(real, "enableLowBatteryNotice"),
            noticePowerChange = bool(real, "enablePowerChangeNotice"),
            noticeOutletBlocked = bool(real, "enableGrainOutletBlockedNotice"),
            noticeRefill = bool(real, "enableReGrainNotice"),
            firmware = str(base, "softwareVersion") ?: str(real, "softwareVersion"),
            volume = int(attr, "volume"),
            displayOn = bool(real, "screenDisplaySwitch"),
            lidMode = str(attr, "coverOpenMode"),
            lidSpeed = str(attr, "coverCloseSpeed") ?: str(real, "coverCloseSpeed"),
            lidCloseSec = int(attr, "closeDoorTimeSec") ?: int(real, "closeDoorTimeSec"),
            desiccantEveryDays = int(real, "changeDesiccantFrequency"),
            displayText = str(matrix, "screenLetter"),
            displayIcon = int(matrix, "screenDisplayId"),
        )
    }

    /** Changes one setting; the calls and bodies are the Petlibro app's (via the Home Assistant integration). */
    fun set(token: String, serial: String, change: Setting) {
        val o = JSONObject().put("deviceSn", serial)
        val path = when (change) {
            is Setting.Plan -> { o.put("enable", change.on); "/device/setting/updateFeedingPlanSwitch" }
            is Setting.ChildLock -> { o.put("enable", change.on); "/device/setting/updateChildLockSwitch" }
            is Setting.Light -> { o.put("enable", change.on); "/device/setting/updateLightSwitch" }
            is Setting.Sound -> { o.put("enable", change.on); "/device/setting/updateSoundSwitch" }
            is Setting.LightAllowed -> { o.put("enable", change.on); "/device/setting/updateLightEnableSwitch" }
            is Setting.SoundAllowed -> { o.put("enable", change.on); "/device/setting/updateSoundEnableSwitch" }
            is Setting.Volume -> { o.put("volume", change.percent); "/device/setting/updateVolumeSetting" }
            is Setting.Display -> {
                o.put("screenDisplayAgingType", 1).put("screenDisplayStartTime", JSONObject.NULL)
                    .put("screenDisplayEndTime", JSONObject.NULL).put("screenDisplaySwitch", change.on)
                "/device/setting/updateDisplayMatrixSetting"
            }
            is Setting.Lid -> {
                o.put("coverOpenMode", change.mode ?: JSONObject.NULL)
                    .put("coverCloseSpeed", change.speed ?: JSONObject.NULL)
                    .put("closeDoorTimeSec", change.closeSec ?: JSONObject.NULL)
                "/device/setting/updateCoverSetting"
            }
            is Setting.DesiccantEvery -> {
                o.put("key", "DESSICANT").put("frequency", change.days)
                    .put("requestId", UUID.randomUUID().toString().replace("-", "")).put("timeout", 5000)
                "/device/device/maintenanceFrequencySetting"
            }
            Setting.DesiccantReset -> {
                o.put("requestId", UUID.randomUUID().toString().replace("-", "")).put("timeout", 5000)
                "/device/device/desiccantReset"
            }
            Setting.OpenLid -> { o.put("barnDoorState", true).put("timeout", 8000); "/device/device/doorStateChange" }
            is Setting.DisplayText -> {
                o.put("screenDisplayId", JSONObject.NULL).put("screenDisplayMatrix", JSONObject.NULL).put("screenLetter", change.text)
                "/device/device/displayMatrix"
            }
            is Setting.DisplayIcon -> {
                o.put("screenDisplayId", change.id).put("screenDisplayMatrix", JSONObject.NULL).put("screenLetter", JSONObject.NULL)
                "/device/device/displayMatrix"
            }
        }
        request(path, o, token)
    }

    /**
     * The feeder's own record of meals dispensed over the last [days] days, newest first: every
     * meal, whether PetBro, the Petlibro app, Petlibro's schedule or the feeder's button sent it.
     */
    fun records(token: String, serial: String, days: Int = 7): List<FeederRecord> {
        val now = System.currentTimeMillis()
        val body = JSONObject().put("deviceSn", serial).put("startTime", now - days * 86_400_000L).put("endTime", now)
            .put("size", 100).put("type", JSONArray().put("GRAIN_OUTPUT_SUCCESS"))
        val data = request("/device/workRecord/list", body, token) as? JSONArray ?: return emptyList()
        val out = mutableListOf<FeederRecord>()
        fun take(r: JSONObject) {
            val time = r.optLong("recordTime", 0L).takeIf { it > 0 } ?: return
            // Petlibro can log a meal before its dispensed count is in (0): fall back to what was asked for.
            val portions = r.optInt("actualGrainNum", 0).takeIf { it > 0 } ?: r.optInt("expectGrainNum", 0)
            out += FeederRecord(time, portions, r.optString("eventType"))
        }
        for (i in 0 until data.length()) {
            val day = data.optJSONObject(i) ?: continue
            val records = day.optJSONArray("workRecords")
            if (records != null) for (j in 0 until records.length()) records.optJSONObject(j)?.let(::take) else take(day)
        }
        return out.sortedByDescending { it.time }
    }

    /** A firmware update waiting for the feeder, or null when it's up to date. */
    fun firmwareUpdate(token: String, serial: String): FirmwareUpdate? {
        val d = request("/device/ota/getUpgrade", JSONObject().put("id", serial).put("deviceSn", serial), token) as? JSONObject ?: return null
        val job = d.optString("jobItemId").takeIf { it.isNotBlank() && it != "null" } ?: return null
        return FirmwareUpdate(
            jobItemId = job,
            version = d.optString("targetVersion").takeIf { it.isNotBlank() && it != "null" },
            notes = d.optString("upgradeDesc").takeIf { it.isNotBlank() && it != "null" },
            progress = if (d.has("progress") && !d.isNull("progress")) d.optInt("progress") else null,
        )
    }

    fun startFirmwareUpdate(token: String, serial: String, jobItemId: String) {
        request("/device/ota/doUpgrade", JSONObject().put("deviceSn", serial).put("jobItemId", jobItemId), token)
    }

    /** What's wrong with the feeder right now, from its status. */
    fun problems(token: String, serial: String): FeederProblems {
        val d = request("/data/data/realInfo", JSONObject().put("id", serial).put("deviceSn", serial), token) as? JSONObject ?: JSONObject()
        val message = d.optString("exceptionMessage").takeIf { it.isNotBlank() && it != "null" }
        return FeederProblems(
            offline = d.has("online") && !d.optBoolean("online", true),
            foodLow = d.has("surplusGrain") && !d.optBoolean("surplusGrain", true),
            lidBlocked = d.optBoolean("barnDoorError", false) || d.optString("doorErrorState", "NORMAL").let { it.isNotBlank() && it != "NORMAL" && it != "null" },
            error = message ?: if (d.optBoolean("errorState", false)) "The feeder reports an error." else null,
            desiccantDue = d.optString("desiccantState") == "OVER_DUE",
        )
    }

    fun feed(token: String, serial: String, portions: Int, requestId: String = UUID.randomUUID().toString()) {
        require(serial.isNotEmpty() && portions in 1..MAX_PORTIONS) { "Invalid feeding details" }
        val body = JSONObject().put("deviceSn", serial).put("grainNum", portions)
            .put("requestId", requestId.replace("-", ""))
        request("/device/device/manualFeeding", body, token)
    }

    private fun request(path: String, payload: JSONObject, token: String?): Any {
        val connection = URL(HOST + path).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("source", "ANDROID")
            connection.setRequestProperty("language", "EN")
            connection.setRequestProperty("timezone", TimeZone.getDefault().id)
            connection.setRequestProperty("version", "1.3.45")
            if (token != null) connection.setRequestProperty("token", token)
            val body = payload.toString().toByteArray()
            connection.setFixedLengthStreamingMode(body.size)

            val response = try {
                connection.outputStream.use { it.write(body) }
                val status = connection.responseCode
                if (status != 200) throw IOException("Petlibro returned HTTP $status.")
                connection.inputStream.use { input ->
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        if (bytes.size() + count > 1_048_576) throw IOException("Petlibro's response is too large.")
                        bytes.write(buffer, 0, count)
                    }
                    JSONObject(bytes.toString(Charsets.UTF_8.name()))
                }
            } catch (e: IOException) {
                if (e.message?.startsWith("Petlibro") == true) throw e
                throw IOException("Couldn't reach Petlibro (${e.javaClass.simpleName}).", e)
            }
            val code = response.getInt("code")
            if (token != null && code == 1009) throw SessionExpired()
            if (code != 0) {
                val operation = if (token == null) "Sign-in" else "Request"
                throw IOException("$operation rejected by Petlibro (code $code).")
            }
            return response.get("data")
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * The saved Petlibro session: its token, encrypted with an Android Keystore key, and the feeder
 * picked to feed. The prefs file and key alias are the first PetBro's, so an update stays signed in.
 */
object Session {
    private const val PREFS = "petlibro-session"
    private const val KEY = "feed-test"

    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasToken(context: Context) = prefs(context).contains("token")

    /** The saved token, or null if none is saved or it can't be decrypted. */
    fun token(context: Context): String? {
        val p = prefs(context)
        val encrypted = p.getString("token", null) ?: return null
        val iv = p.getString("iv", null) ?: return null
        return runCatching { decrypt(encrypted, iv) }.getOrNull()
    }

    fun saveToken(context: Context, token: String): Boolean {
        val (encrypted, iv) = encrypt(token)
        return prefs(context).edit().putString("token", encrypted).putString("iv", iv).commit()
    }

    fun email(context: Context): String? = prefs(context).getString("email", null)

    fun setEmail(context: Context, email: String) = prefs(context).edit().putString("email", email).apply()

    fun feeder(context: Context): String? = prefs(context).getString("feeder", null)

    fun setFeeder(context: Context, serial: String?) = prefs(context).edit().putString("feeder", serial).apply()

    fun clear(context: Context) {
        // Nicknames are PetBro's own and outlast a sign-out.
        val nicknames = prefs(context).all.filterKeys { it.startsWith("nick_") }
        prefs(context).edit().clear().apply {
            nicknames.forEach { (k, v) -> putString(k, v as? String) }
        }.apply()
    }

    fun nickname(context: Context, serial: String): String? = prefs(context).getString("nick_$serial", null)

    fun setNickname(context: Context, serial: String, name: String?) =
        prefs(context).edit().apply { if (name.isNullOrBlank()) remove("nick_$serial") else putString("nick_$serial", name.trim()) }.apply()

    /** A feeder's details as last read, shown while fresh ones load. */
    fun saveDetails(context: Context, serial: String, d: FeederDetails) =
        prefs(context).edit().putString("details_$serial", d.toJson().toString()).apply()

    fun details(context: Context, serial: String): FeederDetails? =
        prefs(context).getString("details_$serial", null)?.let { runCatching { FeederDetails.fromJson(JSONObject(it)) }.getOrNull() }

    fun saveConfig(context: Context, serial: String, c: FeederConfig) =
        prefs(context).edit().putString("config_$serial", c.toJson().toString()).apply()

    fun config(context: Context, serial: String): FeederConfig? =
        prefs(context).getString("config_$serial", null)?.let { runCatching { FeederConfig.fromJson(JSONObject(it)) }.getOrNull() }

    fun savePets(context: Context, names: List<String>) = prefs(context).edit().putString("pets", JSONArray(names).toString()).apply()

    fun pets(context: Context): List<String> = runCatching {
        val a = JSONArray(prefs(context).getString("pets", "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())

    fun saveToday(context: Context, t: TodayStats) = prefs(context).edit()
        .putString("todayDay", t.day).putInt("todayEating", t.eatingTimes)
        .putInt("todayFeeds", t.feedingTimes).putInt("todayPortions", t.feedingPortions)
        .putInt("todayEatingSeconds", t.eatingSeconds).apply()

    /** Today's feeder numbers as last read, or null if none were read today. */
    fun today(context: Context): TodayStats? {
        val p = prefs(context)
        val day = p.getString("todayDay", null) ?: return null
        return TodayStats(day, p.getInt("todayEating", -1), p.getInt("todayFeeds", -1), p.getInt("todayPortions", -1), p.getInt("todayEatingSeconds", -1)).takeIf { it.isToday }
    }

    /** The account's devices as last read, so the app can show them before Petlibro answers. */
    fun saveDevices(context: Context, devices: List<Device>) {
        val list = JSONArray(devices.map { JSONObject().put("serial", it.serial).put("name", it.name).put("model", it.model).put("product", it.product ?: JSONObject.NULL) })
        prefs(context).edit().putString("devices", list.toString()).apply()
    }

    fun devices(context: Context): List<Device>? = runCatching {
        val list = JSONArray(prefs(context).getString("devices", null) ?: return null)
        (0 until list.length()).map { i ->
            list.getJSONObject(i).let {
                Device(it.getString("serial"), it.getString("name"), it.getString("model"), product = it.optString("product").takeIf { p -> p.isNotBlank() && p != "null" })
            }
        }
    }.getOrNull()

    fun encrypt(text: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    fun decrypt(encrypted: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)))
    }

    @Synchronized
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY, null) as SecretKey?)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
