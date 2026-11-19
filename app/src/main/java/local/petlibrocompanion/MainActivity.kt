package local.petlibrocompanion

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The sign-in form and feeder details stay out of screenshots and the recents preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        ThemeStore.load(this)
        Prefs.load(this)
        Meals.init(this)
        Feeder.init(this)
        Clock.load(this)
        WidgetPrefs.load(this)
        MealScheduler.reschedule(this)
        FeedNow.publishShortcut(this)
        setContent {
            val mode by ThemeStore.mode.collectAsStateWithLifecycle()
            val family by ThemeStore.family.collectAsStateWithLifecycle()
            val amoled by ThemeStore.amoled.collectAsStateWithLifecycle()
            val ultra by ThemeStore.ultra.collectAsStateWithLifecycle()
            val palette = paletteFor(family, mode, amoled, ultra)
            LaunchedEffect(palette) { window.setBackgroundDrawable(ColorDrawable(palette.mantle.toArgb())) }
            val darkPage = palette.mantle.luminance() < 0.5f
            LaunchedEffect(darkPage) {
                val style = if (darkPage) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AppTheme(palette) { App() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up a change to 24-hour time made while the app was in the background.
        Clock.load(this)
    }
}

/** The app's four places, along the bottom, with Feed Now in the middle. */
private enum class Tab(val label: String) { HOME("Feeder"), SCHEDULE("Schedule"), LOG("History"), SETTINGS("Settings") }

/** Times as the phone shows them: 24-hour or AM/PM, as set in Android's clock settings. */
private val timeFormat: DateTimeFormatter get() = Clock.format
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/** "Today, 8:00 AM", "Tomorrow, …" or "Tue 18 Nov, …". */
private fun whenLabel(time: LocalDateTime): String {
    val today = LocalDate.now()
    val day = when (time.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> time.format(dayFormat)
    }
    return "$day, ${time.toLocalTime().format(timeFormat)}"
}

private fun whenLabel(ms: Long) = whenLabel(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), java.time.ZoneId.systemDefault()))

@Composable
private fun App() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var feeding by rememberSaveable { mutableStateOf(false) }
    val account by Feeder.account.collectAsStateWithLifecycle()
    val busy by Feeder.feeding.collectAsStateWithLifecycle()
    val scheduleOn by Meals.enabled.collectAsStateWithLifecycle()
    val ready = (account as? AccountState.SignedIn)?.feeder != null ||
        (account is AccountState.Offline && Feeder.ready(context))

    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    // Tabs sit side by side, so a sideways swipe moves between them, as in PurgeBot.
    val pager = rememberPagerState(initialPage = tab.ordinal) { Tab.entries.size }
    var sliding by remember { mutableStateOf(false) }
    LaunchedEffect(tab) {
        if (pager.targetPage != tab.ordinal) {
            sliding = true
            try { pager.animateScrollToPage(tab.ordinal) } finally { sliding = false }
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.targetPage }.collect { page ->
            if (!sliding && Tab.entries[page] != tab) {
                Haptics.tick(context)
                tab = Tab.entries[page]
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().clearFocusOnTap()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                TabBar(
                    selected = tab,
                    scheduled = scheduleOn,
                    feed = when {
                        busy -> FeedState.FEEDING
                        ready -> FeedState.READY
                        else -> FeedState.BLOCKED
                    },
                    // A tap feeds the set amount straight away; holding opens the sheet to change it.
                    onFeed = { configure ->
                        when {
                            !ready -> {
                                Toast.makeText(context, if (account is AccountState.SignedIn) "Pick a feeder first" else "Sign in to feed", Toast.LENGTH_SHORT).show()
                                tab = Tab.HOME
                            }
                            configure -> feeding = true
                            busy -> {}
                            else -> feed(context, Prefs.quickPortions(context))
                        }
                    },
                    onSelect = { tab = it },
                )
            },
        ) { padding ->
            Box(Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding()))) {
                HorizontalPager(
                    pager,
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        pager,
                        pagerSnapDistance = PagerSnapDistance.atMost(1),
                        snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f),
                    ),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    when (Tab.entries[page]) {
                        Tab.HOME -> HomeScreen(onOpenSchedule = { tab = Tab.SCHEDULE }, onOpenLog = { tab = Tab.LOG })
                        Tab.SCHEDULE -> ScheduleScreen()
                        Tab.LOG -> LogScreen()
                        Tab.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }

    if (feeding) FeedSheet(onDismiss = { feeding = false })
}

