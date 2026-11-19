package local.petlibrocompanion

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The widget's look, shared by every PetBro widget, as in Paid to Poop: its own theme or the
 * app's, light or dark, background opacity, what it shows and what a tap does. How many portions
 * a tap feeds is each widget's own.
 */
object WidgetPrefs {
    const val APP = "APP"
    data class Look(
        /** [APP], or a [ThemeFamily] name. */
        val theme: String = APP,
        /** [APP], SYSTEM, LIGHT or DARK. */
        val mode: String = APP,
        val opacity: Int = 100,
        val showLast: Boolean = true,
        val tapFeeds: Boolean = true,
    )

    private val _look = MutableStateFlow(Look())
    val look: StateFlow<Look> = _look

    private fun prefs(context: Context) = context.getSharedPreferences("widget", Context.MODE_PRIVATE)

    fun load(context: Context): Look {
        val p = prefs(context)
        return Look(
            theme = p.getString("theme", APP) ?: APP,
            mode = p.getString("mode", APP) ?: APP,
            opacity = p.getInt("opacity", 100),
            showLast = p.getBoolean("showLast", true),
            tapFeeds = p.getBoolean("tapFeeds", true),
        ).also { _look.value = it }
    }

    fun set(context: Context, look: Look) {
        prefs(context).edit()
            .putString("theme", look.theme).putString("mode", look.mode).putInt("opacity", look.opacity)
            .putBoolean("showLast", look.showLast).putBoolean("tapFeeds", look.tapFeeds)
            .apply()
        _look.value = look
        FeedWidget.updateAll(context)
    }

    /** What a tap on widget [id] feeds; the quick-feed amount until set. */
    fun portions(context: Context, id: Int) = prefs(context).getInt("portions_$id", Prefs.quickPortions(context))

    fun setPortions(context: Context, id: Int, n: Int) {
        prefs(context).edit().putInt("portions_$id", n).apply()
        FeedWidget.updateAll(context)
    }

    fun forget(context: Context, ids: IntArray) {
        prefs(context).edit().apply { ids.forEach { remove("portions_$it") } }.apply()
    }
}

