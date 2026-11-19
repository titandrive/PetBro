package local.petlibrocompanion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// The building blocks of PurgeBot's screens, so PetBro looks and behaves the same.

/** A screen's title row: the logo, the title, the light/dark button and an optional control. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Header(title: String, modeToggle: Boolean = true, action: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val mode by ThemeStore.mode.collectAsStateWithLifecycle()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_logo),
            contentDescription = null,
            tint = palette.warning,
            modifier = Modifier.size(46.dp).padding(end = 10.dp, top = 6.dp, bottom = 6.dp),
        )
        val titleStyle = MaterialTheme.typography.headlineMedium
        BasicText(
            title,
            style = titleStyle.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = titleStyle.fontSize, stepSize = 1.sp),
            modifier = Modifier.weight(1f),
        )
        if (modeToggle) {
            val tint = if (mode == ThemeMode.SYSTEM) MaterialTheme.colorScheme.onSurfaceVariant else palette.mauve
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClickLabel = if (palette.isDark) "Switch to light theme" else "Switch to dark theme",
                        onLongClickLabel = "Follow system theme",
                        onLongClick = {
                            if (Prefs.haptics(context)) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            ThemeStore.set(context, ThemeMode.SYSTEM)
                            Toast.makeText(context, "Following system theme", Toast.LENGTH_SHORT).show()
                        },
                        onClick = {
                            Haptics.tick(context)
                            ThemeStore.set(context, if (palette.isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                    ),
            ) {
                Icon(if (palette.isDark) LucideSunMedium else LucideMoon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        action?.invoke()
    }
}

@Composable
private fun cardBorder(): BorderStroke? = LocalPalette.current.cardBorder?.let { BorderStroke(1.dp, it) }

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        border = cardBorder(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        GapColumn(8.dp, modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Takes all the room the action leaves, so it only truncates when it has to.
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) {
                Haptics.tick(context)
                onChange(it)
            }
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = noteColor) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun <T> Segmented(label: String, options: List<T>, selected: T, name: (T) -> String, onSelect: (T) -> Unit) {
    GapColumn(8.dp, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillSegmented(options, selected, name, onSelect = onSelect)
    }
}

/** A single choice as a recessed track with a solid pill that slides to the selected option. */
@Composable
fun <T> PillSegmented(
    options: List<T>,
    selected: T,
    name: (T) -> String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (T) -> Unit,
) {
    val context = LocalContext.current
    val index = options.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * index, label = "pill")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { option ->
                val on = option == selected
                val color by animateColorAsState(
                    if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "pillText",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(selected = on, role = Role.RadioButton) {
                            if (!on) Haptics.tick(context)
                            onSelect(option)
                        },
                ) {
                    Text(
                        name(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        modifier = Modifier.layoutId(Collapsible),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val appearing = remember { transition.currentState == EnterExitState.PreEnter }
        CompositionLocalProvider(LocalAppearing provides appearing) { content() }
    }
}

/** True inside a [Reveal] that's appearing because of a tap: its text field takes the cursor. */
val LocalAppearing = compositionLocalOf { false }

/** Marks a [Reveal] for [GapColumn]. */
private object Collapsible

/**
 * A column with [gap] between items, except the gap next to a [Reveal] is never taller than the
 * Reveal itself, so space shrinks with a collapsing section instead of snapping shut after it.
 */
@Composable
fun GapColumn(
    gap: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val loose = Constraints(maxWidth = constraints.maxWidth)
        val placeables = measurables.map { it.measure(loose) }
        val collapsible = measurables.map { it.layoutId == Collapsible }
        val ys = IntArray(placeables.size)
        var y = 0
        placeables.forEachIndexed { i, p ->
            if (i > 0) {
                var g = gapPx
                if (collapsible[i]) g = minOf(g, p.height)
                if (i == 1 && collapsible[0]) g = minOf(g, placeables[0].height)
                y += g
            }
            ys[i] = y
            y += p.height
        }
        val width = (placeables.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, y.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            placeables.forEachIndexed { i, p -> p.placeRelative(horizontalAlignment.align(p.width, width, layoutDirection), ys[i]) }
        }
    }
}

/** Seven round toggles, Monday first. */
@Composable
fun DayPicker(days: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    val locale = Locale.getDefault()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            val on = day in days
            DayCell(
                label = day.getDisplayName(TextStyle.NARROW, locale),
                description = day.getDisplayName(TextStyle.FULL, locale),
                selected = on,
                role = Role.Checkbox,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) { onChange(if (on) days - day else days + day) }
        }
    }
}

private val MONTH_DAYS = (1..31).toList() + Meal.LAST_DAY

fun monthDayLabel(day: Int) = if (day == Meal.LAST_DAY) "Last day" else ordinal(day)

fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}

/** "Day  ‹  3rd  ›": the buttons nudge (hold to repeat), tapping the day opens a month grid. */
@Composable
fun MonthDayStepper(day: Int, onChange: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val index = MONTH_DAYS.indexOf(day).coerceAtLeast(0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Day", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RepeatButton(LucideChevronLeft, "Earlier day", enabled = index > 0) {
            onChange(MONTH_DAYS[(index - 1).coerceAtLeast(0)])
        }
        TextButton(onClick = { picking = true }, modifier = Modifier.width(112.dp)) {
            Text(monthDayLabel(day), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        RepeatButton(LucideChevronRight, "Later day", enabled = index < MONTH_DAYS.lastIndex) {
            onChange(MONTH_DAYS[(index + 1).coerceAtMost(MONTH_DAYS.lastIndex)])
        }
    }
    if (picking) {
        MonthDayDialog(day, onDismiss = { picking = false }) {
            picking = false
            onChange(it)
        }
    }
}

/** "Portions  −  2  +", the same stepper as the month day's. */
@Composable
fun PortionStepper(portions: Int, label: String = "Portions", onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RepeatButton(LucideMinus, "Fewer portions", enabled = portions > 1) {
            onChange((portions - 1).coerceAtLeast(1))
        }
        Text(
            "$portions",
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(64.dp).semantics { contentDescription = portionsLabel(portions) },
        )
        RepeatButton(LucidePlus, "More portions", enabled = portions < Petlibro.MAX_PORTIONS) {
            onChange((portions + 1).coerceAtMost(Petlibro.MAX_PORTIONS))
        }
    }
}

