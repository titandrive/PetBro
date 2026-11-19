package local.petlibrocompanion

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * "Feed now" from outside the app: the Quick Settings tile, the launcher shortcut and broadcasts
 * from automation apps, as in PurgeBot. Each dispenses the quick-feed portions straight away,
 * with no sheet to confirm.
 */
object FeedNow {
    const val ACTION = "local.petlibrocompanion.action.FEED_NOW"
    /** Optional, for automation: how many portions, 1–48. Without it, the quick-feed amount. */
    const val EXTRA_PORTIONS = "portions"
    const val LABEL = "Feed now"
    private const val EXTRA_KEY = "key"
    private const val SHORTCUT_ID = "feed_now"

    /** The app's own way into [FeedNowActivity], carrying the key that lets it feed. */
    fun intent(context: Context): Intent = Intent(context, FeedNowActivity::class.java)
        .setAction(ACTION)
        .putExtra(EXTRA_KEY, Prefs.feedKey(context))

    /** Whether [intent] came from the app itself (or a shortcut it made), not another app. */
    fun isOwn(context: Context, intent: Intent): Boolean = intent.getStringExtra(EXTRA_KEY) == Prefs.feedKey(context)

    /** The launcher's long-press Feed now, published at runtime so it can carry the key. */
    fun publishShortcut(context: Context) {
        val shortcuts = context.getSystemService(ShortcutManager::class.java) ?: return
        val shortcut = ShortcutInfo.Builder(context, SHORTCUT_ID)
            .setShortLabel(LABEL)
            .setLongLabel("Feed ${portionsLabel(Prefs.quickPortions(context))}")
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent(context))
            .build()
        runCatching { shortcuts.dynamicShortcuts = listOf(shortcut) }
    }

    /** Dispenses [portions] (the quick-feed amount by default) and says how it went. */
    fun start(context: Context, portions: Int = Prefs.quickPortions(context)) {
        val app = context.applicationContext
        Feeder.init(app)
        when {
            !Feeder.ready(app) -> toast(app, "Open PetBro and sign in to feed")
            Feeder.feeding.value -> toast(app, "Already feeding")
            else -> {
                Haptics.press(app)
                toast(app, "Feeding ${portionsLabel(portions)}…")
                Feeder.feedNow(app, portions, FeedSource.QUICK) { entry ->
                    // One buzz on the tap is enough away from the app; the message says how it went.
                    toast(app, if (entry.ok) "Dispensing ${portionsLabel(entry.portions)}" else entry.message)
                }
            }
        }
    }

    fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

/** The launcher shortcut, and the trampoline for the tile. No window. */
class FeedNowActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            // Older shortcut pickers (and automation apps) ask for a shortcut instead of reading
            // the published ones. It carries the key, so only with automation on.
            if (Prefs.automationOn(this)) {
                @Suppress("DEPRECATION")
                setResult(
                    RESULT_OK,
                    Intent()
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, FeedNow.LABEL)
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher))
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, FeedNow.intent(this)),
                )
            } else {
                Toast.makeText(this, AUTOMATION_OFF, Toast.LENGTH_LONG).show()
            }
        } else if (savedInstanceState == null && !FeedNow.isOwn(this, intent)) {
            // Another app starting a feed directly, without a shortcut PetBro made.
            Toast.makeText(this, AUTOMATION_OFF, Toast.LENGTH_LONG).show()
        } else if (savedInstanceState == null) {
            FeedNow.start(this)
        }
        finish()
    }

    companion object {
        const val AUTOMATION_OFF = "To let other apps feed, turn on Allow automation apps in PetBro's Settings"
    }
}

/** Public broadcast entry point for Tasker, MacroDroid and the like. */
class FeedNowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FeedNow.ACTION) return
        // Any app can send this broadcast, so it only feeds once the user allows it.
        if (!Prefs.automationOn(context)) {
            Toast.makeText(context, FeedNowActivity.AUTOMATION_OFF, Toast.LENGTH_LONG).show()
            return
        }
        val portions = intent.getIntExtra(FeedNow.EXTRA_PORTIONS, Prefs.quickPortions(context))
        if (portions !in 1..Petlibro.MAX_PORTIONS) {
            Toast.makeText(context, "PetBro: portions must be 1 to ${Petlibro.MAX_PORTIONS}", Toast.LENGTH_LONG).show()
            return
        }
        // Keeps the process up until the request is answered.
        val pending = goAsync()
        Thread({
            try {
                Feeder.init(context.applicationContext)
                Feeder.dispense(context.applicationContext, portions, FeedSource.AUTOMATION)
            } finally {
                pending.finish()
            }
        }, "petbro-automation").start()
    }
}

/** Quick Settings tile: feeds the quick-feed amount. Unavailable until signed in. */
class FeedTileService : TileService() {
    private var scope: CoroutineScope? = null
    private var watching: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        Feeder.init(applicationContext)
        Prefs.load(applicationContext)
        val s = scope ?: MainScope().also { scope = it }
        watching?.cancel()
        watching = s.launch {
            combine(Feeder.feeding, Feeder.account, Prefs.quickPortionsFlow) { feeding, _, portions -> feeding to portions }
                .collect { (feeding, portions) -> refresh(feeding, portions) }
        }
    }

    override fun onStopListening() {
        watching?.cancel()
        watching = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope?.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (!Feeder.ready(this)) {
            // Opens the app to sign in.
            val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(open)
            }
            return
        }
        // Feeds right from the tile, without collapsing the shade, so you see it change.
        FeedNow.start(this)
    }

    private fun refresh(feeding: Boolean, portions: Int) {
        val tile = qsTile ?: return
        val ready = Feeder.ready(this)
        tile.label = FeedNow.LABEL
        tile.icon = Icon.createWithResource(this, R.drawable.ic_logo)
        tile.state = when {
            feeding -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                feeding -> "Feeding…"
                !ready -> "Sign in"
                else -> portionsLabel(portions)
            }
        }
        tile.contentDescription = if (feeding) "${FeedNow.LABEL}: feeding" else "${FeedNow.LABEL}, ${portionsLabel(portions)}"
        tile.updateTile()
    }
}