/** The home-screen widget: a Feed Now button that can show the last feed. */
class FeedWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) updateAll(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(context, manager, it) }
        // The half-hourly update also re-reads today's feeder numbers for the large size.
        if (!Session.hasToken(context)) return
        val pending = goAsync()
        Thread({
            try {
                if (Feeder.readToday(context.applicationContext) != null) updateAll(context)
            } finally {
                pending.finish()
            }
        }, "petbro-widget-update").start()
    }

    /** Older launchers: a resize only reports the new size here, so draw for it. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        draw(context, manager, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) = WidgetPrefs.forget(context, ids)

    companion object {
        const val ACTION_FEED = "local.petlibrocompanion.widget.FEED"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, FeedWidget::class.java)).forEach { draw(context, manager, it) }
        }

        /**
         * Draws widget [id] at every size the launcher may show it, picking from three layouts:
         * small (just the paw), middle (the last feed) and large (with today's numbers and the
         * next meal). [feeding] shows "Feeding…" while a tap's request is out.
         */
        fun draw(context: Context, manager: AppWidgetManager, id: Int, feeding: Boolean = false) {
            val look = WidgetPrefs.load(context)
            Clock.load(context)
            val content = Content(context, look, id, feeding)
            val small = content.build(R.layout.feed_widget_small)
            val middle = content.build(R.layout.feed_widget)
            val large = content.build(R.layout.feed_widget_large)
            val views = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // The launcher picks the largest that fits, as the widget is resized.
                RemoteViews(mapOf(
                    android.util.SizeF(40f, 40f) to small,
                    android.util.SizeF(110f, 100f) to middle,
                    android.util.SizeF(150f, 190f) to large,
                ))
            } else {
                // Older launchers report the size in the widget's options.
                val options = manager.getAppWidgetOptions(id)
                val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
                val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 100)
                when {
                    w < 110 || h < 100 -> small
                    w >= 150 && h >= 190 -> large
                    else -> middle
                }
            }
            manager.updateAppWidget(id, views)
        }

        /** What one widget shows, built into each size's layout. */
        private class Content(val context: Context, val look: WidgetPrefs.Look, val id: Int, val feeding: Boolean) {
            val portions = WidgetPrefs.portions(context, id)
            val palette = palette(context, look)
            val accent = palette.mauve.toArgb()
            val text = palette.text.toArgb()
            val background = if (palette.mantle.toArgb() == android.graphics.Color.BLACK) android.graphics.Color.BLACK else palette.surface0.toArgb()
            val last = FeedLog.lastFed(context)

            fun build(layout: Int): RemoteViews {
                val views = RemoteViews(context.packageName, layout)
                views.setInt(R.id.widget_paw, "setColorFilter", accent)
                if (layout == R.layout.feed_widget_small) {
                    setText(context, views, R.id.widget_sub, if (feeding) "…" else "$portions", accent, 12f, R.font.jetbrains_mono_semibold, 0f)
                } else {
                    val showLast = look.showLast && last != null && !feeding
                    setText(context, views, R.id.widget_label, if (showLast) "LAST FED" else "FEED NOW", accent, 11f, R.font.jetbrains_mono_medium, 0.12f)
                    if (showLast) {
                        setText(context, views, R.id.widget_big, shortWhen(last!!.time), accent, 30f, R.font.jetbrains_mono_bold, 0f)
                        views.setViewVisibility(R.id.widget_big, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_paw, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_big, View.GONE)
                        views.setViewVisibility(R.id.widget_paw, View.VISIBLE)
                    }
                    val sub = when {
                        feeding -> "Feeding…"
                        showLast && look.tapFeeds -> "Tap to feed"
                        showLast -> portionsLabel(last!!.portions)
                        else -> portionsLabel(portions)
                    }
                    setText(context, views, R.id.widget_sub, sub, accent, 13f, R.font.jetbrains_mono_regular, 0f)
                    if (layout == R.layout.feed_widget_large) {
                        setText(context, views, R.id.widget_stats, stats(), text, 12f, R.font.jetbrains_mono_regular, 0f)
                        views.setInt(R.id.widget_divider, "setBackgroundColor", (accent and 0xFFFFFF) or (0x40 shl 24))
                    }
                }
                // The background in the theme's color at the set opacity, keeping its rounded corners.
                val color = (background and 0xFFFFFF) or ((look.opacity.coerceIn(0, 100) * 255 / 100) shl 24)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    views.setColorStateList(R.id.widget_container, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(color))
                } else {
                    views.setInt(R.id.widget_container, "setBackgroundColor", color)
                }
                views.setOnClickPendingIntent(R.id.widget_container, tap())
                return views
            }

            /** Today's feeds, how often the pet ate (from the feeder) and the next meal, a line each. */
            fun stats(): String {
                val fed = FeedLog.todayFed(context)
                val lines = mutableListOf("Today: " + if (fed.isEmpty()) "no feeds" else "${fed.size}× · ${fed.sumOf { it.portions }} portions")
                Session.today(context)?.eatingTimes?.takeIf { it >= 0 }?.let { n ->
                    val pet = Session.pets(context).singleOrNull() ?: "Pet"
                    lines += "$pet ate ${if (n == 1) "once" else "$n times"}"
                }
                val next = if (Meals.scheduleOn(context)) Meals.nextDue(Meals.load(context)) else null
                lines += if (next == null) "No meals scheduled" else "Next: " + next.first.toLocalTime().format(Clock.format) +
                    if (next.first.toLocalDate() != LocalDate.now()) " tmrw" else ""
                return lines.joinToString("\n")
            }

            fun tap(): PendingIntent = if (look.tapFeeds) {
                PendingIntent.getBroadcast(
                    context, id,
                    Intent(context, FeedWidgetReceiver::class.java).setAction(ACTION_FEED)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } else {
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        }

        /** The theme the widget draws in: its own, or the app's, light or dark as set. */
        private fun palette(context: Context, look: WidgetPrefs.Look): Palette {
            ThemeStore.load(context)
            val family = if (look.theme == WidgetPrefs.APP) ThemeStore.family.value else themeFamilyNamed(look.theme) ?: ThemeStore.family.value
            val system = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val appDark = when (ThemeStore.mode.value) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> system }
            val followsApp = look.theme == WidgetPrefs.APP || look.mode == WidgetPrefs.APP
            val dark = when {
                look.theme == WidgetPrefs.APP || look.mode == WidgetPrefs.APP -> appDark
                look.mode == "LIGHT" -> false
                look.mode == "DARK" -> true
                else -> system
            }
            // AMOLED black when following the app's dark mode, as in Paid to Poop.
            return when {
                !dark -> family.light
                followsApp && ThemeStore.amoled.value -> family.dark.amoled(ThemeStore.ultra.value)
                else -> family.dark
            }
        }

        /** "18:45" today, "Yest 18:45", or "Tue 18:45" within a week; else the date. */
        private fun shortWhen(ms: Long): String {
            val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
            val time = t.toLocalTime().format(Clock.format)
            val today = LocalDate.now()
            return when {
                t.toLocalDate() == today -> time
                t.toLocalDate() == today.minusDays(1) -> "Yest $time"
                t.toLocalDate().isAfter(today.minusDays(7)) -> t.format(java.time.format.DateTimeFormatter.ofPattern("EEE")) + " $time"
                else -> t.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
            }
        }

        /**
         * Widget text as an image, in the app's own font: launchers draw widgets themselves and
         * can swap a font for the system's (as in Paid to Poop).
         */
        private fun setText(context: Context, views: RemoteViews, id: Int, text: String, color: Int, sizeSp: Float, font: Int, spacing: Float) {
            val metrics = context.resources.displayMetrics
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ResourcesCompat.getFont(context, font)
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
                textAlign = Paint.Align.CENTER
                letterSpacing = spacing
                this.color = color
            }
            val lines = text.split("\n")
            val gap = 3 * metrics.density
            val lineHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
            val width = (lines.maxOf { paint.measureText(it) }.toInt() + 2).coerceAtLeast(1)
            val height = (lines.size * lineHeight + (lines.size - 1) * gap).toInt() + 1
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            lines.forEachIndexed { i, line -> canvas.drawText(line, width / 2f, i * (lineHeight + gap) - paint.fontMetrics.ascent, paint) }
            views.setImageViewBitmap(id, bitmap)
            views.setContentDescription(id, text.replace("\n", ", "))
        }
    }
}