private enum class FeedState { READY, FEEDING, BLOCKED }

/** Feed Now from the app: dispenses [portions] and says how it went. */
private fun feed(context: android.content.Context, portions: Int) {
    Feeder.feedNow(context, portions) { entry ->
        if (entry.ok) Haptics.finished(context)
        Toast.makeText(
            context,
            if (entry.ok) "Dispensing ${portionsLabel(entry.portions)}" else entry.message,
            Toast.LENGTH_LONG,
        ).show()
    }
}

@Composable
private fun TabBar(selected: Tab, scheduled: Boolean, feed: FeedState, onFeed: (configure: Boolean) -> Unit, onSelect: (Tab) -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    Box(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
        ) {
            Tab.entries.forEach { tab ->
                // Room in the middle for Feed Now.
                if (tab == Tab.LOG) Spacer(Modifier.width(82.dp))
                val on = tab == selected
                val tint = if (on) palette.mauve else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) palette.mauve.copy(alpha = 0.16f) else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab) {
                            Haptics.tick(context)
                            onSelect(tab)
                        },
                ) {
                    when (tab) {
                        Tab.HOME -> Icon(LucideHouse, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                        // A dot while the schedule is on.
                        Tab.SCHEDULE -> BadgedBox(badge = { if (scheduled) Badge(containerColor = palette.success) }) {
                            Icon(LucideCalendarClock, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                        }
                        Tab.LOG -> Icon(LucideHistory, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                        Tab.SETTINGS -> Icon(LucideSettings, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        FeedButton(feed, onFeed, Modifier.align(Alignment.TopCenter).offset(y = (-16).dp))
    }
}

/**
 * Feed Now: a round button in the middle of the tab bar that stands up out of it, like PurgeBot's
 * Start. A tap feeds the set amount; holding opens the sheet to change it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedButton(state: FeedState, onFeed: (configure: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val fill = when (state) {
        FeedState.READY -> palette.warning
        FeedState.FEEDING -> palette.mauve
        FeedState.BLOCKED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (state == FeedState.BLOCKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(66.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .combinedClickable(
                onClickLabel = "Feed now",
                onLongClickLabel = "Change portions",
                onLongClick = {
                    Haptics.press(context)
                    onFeed(true)
                },
                onClick = {
                    Haptics.press(context)
                    onFeed(false)
                },
            ),
    ) {
        if (state == FeedState.FEEDING) {
            CircularProgressIndicator(Modifier.matchParentSize().padding(4.dp), color = content, strokeWidth = 2.5.dp)
        }
        Icon(LucidePawPrint, contentDescription = null, tint = content, modifier = Modifier.size(30.dp))
    }
}

/** Held Feed Now: sets how much a tap feeds, and can dispense it now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val portions by Prefs.quickPortionsFlow.collectAsStateWithLifecycle()
    val account by Feeder.account.collectAsStateWithLifecycle()
    val name = (account as? AccountState.SignedIn)?.feeder?.name
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        GapColumn(12.dp, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Column {
                Text("Feed now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    (name?.let { "$it · " } ?: "") + "A tap on the paw feeds this much",
                    style = MaterialTheme.typography.bodySmall,
                    color = noteColor,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PortionStepper(portions) { Prefs.setQuickPortions(context, it) }
            }
            Button(
                onClick = {
                    Haptics.press(context)
                    onDismiss()
                    if (!Feeder.feeding.value) feed(context, portions)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalPalette.current.warning),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(LucidePawPrint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Dispense ${portionsLabel(portions)}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ScreenColumn(title: String, header: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header(title, action = header) },
    ) { padding ->
        GapColumn(
            16.dp,
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------- Feeder (home)

@Composable
private fun HomeScreen(onOpenSchedule: () -> Unit, onOpenLog: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val account by Feeder.account.collectAsStateWithLifecycle()
    ScreenColumn("PetBro") {
        when (val a = account) {
            AccountState.SignedOut -> SignInCard()
            is AccountState.Checking -> SectionCard("Petlibro", LucideRouter, palette.secondary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(a.message, style = MaterialTheme.typography.bodyMedium, color = noteColor)
                }
            }
            is AccountState.Offline -> {
                SectionCard("Feeder", LucidePawPrint, palette.warning) {
                    Notice(a.message, MaterialTheme.colorScheme.error, "Retry") { Feeder.refresh(context) }
                    Text("Your sign-in is saved. Feed Now and the schedule use it once Petlibro can be reached.", style = MaterialTheme.typography.bodySmall, color = noteColor)
                }
                AccountCard(a.email)
            }
            is AccountState.SignedIn -> {
                FeederCard(a)
                TodayCard(onOpenSchedule, onOpenLog)
                AccountCard(a.email)
            }
        }
    }
}

@Composable
private fun SignInCard() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val focus = LocalFocusManager.current
    val error by Feeder.error.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf(Session.email(context).orEmpty()) }
    // Not saved across rotation or process death: the password lives only as long as the form.
    var password by remember { mutableStateOf("") }
    val canSubmit = email.isNotBlank() && password.isNotEmpty()
    fun submit() {
        if (!canSubmit) return
        focus.clearFocus()
        Feeder.signIn(context, email.trim(), password)
        password = ""
    }
    SectionCard("Petlibro account", LucideUser, palette.mauve) {
        Text("Sign in with a US Petlibro account that has access to your feeder.", style = MaterialTheme.typography.bodySmall, color = noteColor)
        AppField(
            email, { email = it }, "Email", "you@example.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        AppField(
            password, { password = it }, "Password", null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        error?.let { Notice(it, MaterialTheme.colorScheme.error) }
        Button(
            onClick = { Haptics.press(context); submit() },
            enabled = canSubmit,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp),
        ) { Text("Sign in", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun FeederCard(a: AccountState.SignedIn) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val feeders = a.devices.filter { it.canFeed }
    SectionCard(
        "Feeder",
        LucidePawPrint,
        palette.warning,
        action = {
            // Spins while the saved devices are being checked in the background.
            val refreshing by Feeder.refreshing.collectAsStateWithLifecycle()
            IconButton(onClick = { Haptics.tick(context); Feeder.refresh(context) }, enabled = !refreshing) {
                if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(LucideRefreshCw, contentDescription = "Refresh devices", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        },
    ) {
        val stale by Feeder.stale.collectAsStateWithLifecycle()
        stale?.let { Notice("Showing saved devices. $it", MaterialTheme.colorScheme.error) }
        when {
            feeders.isEmpty() -> {
                Notice("No ${Device.FEEDER_MODEL} feeder on this account. Share the feeder with it in the Petlibro app.", palette.warning)
            }
            feeders.size == 1 -> DeviceRow(feeders[0], picked = true, onPick = null)
            else -> {
                Text("Pick the feeder PetBro feeds", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                feeders.forEach { d -> DeviceRow(d, picked = d.serial == a.feeder?.serial) { Feeder.pickFeeder(context, d) } }
            }
        }
        val others = a.devices.filterNot { it.canFeed }
        if (others.isNotEmpty()) {
            Text(
                "Also on this account: " + others.joinToString { it.name + if (it.model.isNotEmpty()) " (${it.model})" else "" },
                style = MaterialTheme.typography.bodySmall,
                color = noteColor,
            )
        }
    }
}

@Composable
private fun DeviceRow(device: Device, picked: Boolean, onPick: (() -> Unit)?) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // With several feeders to pick from, the picked one is outlined in the accent.
            .then(if (picked && onPick != null) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier)
            .then(if (onPick != null) Modifier.selectable(picked, role = Role.RadioButton) { Haptics.tick(context); onPick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Under the picked feeder, how often the pet ate today (counted by the feeder from
            // its RFID tag); the model otherwise.
            val today by Feeder.today.collectAsStateWithLifecycle()
            val pets by Feeder.pets.collectAsStateWithLifecycle()
            val ate = today?.takeIf { picked && it.isToday && it.eatingTimes >= 0 }?.let {
                "${pets.singleOrNull() ?: "Your pet"} ate " + if (it.eatingTimes == 1) "once today" else "${it.eatingTimes} times today"
            }
            Text(ate ?: device.model, style = MaterialTheme.typography.bodySmall, color = noteColor)
        }
        if (picked) Pill("Ready", palette.success)
    }
}

/** What's next and what happened last. */
@Composable
private fun TodayCard(onOpenSchedule: () -> Unit, onOpenLog: () -> Unit) {
    val palette = LocalPalette.current
    val meals by Meals.meals.collectAsStateWithLifecycle()
    val on by Meals.enabled.collectAsStateWithLifecycle()
    val log by FeedLog.entries.collectAsStateWithLifecycle()
    val next = if (on) Meals.nextDue(meals) else null
    SectionCard("Meals", LucideCalendarClock, palette.secondary) {
        InfoRow(
            "Next meal",
            when {
                !on -> "Schedule off"
                next == null -> "None scheduled"
                else -> "${next.second.joinToString { it.name }} · ${whenLabel(next.first)}"
            },
            onClick = onOpenSchedule,
        )
        val last = log.firstOrNull()
        InfoRow(
            "Last feed",
            last?.let { "${portionsLabel(it.portions)} · ${whenLabel(it.time)}" + if (it.ok) "" else " (not confirmed)" } ?: "None yet",
            onClick = onOpenLog,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { Haptics.tick(context); onClick() }
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(LucideChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccountCard(email: String?) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var confirming by remember { mutableStateOf(false) }
    // Sign out sits in the card's corner, like Refresh on the feeder card, to keep the page short.
    SectionCard(
        "Petlibro account",
        LucideUser,
        palette.mauve,
        action = {
            IconButton(onClick = { Haptics.tick(context); confirming = true }) {
                Icon(LucideLogOut, contentDescription = "Sign out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        },
    ) {
        Text(email?.let { "Signed in as $it" } ?: "Signed in", style = MaterialTheme.typography.bodyMedium)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Sign out?") },
            text = { Text("Scheduled meals won't go out until you sign in again.") },
            confirmButton = {
                TextButton(onClick = { confirming = false; Feeder.signOut(context) }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

// ---------------------------------------------------------------- Schedule

@Composable
private fun ScheduleScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val meals by Meals.meals.collectAsStateWithLifecycle()
    val on by Meals.enabled.collectAsStateWithLifecycle()
    val account by Feeder.account.collectAsStateWithLifecycle()
    var askingBattery by remember { mutableStateOf(false) }
    var backgroundOk by remember { mutableStateOf(MealScheduler.batteryExempt(context)) }
    var exactOk by remember { mutableStateOf(MealScheduler.canSchedule(context)) }
    LifecycleResumeEffect(Unit) {
        backgroundOk = MealScheduler.batteryExempt(context)
        exactOk = MealScheduler.canSchedule(context)
        onPauseOrDispose {}
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun update(meal: Meal) = Meals.set(context, meals.map { if (it.id == meal.id) meal else it })

    ScreenColumn("Schedule") {
        SectionCard("Feeding schedule", LucideCalendarClock, palette.secondary) {
            SwitchRow("Feed automatically", null, on) { enable ->
                if (enable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!backgroundOk) askingBattery = true
                    if (meals.isEmpty()) Meals.set(context, listOf(Meal()))
                }
                Meals.setEnabled(context, enable)
            }
            val next = Meals.nextDue(meals)
            val signedOut = account is AccountState.SignedOut
            val exactOff = !exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            // With one meal its own card says when it's next, so the summary only shows for two
            // or more (or when nothing's coming up). Nothing to show, nothing taking up room.
            val summary = next == null || meals.count { it.enabled } > 1
            Reveal(on && (signedOut || !backgroundOk || exactOff || summary)) {
                GapColumn(12.dp, modifier = Modifier.padding(top = 4.dp)) {
                    if (signedOut) {
                        Notice("Sign in on the Feeder tab, or meals can't go out", MaterialTheme.colorScheme.error)
                    }
                    if (!backgroundOk) {
                        Notice("Battery optimization can hold meals back", MaterialTheme.colorScheme.error, "Fix") { askingBattery = true }
                    }
                    if (exactOff) {
                        Notice("Alarms & reminders is off, so meals may be late", MaterialTheme.colorScheme.error, "Fix") {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }
                    }
                    if (summary) Text(
                        if (next == null) "No meals coming up"
                        else "Next: ${next.second.joinToString { it.name }} at ${whenLabel(next.first)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (next == null) MaterialTheme.colorScheme.error else palette.secondary,
                    )
                }
            }
        }

        Reveal(on) {
            GapColumn(16.dp) {
                meals.sortedBy { it.hour * 60 + it.minute }.forEach { meal ->
                    MealCard(meal, canRemove = meals.size > 1, onChange = ::update) {
                        Meals.set(context, meals.filterNot { it.id == meal.id })
                    }
                }
                FilledTonalButton(
                    onClick = {
                        Haptics.tick(context)
                        val hour = ((meals.maxOfOrNull { it.hour } ?: 4) + 4).let { if (it > 23) 8 else it }
                        Meals.set(context, meals + Meal(name = Meal.nameFor(hour), hour = hour, portions = meals.lastOrNull()?.portions ?: 1))
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(LucidePlus, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add meal")
                }
            }
        }
    }

    if (askingBattery) {
        AlertDialog(
            onDismissRequest = { askingBattery = false },
            title = { Text("Allow background feeding") },
            text = {
                Text(
                    "Android can hold back a scheduled meal while the phone sleeps if PetBro is battery optimized. " +
                        "It only wakes at the times you schedule."
                )
            },
            confirmButton = {
                Button(onClick = {
                    askingBattery = false
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = { askingBattery = false }) { Text("Not now") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

/** One meal: on or off, how it repeats, its time and portions, as PurgeBot's scheduled purge card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(meal: Meal, canRemove: Boolean, onChange: (Meal) -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var pickingTime by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingRemove by remember { mutableStateOf(false) }
    SectionCard(
        meal.name,
        LucideClock,
        if (meal.enabled) palette.warning else MaterialTheme.colorScheme.onSurfaceVariant,
        action = {
            IconButton(onClick = { Haptics.tick(context); renaming = true }, modifier = Modifier.size(36.dp)) {
                Icon(LucidePencil, contentDescription = "Rename meal", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Switch(checked = meal.enabled, onCheckedChange = { Haptics.tick(context); onChange(meal.copy(enabled = it)) })
        },
    ) {
        // Collapsed while off: just the time and amount.
        if (!meal.enabled) {
            Text(
                "${LocalTime.of(meal.hour, meal.minute).format(timeFormat)} · ${portionsLabel(meal.portions)} · off",
                style = MaterialTheme.typography.bodySmall,
                color = noteColor,
            )
        }
        Reveal(meal.enabled) {
            GapColumn(12.dp) {
                Segmented("Repeats", Repeat.entries, meal.repeat, {
                    when (it) { Repeat.DAILY -> "Daily"; Repeat.WEEKLY -> "Weekly"; Repeat.MONTHLY -> "Monthly" }
                }) {
                    // Weekly starts on today's weekday when no days are picked yet.
                    val days = if (it == Repeat.WEEKLY && meal.daysOfWeek.isEmpty()) setOf(LocalDate.now().dayOfWeek) else meal.daysOfWeek
                    onChange(meal.copy(repeat = it, daysOfWeek = days))
                }
                when (meal.repeat) {
                    Repeat.DAILY -> {}
                    Repeat.WEEKLY -> DayPicker(meal.daysOfWeek) { onChange(meal.copy(daysOfWeek = it)) }
                    Repeat.MONTHLY -> GapColumn(8.dp) {
                        MonthDayStepper(meal.dayOfMonth) { onChange(meal.copy(dayOfMonth = it)) }
                        if (meal.dayOfMonth > 28) {
                            Text("Short months use their last day", style = MaterialTheme.typography.bodySmall, color = noteColor)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = { pickingTime = true }) {
                        Text(LocalTime.of(meal.hour, meal.minute).format(timeFormat))
                    }
                }
                PortionStepper(meal.portions) { onChange(meal.copy(portions = it)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val next = meal.nextRun()
                    Text(
                        if (next == null) "Pick at least one day" else "Next at ${whenLabel(next)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (next == null) MaterialTheme.colorScheme.error else palette.secondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (canRemove) {
                        TextButton(onClick = { confirmingRemove = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(LucideTrash, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (pickingTime) {
        TimeDialog("Feed at", meal.hour, meal.minute, onDismiss = { pickingTime = false }) { h, m ->
            pickingTime = false
            onChange(meal.copy(hour = h, minute = m))
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(meal.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Meal name") },
            text = { AppField(name, { name = it.take(40) }, null, "Breakfast", autoFocus = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    renaming = false
                    onChange(meal.copy(name = name.trim()))
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            title = { Text("Remove ${meal.name}?") },
            confirmButton = { TextButton(onClick = { confirmingRemove = false; onRemove() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmingRemove = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

// ---------------------------------------------------------------- History

@Composable
private fun LogScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val entries by FeedLog.entries.collectAsStateWithLifecycle()
    var confirmingClear by remember { mutableStateOf(false) }
    ScreenColumn("History") {
        SectionCard(
            "Feeds",
            LucideHistory,
            palette.mauve,
            action = {
                if (entries.isNotEmpty()) TextButton(onClick = { confirmingClear = true }) { Text("Clear") }
            },
        ) {
            if (entries.isEmpty()) {
                Text("Feeds from Feed Now, the schedule, the tile and automation show up here.", style = MaterialTheme.typography.bodySmall, color = noteColor)
            }
            entries.forEachIndexed { i, entry ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                LogRow(entry)
            }
        }
    }
    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { confirmingClear = false },
            title = { Text("Clear history?") },
            text = { Text("This only clears PetBro's list. The Petlibro app keeps its own feeding log.") },
            confirmButton = { TextButton(onClick = { confirmingClear = false; FeedLog.clear(context) }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmingClear = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

@Composable
private fun LogRow(entry: FeedEntry) {
    val palette = LocalPalette.current
    val color = if (entry.ok) palette.success else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        ) {
            Icon(if (entry.ok) LucideCheck else LucideX, contentDescription = if (entry.ok) "Sent" else "Not confirmed", tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    portionsLabel(entry.portions),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(whenLabel(entry.time), style = MaterialTheme.typography.bodySmall, color = noteColor)
            }
            Text(
                entry.meal?.let { "${entry.source.label} · $it" } ?: entry.source.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!entry.ok) Text(entry.message, style = MaterialTheme.typography.bodySmall, color = noteColor)
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val hapticsOn by Prefs.hapticsOn.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header("Settings", modeToggle = false) },
    ) { padding ->
        GapColumn(
            16.dp,
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SectionCard("App theme", LucidePalette, palette.mauve) { AppearanceSection() }

            SectionCard("Feedback", LucideVibrate, palette.secondary) {
                SwitchRow("Haptic feedback", null, hapticsOn) { Prefs.setHaptics(context, it) }
            }

            val quick by Prefs.quickPortionsFlow.collectAsStateWithLifecycle()
            SectionCard("Quick feed", LucidePawPrint, palette.warning) {
                Text(
                    "What a tap on the paw button, the Quick Settings tile, the launcher shortcut and automation feed. Hold the paw button to change it there too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = noteColor,
                )
                PortionStepper(quick) { Prefs.setQuickPortions(context, it) }
            }

            WidgetLookCard()

            val automationOn by Prefs.automation.collectAsStateWithLifecycle()
            SectionCard("Automation", LucideZap, palette.secondary) {
                SwitchRow(
                    "Allow automation apps",
                    "Let Tasker, MacroDroid and other apps feed your pet.",
                    automationOn,
                ) { Prefs.setAutomation(context, it) }
                Reveal(automationOn) {
                    GapColumn(8.dp, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Send a broadcast intent with these (tap to copy):", style = MaterialTheme.typography.bodySmall, color = noteColor)
                        CopyField("Action", FeedNow.ACTION)
                        CopyField("Package", context.packageName)
                        CopyField("Class", FeedNowReceiver::class.java.name)
                        CopyField("Extra (optional, integer)", "${FeedNow.EXTRA_PORTIONS}: 1–${Petlibro.MAX_PORTIONS}")
                        Text(
                            "Without the extra it feeds the quick-feed amount. Feeds show up in History.",
                            style = MaterialTheme.typography.bodySmall,
                            color = noteColor,
                        )
                    }
                }
            }

            Text(
                "PetBro ${BuildConfigInfo.version(context)} · Uses Petlibro's US cloud. Not affiliated with Petlibro.",
                style = MaterialTheme.typography.bodySmall,
                color = noteColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private object BuildConfigInfo {
    fun version(context: android.content.Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
}