/** A round tonal button that steps once on tap and keeps stepping while held. */
@Composable
fun RepeatButton(icon: ImageVector, description: String, enabled: Boolean, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
            .semantics { contentDescription = description }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    step()
                    val repeat = scope.launch {
                        delay(400)
                        while (true) {
                            if (Prefs.haptics(context)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step()
                            delay(70)
                        }
                    }
                    tryAwaitRelease()
                    repeat.cancel()
                })
            },
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha))
    }
}

@Composable
private fun MonthDayDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day of the month") },
        text = {
            GapColumn(6.dp) {
                (1..31).chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            DayCell("$d", ordinal(d), d == selected, Role.RadioButton, Modifier.weight(1f).aspectRatio(1f)) { onSelect(d) }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                DayCell(
                    "Last day", "Last day of the month", selected == Meal.LAST_DAY, Role.RadioButton,
                    Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp),
                ) { onSelect(Meal.LAST_DAY) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun DayCell(label: String, description: String, selected: Boolean, role: Role, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .selectable(selected = selected, role = role) {
                Haptics.tick(context)
                onClick()
            }
            .semantics { contentDescription = description },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The app's text field: a 44dp rounded field on a dark track, with its label above it. */
@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    autoFocus: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val focus = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
    }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(22.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        label?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                val outline = MaterialTheme.colorScheme.run {
                    when {
                        focused -> primary
                        surfaceContainerLowest == surfaceContainer -> outline
                        else -> Color.Transparent
                    }
                }
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, outline, shape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
        )
    }
}

/** Tapping empty space takes the cursor out of a text field (closing the keyboard). */
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focus = LocalFocusManager.current
    pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
}

/** A short warning or error in a tinted box with an icon, with an optional button. */
@Composable
fun Notice(text: String, color: Color, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.14f))
            .padding(start = 14.dp, end = if (action != null) 4.dp else 14.dp, top = 8.dp, bottom = 8.dp)
            .heightIn(min = 32.dp),
    ) {
        Icon(LucideTriangleAlert, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        action?.let { TextButton(onClick = onAction) { Text(it) } }
    }
}

@Composable
fun Pill(text: String, accent: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Theme picker (each theme has a light and dark version) and the light/dark/system choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection() {
    val context = LocalContext.current
    val mode by ThemeStore.mode.collectAsStateWithLifecycle()
    val family by ThemeStore.family.collectAsStateWithLifecycle()
    val amoled by ThemeStore.amoled.collectAsStateWithLifecycle()
    val ultra by ThemeStore.ultra.collectAsStateWithLifecycle()
    Text("Mode", style = MaterialTheme.typography.bodyLarge)
    PillSegmented(ThemeMode.entries, mode, { when (it) { ThemeMode.SYSTEM -> "System"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" } }) {
        ThemeStore.set(context, it)
    }
    Reveal(mode != ThemeMode.LIGHT) {
        Column {
            SwitchRow("AMOLED black", "Pure black backgrounds in dark mode", amoled) { ThemeStore.setAmoled(context, it) }
            Reveal(amoled) {
                Column(Modifier.padding(start = 16.dp)) {
                    SwitchRow("Ultra AMOLED", "Everything is pure black", ultra) { ThemeStore.setUltra(context, it) }
                }
            }
        }
    }
    Text("Theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    var picking by rememberSaveable { mutableStateOf(false) }
    ThemeOption(family) { picking = true }
    if (picking) {
        ModalBottomSheet(
            onDismissRequest = { picking = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                "Choose a theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                ThemeFamily.entries.sortedBy { it.label }.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            ThemeTile(f, selected = f == family, modifier = Modifier.weight(1f)) {
                                Haptics.tick(context)
                                if (f != family) ThemeStore.setFamily(context, f)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(family: ThemeFamily, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background)
            .border(2.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(40.dp)) {
            ThemeSwatch(family.light, Modifier.weight(1f).fillMaxHeight())
            ThemeSwatch(family.dark, Modifier.weight(1f).fillMaxHeight())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                family.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) Icon(LucideCheck, contentDescription = "Selected", tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ThemeOption(family: ThemeFamily, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = "Choose theme", onClick = onClick)
            .padding(10.dp),
    ) {
        ThemeSwatch(family.light)
        Spacer(Modifier.width(6.dp))
        ThemeSwatch(family.dark)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(family.label, style = MaterialTheme.typography.titleSmall)
            val small = MaterialTheme.typography.bodySmall
            Text(
                "${family.lightName} · ${family.darkName}",
                style = small,
                color = noteColor,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = small.fontSize),
            )
        }
        Icon(LucideChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeSwatch(p: Palette, modifier: Modifier = Modifier.size(width = 46.dp, height = 40.dp)) {
    GapColumn(
        4.dp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.mantle)
            .border(1.dp, p.surface1, RoundedCornerShape(10.dp))
            .padding(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(p.text.copy(alpha = 0.7f)))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(p.mauve, p.secondary, p.tertiary).forEach { Box(Modifier.size(9.dp).clip(CircleShape).background(it)) }
        }
    }
}