/** A tap on the widget: feeds that widget's portions and says how it went. */
class FeedWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FeedWidget.ACTION_FEED) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val app = context.applicationContext
        if (!Feeder.ready(app)) {
            Toast.makeText(app, "Open PetBro and sign in to feed", Toast.LENGTH_SHORT).show()
            return
        }
        val portions = WidgetPrefs.portions(app, id)
        Haptics.press(app)
        FeedWidget.draw(app, AppWidgetManager.getInstance(app), id, feeding = true)
        val pending = goAsync()
        Thread({
            try {
                Feeder.init(app)
                val entry = Feeder.dispense(app, portions, FeedSource.QUICK)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (entry.ok) Haptics.finished(app)
                    Toast.makeText(app, if (entry.ok) "Dispensing ${portionsLabel(portions)}" else entry.message, Toast.LENGTH_SHORT).show()
                }
            } finally {
                FeedWidget.updateAll(app)
                pending.finish()
            }
        }, "petbro-widget").start()
    }
}

/**
 * The widget's settings, opened by the launcher when the widget is placed and from its
 * long-press menu: this widget's portions, then the look every widget shares.
 */
class FeedWidgetConfigure : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Every setting is optional: the widget stays however this page is left.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        ThemeStore.load(this)
        Prefs.load(this)
        Clock.load(this)
        WidgetPrefs.load(this)
        setContent {
            val mode by ThemeStore.mode.collectAsStateWithLifecycle()
            val family by ThemeStore.family.collectAsStateWithLifecycle()
            val amoled by ThemeStore.amoled.collectAsStateWithLifecycle()
            val ultra by ThemeStore.ultra.collectAsStateWithLifecycle()
            AppTheme(paletteFor(family, mode, amoled, ultra)) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = { Header("Widget", modeToggle = false) },
                    bottomBar = {
                        Button(
                            onClick = { finish() },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                        ) { Text("Done") }
                    },
                ) { padding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            var portions by remember { mutableIntStateOf(WidgetPrefs.portions(this@FeedWidgetConfigure, id)) }
                            SectionCard("This widget", LucidePawPrint, LocalPalette.current.warning) {
                                Text("How much a tap on this widget feeds.", style = MaterialTheme.typography.bodySmall, color = noteColor)
                                PortionStepper(portions) {
                                    portions = it
                                    WidgetPrefs.setPortions(this@FeedWidgetConfigure, id, it)
                                }
                            }
                        }
                        WidgetLookCard()
                    }
                }
            }
        }
    }
}

