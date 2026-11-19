package local.petlibrocompanion

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The feeder's own settings: its Petlibro schedule, lid, sound, light, display, child lock and
 * desiccant. Shows what was read last time at once, then refreshes. A change shows straight
 * away and goes back if Petlibro refuses it.
 */
@Composable
fun ConfigureScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val account by Feeder.account.collectAsStateWithLifecycle()
    val feeder = (account as? AccountState.SignedIn)?.feeder
    val serial = feeder?.serial ?: Session.feeder(context)
    var config by remember(serial) { mutableStateOf(serial?.let { Session.config(context, it) }) }
    var loading by remember(serial) { mutableStateOf(false) }
    var error by remember(serial) { mutableStateOf<String?>(null) }

    fun reload() {
        val sn = serial ?: return
        loading = true
        scope.launch {
            val token = Session.token(context)
            val result = withContext(Dispatchers.IO) { runCatching { Petlibro.config(token ?: throw java.io.IOException("Not signed in."), sn) } }
            result.onSuccess {
                config = it
                error = null
                Session.saveConfig(context, sn, it)
            }.onFailure { error = it.message ?: "Couldn't reach Petlibro." }
            loading = false
        }
    }
    LaunchedEffect(serial) { reload() }

    // A firmware update waiting for the feeder; checked with the settings.
    var update by remember(serial) { mutableStateOf<FirmwareUpdate?>(null) }
    var updateChecked by remember(serial) { mutableStateOf(false) }
    var updating by remember(serial) { mutableStateOf(false) }
    LaunchedEffect(serial, updating) {
        val sn = serial ?: return@LaunchedEffect
        val token = Session.token(context) ?: return@LaunchedEffect
        // While updating, follow its progress until it's done (or for ten minutes).
        repeat(if (updating) 120 else 1) {
            val u = withContext(Dispatchers.IO) { runCatching { Petlibro.firmwareUpdate(token, sn) } }
            u.onSuccess { update = it; updateChecked = true }
            if (updating && u.getOrNull() == null && u.isSuccess) { updating = false; reload(); return@LaunchedEffect }
            if (updating) kotlinx.coroutines.delay(5_000)
        }
    }

    /** Shows [new] at once, sends [change], and puts the old settings back if it fails. */
    fun apply(change: Setting, new: FeederConfig? = config, done: String? = null) {
        val sn = serial ?: return
        val old = config
        config = new
        scope.launch {
            val token = Session.token(context)
            val result = withContext(Dispatchers.IO) { runCatching { Petlibro.set(token ?: throw java.io.IOException("Not signed in."), sn, change) } }
            result.onSuccess {
                new?.let { Session.saveConfig(context, sn, it) }
                done?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            }.onFailure {
                config = old
                Toast.makeText(context, it.message ?: "Petlibro didn't take that change.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Refresh beside the light/dark button, in the same color.
            Header("Configure", lead = {
                IconButton(onClick = { Haptics.tick(context); reload() }, enabled = !loading && serial != null) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(LucideRefreshCw, contentDescription = "Refresh settings", tint = headerTint(), modifier = Modifier.size(20.dp))
                }
            })
        },
    ) { padding ->
        GapColumn(
            16.dp,
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            val c = config
            when {
                serial == null -> SectionCard("Feeder", LucidePawPrint, palette.warning) {
                    Text("Sign in and pick a feeder on the Feeder tab to change its settings.", style = MaterialTheme.typography.bodySmall, color = noteColor)
                }
                c == null && error != null -> Notice(error!!, MaterialTheme.colorScheme.error, "Retry") { reload() }
                c == null -> SectionCard(feeder?.label ?: "Feeder", LucidePawPrint, palette.warning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Loading the feeder's settings…", style = MaterialTheme.typography.bodyMedium, color = noteColor)
                    }
                }
                else -> {
                    ConfigCards(c, error, ::apply)
                    FirmwareCard(c.firmware, update, updateChecked, updating) {
                        val sn = serial ?: return@FirmwareCard
                        val job = update?.jobItemId ?: return@FirmwareCard
                        scope.launch {
                            val token = Session.token(context)
                            val r = withContext(Dispatchers.IO) { runCatching { Petlibro.startFirmwareUpdate(token!!, sn, job) } }
                            r.onSuccess { updating = true; Toast.makeText(context, "Update started", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, it.message ?: "The update didn't start.", Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConfigCards(c: FeederConfig, error: String?, apply: (Setting, FeederConfig?, String?) -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val focus = LocalFocusManager.current
    error?.let { Notice("Showing saved settings. $it", MaterialTheme.colorScheme.error) }

    c.planOn?.let { on ->
        SectionCard("Petlibro schedule", LucideCalendarClock, palette.secondary) {
            SwitchRow(
                "Feed on Petlibro's schedule",
                "The feeding plan kept on the feeder, set in the Petlibro app. With PetBro's schedule on too, both feed.",
                on,
            ) { apply(Setting.Plan(it), c.copy(planOn = it), null) }
        }
    }

    SectionCard("Lid", LucidePawPrint, palette.warning) {
        c.lidMode?.let { mode ->
            Segmented("Mode", listOf("CUSTOM", "KEEP_OPEN", "KEEP_CLOSE"), mode, {
                when (it) { "KEEP_OPEN" -> "Open"; "KEEP_CLOSE" -> "Closed"; else -> "For pet" }
            }) {
                apply(Setting.Lid(mode = it), c.copy(lidMode = it), null)
            }
        }
        c.lidSpeed?.let { speed ->
            Segmented("Closes", listOf("SLOW", "MEDIUM", "FAST"), speed, { it.lowercase().replaceFirstChar { ch -> ch.uppercase() } }) {
                apply(Setting.Lid(speed = it), c.copy(lidSpeed = it), null)
            }
        }
        c.lidCloseSec?.let { sec ->
            // Sent when the stepper stops, not on every step of a hold.
            var shown by remember(sec) { mutableIntStateOf(sec.coerceIn(1, 10)) }
            LaunchedEffect(shown) {
                if (shown != sec) {
                    kotlinx.coroutines.delay(600)
                    apply(Setting.Lid(closeSec = shown), c.copy(lidCloseSec = shown), null)
                }
            }
            CountStepper("Close after", shown, 1..10, { "$it s" }) { shown = it }
        }
        FilledTonalButton(onClick = { Haptics.press(context); apply(Setting.OpenLid, c, "Opening the lid") }) {
            Text("Open the lid now")
        }
    }

    SectionCard("Sound and light", LucideVibrate, palette.tertiary) {
        // Petlibro has two switches each for sound and light: a master one that allows it, and
        // one that turns it on.
        c.soundAllowed?.let { on -> SwitchRow("Allow sound", "Petlibro's master switch for sound", on) { apply(Setting.SoundAllowed(it), c.copy(soundAllowed = it), null) } }
        c.soundOn?.let { on -> SwitchRow("Sound", null, on) { apply(Setting.Sound(it), c.copy(soundOn = it), null) } }
        c.volume?.let { v ->
            var shown by remember(v) { mutableFloatStateOf(v.toFloat()) }
            Row {
                Text("Volume", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("${shown.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = noteColor)
            }
            // Ten stops, 10% to 100%, with a mark at each.
            Slider(
                value = shown.coerceIn(10f, 100f),
                onValueChange = {
                    val stepped = (Math.round(it / 10f) * 10).coerceIn(10, 100)
                    if (stepped != shown.toInt()) Haptics.tick(context)
                    shown = stepped.toFloat()
                },
                onValueChangeFinished = { apply(Setting.Volume(shown.toInt()), c.copy(volume = shown.toInt()), null) },
                valueRange = 10f..100f,
                steps = 8,
            )
        }
        c.lightAllowed?.let { on -> SwitchRow("Allow light", "Petlibro's master switch for the indicator light", on) { apply(Setting.LightAllowed(it), c.copy(lightAllowed = it), null) } }
        c.lightOn?.let { on -> SwitchRow("Light", "The feeder's indicator light", on) { apply(Setting.Light(it), c.copy(lightOn = it), null) } }
    }

    SectionCard("Display", LucideHash, palette.mauve) {
        c.displayOn?.let { on -> SwitchRow("Screen", null, on) { apply(Setting.Display(it), c.copy(displayOn = it), null) } }
        // What the screen shows: one icon, or a few letters (setting one clears the other).
        val icons = listOf(5 to "Heart", 6 to "Dog", 7 to "Cat", 8 to "Elk")
        val TEXT = -2
        val current = c.displayIcon?.takeIf { id -> icons.any { it.first == id } } ?: if (c.displayText != null) TEXT else -1
        var picked by remember(current) { mutableIntStateOf(current) }
        Segmented("Shows", icons.map { it.first } + TEXT, picked, { id -> icons.firstOrNull { it.first == id }?.second ?: "Text" }) { id ->
            picked = id
            if (id != TEXT) apply(Setting.DisplayIcon(id), c.copy(displayIcon = id, displayText = null), null)
        }
        Reveal(picked == TEXT) {
            GapColumn(4.dp) {
                var text by remember(c.displayText) { mutableStateOf(c.displayText.orEmpty()) }
                fun sendText() {
                    focus.clearFocus()
                    val t = text.trim()
                    if (t.isNotEmpty() && t != c.displayText) apply(Setting.DisplayText(t), c.copy(displayText = t, displayIcon = null), "Sent to the screen")
                }
                AppField(
                    text, { text = it.take(12) }, null, "GINNY",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { sendText() }),
                    autoFocus = LocalAppearing.current,
                )
                if (text.trim().isNotEmpty() && text.trim() != c.displayText) {
                    TextButton(onClick = { sendText() }) { Text("Show this text") }
                }
            }
        }
    }

    SectionCard("Safety", LucideKeyRound, palette.warning) {
        c.childLock?.let { on -> SwitchRow("Child lock", "Locks the feeder's buttons", on) { apply(Setting.ChildLock(it), c.copy(childLock = it), null) } }
    }

    val notices = listOf(
        "Low battery" to c.noticeLowBattery,
        "Power change" to c.noticePowerChange,
        "Outlet blocked" to c.noticeOutletBlocked,
        "Refill reminder" to c.noticeRefill,
    ).filter { it.second != null }
    if (notices.isNotEmpty()) {
        SectionCard("Petlibro notifications", LucideBell, palette.secondary) {
            Text(
                "What the Petlibro app notifies you about for this feeder. Petlibro has no known way to change these from outside its app, so they're shown here only.",
                style = MaterialTheme.typography.bodySmall,
                color = noteColor,
            )
            notices.forEach { (label, on) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(if (on == true) "On" else "Off", style = MaterialTheme.typography.bodyMedium, color = if (on == true) palette.success else noteColor)
                }
            }
        }
    }

    SectionCard("Desiccant", LucideRefreshCw, palette.success) {
        c.desiccantEveryDays?.let { days ->
            var shown by remember(days) { mutableIntStateOf(days.coerceIn(1, 60)) }
            LaunchedEffect(shown) {
                if (shown != days) {
                    kotlinx.coroutines.delay(600)
                    apply(Setting.DesiccantEvery(shown), c.copy(desiccantEveryDays = shown), null)
                }
            }
            CountStepper("Change every", shown, 1..60, { if (it == 1) "1 day" else "$it days" }) { shown = it }
        }
        var confirming by remember { mutableStateOf(false) }
        FilledTonalButton(onClick = { confirming = true }) { Text("I changed it") }
        if (confirming) {
            AlertDialog(
                onDismissRequest = { confirming = false },
                title = { Text("Reset the desiccant countdown?") },
                text = { Text("Do this after putting in a fresh desiccant pack.") },
                confirmButton = {
                    TextButton(onClick = { confirming = false; apply(Setting.DesiccantReset, c, "Desiccant countdown reset") }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

/** "Label  −  value  +" for a whole number in [range]. */
@Composable
private fun CountStepper(label: String, value: Int, range: IntRange, format: (Int) -> String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RepeatButton(LucideMinus, "Less", enabled = value > range.first) { onChange((value - 1).coerceAtLeast(range.first)) }
        Text(
            format(value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        RepeatButton(LucidePlus, "More", enabled = value < range.last) { onChange((value + 1).coerceAtMost(range.last)) }
    }
}

/** The feeder's firmware, and an update when Petlibro has one, with its progress while it installs. */
@Composable
private fun FirmwareCard(current: String?, update: FirmwareUpdate?, checked: Boolean, updating: Boolean, onUpdate: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var confirming by remember { mutableStateOf(false) }
    SectionCard("Firmware", LucideZap, palette.tertiary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Version", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(current ?: "Unknown", style = MaterialTheme.typography.bodyMedium, color = noteColor)
        }
        when {
            updating -> {
                val p = update?.progress
                Text(if (p != null) "Updating… $p%" else "Updating…", style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
                if (p != null) LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Leave the feeder plugged in until it's done.", style = MaterialTheme.typography.bodySmall, color = noteColor)
            }
            update != null -> {
                Text("Update available" + (update.version?.let { ": $it" } ?: ""), style = MaterialTheme.typography.bodyMedium, color = palette.warning)
                update.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = noteColor) }
                Button(onClick = { Haptics.tick(context); confirming = true }, shape = MaterialTheme.shapes.extraLarge) { Text("Update the feeder") }
            }
            checked -> Text("Up to date", style = MaterialTheme.typography.bodyMedium, color = palette.success)
            else -> Text("Checking for updates…", style = MaterialTheme.typography.bodySmall, color = noteColor)
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Update the feeder?") },
            text = { Text("It may stop for a few minutes while it installs, so don't run it near a meal. Keep it plugged in.") },
            confirmButton = { TextButton(onClick = { confirming = false; onUpdate() }) { Text("Update") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}
