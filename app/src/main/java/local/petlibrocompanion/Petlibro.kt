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
data class Device(val serial: String, val name: String, val model: String) {
    /** Feeders whose manual feeding PetBro has been tested with. */
    val canFeed: Boolean get() = model == FEEDER_MODEL

    companion object {
        const val FEEDER_MODEL = "PLAF301"
    }
}

/** Today's feeder numbers, for [day] (ISO date); -1 where Petlibro didn't say. */
data class TodayStats(val day: String, val eatingTimes: Int, val feedingTimes: Int, val feedingPortions: Int) {
    val isToday: Boolean get() = day == java.time.LocalDate.now().toString()
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

    fun clear(context: Context) = prefs(context).edit().clear().apply()

    fun savePets(context: Context, names: List<String>) = prefs(context).edit().putString("pets", JSONArray(names).toString()).apply()

    fun pets(context: Context): List<String> = runCatching {
        val a = JSONArray(prefs(context).getString("pets", "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())

    fun saveToday(context: Context, t: TodayStats) = prefs(context).edit()
        .putString("todayDay", t.day).putInt("todayEating", t.eatingTimes)
        .putInt("todayFeeds", t.feedingTimes).putInt("todayPortions", t.feedingPortions).apply()

    /** Today's feeder numbers as last read, or null if none were read today. */
    fun today(context: Context): TodayStats? {
        val p = prefs(context)
        val day = p.getString("todayDay", null) ?: return null
        return TodayStats(day, p.getInt("todayEating", -1), p.getInt("todayFeeds", -1), p.getInt("todayPortions", -1)).takeIf { it.isToday }
    }

    /** The account's devices as last read, so the app can show them before Petlibro answers. */
    fun saveDevices(context: Context, devices: List<Device>) {
        val list = JSONArray(devices.map { JSONObject().put("serial", it.serial).put("name", it.name).put("model", it.model) })
        prefs(context).edit().putString("devices", list.toString()).apply()
    }

    fun devices(context: Context): List<Device>? = runCatching {
        val list = JSONArray(prefs(context).getString("devices", null) ?: return null)
        (0 until list.length()).map { i ->
            list.getJSONObject(i).let { Device(it.getString("serial"), it.getString("name"), it.getString("model")) }
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