/** The look every widget shares, as in Paid to Poop's widget settings. Also in the app's Settings. */
@Composable
fun WidgetLookCard() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val look by WidgetPrefs.look.collectAsStateWithLifecycle()
    val appFamily by ThemeStore.family.collectAsStateWithLifecycle()
    fun set(l: WidgetPrefs.Look) = WidgetPrefs.set(context, l)
    SectionCard("Widget look", LucidePalette, palette.mauve) {
        val matching = look.theme == WidgetPrefs.APP
        SwitchRow(
            "Match app theme",
            if (matching) "Using ${appFamily.label}" else "Uses its own theme",
            matching,
        ) { on -> set(look.copy(theme = if (on) WidgetPrefs.APP else appFamily.name)) }
        Reveal(!matching) {
            Column(Modifier.padding(bottom = 4.dp)) {
                ThemePicker(themeFamilyNamed(look.theme) ?: appFamily) { set(look.copy(theme = it.name)) }
            }
        }
        Reveal(!matching) {
            Column {
                Text("Dark mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                PillSegmented(listOf(WidgetPrefs.APP, "SYSTEM", "LIGHT", "DARK"), look.mode, {
                    when (it) { WidgetPrefs.APP -> "App"; "SYSTEM" -> "System"; "LIGHT" -> "Light"; else -> "Dark" }
                }) { set(look.copy(mode = it)) }
            }
        }
        Column {
            Row {
                Text("Opacity", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("${look.opacity}%", style = MaterialTheme.typography.bodyMedium, color = noteColor)
            }
            var opacity by remember(look.opacity) { mutableFloatStateOf(look.opacity.toFloat()) }
            Slider(
                value = opacity,
                onValueChange = {
                    val stepped = (it / 10).toInt() * 10
                    if (stepped != opacity.toInt()) Haptics.tick(context)
                    opacity = stepped.toFloat()
                },
                onValueChangeFinished = { set(look.copy(opacity = opacity.toInt())) },
                valueRange = 0f..100f,
                steps = 9,
            )
        }
        SwitchRow("Show last feed", if (look.showLast) "Shows when your pet was last fed" else "Shows the paw and portions", look.showLast) {
            set(look.copy(showLast = it))
        }
        SwitchRow("Tap to feed", if (look.tapFeeds) "Tapping the widget feeds right away" else "Tapping the widget opens PetBro", look.tapFeeds) {
            set(look.copy(tapFeeds = it))
        }
    }
}
