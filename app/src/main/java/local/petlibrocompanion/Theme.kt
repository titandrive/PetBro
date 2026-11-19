package local.petlibrocompanion

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A theme's colors, named after Catppuccin's roles (https://catppuccin.com/palette): [mauve] is
 * the main accent, [red] destructive, [base]/[mantle]/[crust] the backgrounds from card to page
 * to deepest, and [text] down to [overlay0] the foreground shades. Other themes fill the same roles.
 */
data class Palette(
    val isDark: Boolean,
    val rosewater: Color, val flamingo: Color, val pink: Color, val mauve: Color,
    val red: Color, val maroon: Color, val peach: Color, val yellow: Color,
    val green: Color, val teal: Color, val sky: Color, val sapphire: Color,
    val blue: Color, val lavender: Color,
    val text: Color, val subtext1: Color, val subtext0: Color,
    val overlay2: Color, val overlay1: Color, val overlay0: Color,
    val surface2: Color, val surface1: Color, val surface0: Color,
    val base: Color, val mantle: Color, val crust: Color,
    /** Draw faint CRT scanlines over the whole app (Nuked's dark look). */
    val scanlines: Boolean = false,
    /**
     * Roles picked from each theme's own colors: [mauve] is the primary accent, [secondary] and
     * [tertiary] the others the theme is known for, [success] for finished work, [warning] for
     * caution, and [red] for destructive actions and errors.
     */
    val secondary: Color = blue,
    val tertiary: Color = pink,
    val success: Color = green,
    val warning: Color = peach,
    /** Start's color for a live purge: [red], except where a theme's red slot isn't red. */
    val live: Color = red,
    /** The outline cards get on AMOLED black, where they'd otherwise vanish into the page: the theme's accent. */
    val amoledBorder: Color = mauve,
    /** Set by [amoled]: a hairline around cards; null draws none. */
    val cardBorder: Color? = null,
    /** Text and icons on each accent, where a theme sets its own; null takes the page's darkest (dark) or the card (light). */
    val onPrimary: Color? = null,
    val onSecondary: Color? = null,
    val onTertiary: Color? = null,
    val onError: Color? = null,
) {
    /** Colors that tell accounts apart, all from the theme's own roles. */
    val accountColors: List<Color> get() = listOf(mauve, secondary, tertiary, success, warning).distinct()
}

// Catppuccin Latte.
val Latte = Palette(
    isDark = false,
    rosewater = Color(0xFFDC8A78), flamingo = Color(0xFFDD7878), pink = Color(0xFFEA76CB), mauve = Color(0xFF8839EF),
    red = Color(0xFFD20F39), maroon = Color(0xFFE64553), peach = Color(0xFFFE640B), yellow = Color(0xFFDF8E1D),
    green = Color(0xFF40A02B), teal = Color(0xFF179299), sky = Color(0xFF04A5E5), sapphire = Color(0xFF209FB5),
    blue = Color(0xFF1E66F5), lavender = Color(0xFF7287FD), text = Color(0xFF4C4F69), subtext1 = Color(0xFF5C5F77),
    subtext0 = Color(0xFF6C6F85), overlay2 = Color(0xFF7C7F93), overlay1 = Color(0xFF8C8FA1), overlay0 = Color(0xFF9CA0B0),
    surface2 = Color(0xFFACB0BE), surface1 = Color(0xFFBCC0CC), surface0 = Color(0xFFCCD0DA), base = Color(0xFFEFF1F5),
    mantle = Color(0xFFE6E9EF), crust = Color(0xFFDCE0E8),
    secondary = Color(0xFFEA76CB), tertiary = Color(0xFF40A02B), success = Color(0xFF40A02B), warning = Color(0xFFFE640B),
)

// Catppuccin Macchiato.
val Macchiato = Palette(
    isDark = true,
    rosewater = Color(0xFFF4DBD6), flamingo = Color(0xFFF0C6C6), pink = Color(0xFFF5BDE6), mauve = Color(0xFFC6A0F6),
    red = Color(0xFFED8796), maroon = Color(0xFFEE99A0), peach = Color(0xFFF5A97F), yellow = Color(0xFFEED49F),
    green = Color(0xFFA6DA95), teal = Color(0xFF8BD5CA), sky = Color(0xFF91D7E3), sapphire = Color(0xFF7DC4E4),
    blue = Color(0xFF8AADF4), lavender = Color(0xFFB7BDF8), text = Color(0xFFCAD3F5), subtext1 = Color(0xFFB8C0E0),
    subtext0 = Color(0xFFA5ADCB), overlay2 = Color(0xFF939AB7), overlay1 = Color(0xFF8087A2), overlay0 = Color(0xFF6E738D),
    surface2 = Color(0xFF5B6078), surface1 = Color(0xFF494D64), surface0 = Color(0xFF363A4F), base = Color(0xFF24273A),
    mantle = Color(0xFF1E2030), crust = Color(0xFF181926),
    secondary = Color(0xFFF5BDE6), tertiary = Color(0xFFA6DA95), success = Color(0xFFA6DA95), warning = Color(0xFFF5A97F),
)

// Dracula (https://draculatheme.com), as in Paid to Poop: purple main, cards on the current-line grey.
val Dracula = Palette(
    isDark = true,
    rosewater = Color(0xFFFFCAE8), flamingo = Color(0xFFFF92DF), pink = Color(0xFFFF79C6), mauve = Color(0xFFBD93F9),
    red = Color(0xFFFF5555), maroon = Color(0xFFFF6E6E), peach = Color(0xFFFFB86C), yellow = Color(0xFFF1FA8C),
    green = Color(0xFF50FA7B), teal = Color(0xFF8BE9FD), sky = Color(0xFF9AEDFE), sapphire = Color(0xFF7DD3FC),
    blue = Color(0xFFA4B0FF), lavender = Color(0xFFD6BCFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFF858EB8), overlay1 = Color(0xFF7280B0), overlay0 = Color(0xFF6272A4),
    surface2 = Color(0xFF666A86), surface1 = Color(0xFF5B5F78), surface0 = Color(0xFF50536A), base = Color(0xFF44475A),
    mantle = Color(0xFF282A36), crust = Color(0xFF21222C),
    onPrimary = Color.White,
    // Pink and green beside the purple: the code-editor Dracula look.
    secondary = Color(0xFFFF79C6), tertiary = Color(0xFF50FA7B), success = Color(0xFF50FA7B), warning = Color(0xFFFFB86C),
)

// Alucard, Dracula's official light theme.
val Alucard = Palette(
    isDark = false,
    rosewater = Color(0xFF8D5A6E), flamingo = Color(0xFFB0306A), pink = Color(0xFFA3144D), mauve = Color(0xFF644AC9),
    red = Color(0xFFCB3A2A), maroon = Color(0xFFB5452F), peach = Color(0xFFA34D14), yellow = Color(0xFF846E15),
    green = Color(0xFF14710A), teal = Color(0xFF036A96), sky = Color(0xFF0A7EA4), sapphire = Color(0xFF1B6A8F),
    blue = Color(0xFF3B4BB5), lavender = Color(0xFF7860C9), text = Color(0xFF1F1F1F), subtext1 = Color(0xFF333333),
    subtext0 = Color(0xFF4A4A4A), overlay2 = Color(0xFF635D97), overlay1 = Color(0xFF7A75A0), overlay0 = Color(0xFF8F8AAB),
    surface2 = Color(0xFFBCBAC8), surface1 = Color(0xFFCFCFDE), surface0 = Color(0xFFDEDACA), base = Color(0xFFFFFBEB),
    mantle = Color(0xFFF5F1E1), crust = Color(0xFFE9E5D5),
    secondary = Color(0xFFA3144D), tertiary = Color(0xFF036A96), success = Color(0xFF14710A), warning = Color(0xFFA34D14),
)

// Synthwave, dark (Midnight Drive), as in Paid to Poop: fuchsia and cyan on deep indigo.
val Synthwave = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFFE879F9),
    red = Color(0xFFF87171), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFFA79BC9), overlay1 = Color(0xFF9384BC), overlay0 = Color(0xFF806EB0),
    surface2 = Color(0xFF6651A1), surface1 = Color(0xFF563F97), surface0 = Color(0xFF4B3290), base = Color(0xFF3B2086),
    mantle = Color(0xFF1A103D), crust = Color(0xFF120B2B),
    onPrimary = Color.White,
    secondary = Color(0xFF22D3EE), tertiary = Color(0xFF4ADE80), success = Color(0xFF4ADE80), warning = Color(0xFFFBBF24),
)

// Synthwave, light (Sunset Drive), as in Paid to Poop: the dark version turned around, on pale violet.
val SynthwaveLight = Palette(
    isDark = false,
    rosewater = Color(0xFFBE123C), flamingo = Color(0xFFE11D48), pink = Color(0xFFDB2777), mauve = Color(0xFFC026D3),
    red = Color(0xFFDC2626), maroon = Color(0xFFE11D48), peach = Color(0xFFEA580C), yellow = Color(0xFFCA8A04),
    green = Color(0xFF16A34A), teal = Color(0xFF0D9488), sky = Color(0xFF0284C7), sapphire = Color(0xFF0891B2),
    blue = Color(0xFF2563EB), lavender = Color(0xFF7C3AED), text = Color(0xFF1A103D), subtext1 = Color(0xFF3A2D64),
    subtext0 = Color(0xFF5B4A8B), overlay2 = Color(0xFF7F7894), overlay1 = Color(0xFF9690A8), overlay0 = Color(0xFFACA7BB),
    surface2 = Color(0xFFCAC5D4), surface1 = Color(0xFFDCD8E4), surface0 = Color(0xFFE9E5EF), base = Color(0xFFFBF8FF),
    mantle = Color(0xFFECE6F9), crust = Color(0xFFDED8EA),
    onPrimary = Color.White,
    secondary = Color(0xFF0891B2), tertiary = Color(0xFF16A34A), success = Color(0xFF16A34A), warning = Color(0xFFD97706),
)

// Nuked, dark (Terminal), as in Paid to Poop: phosphor-green CRT, with scanlines.
val NukedDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFF1AFF80),
    red = Color(0xFFFF6B6B), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4CFF7A), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFF1AFF80), subtext1 = Color(0xFF15DC70),
    subtext0 = Color(0xFF10B860), overlay2 = Color(0xFF169F51), overlay1 = Color(0xFF168A47), overlay0 = Color(0xFF1AFF80),
    surface2 = Color(0xFF14592F), surface1 = Color(0xFF0D4020), surface0 = Color(0xFF133B20), base = Color(0xFF122A18),
    mantle = Color(0xFF0A1A0F), crust = Color(0xFF07120A),
    onPrimary = Color(0xFF0A1A0F),
    scanlines = true,
    secondary = Color(0xFF14CC66), tertiary = Color(0xFF1AFF80), success = Color(0xFF1AFF80), warning = Color(0xFFFFB641),
)

// Nuked, light (Atomic Age), as in Paid to Poop: jumpsuit blue with yellow trim, cola red, on steel greys.
val NukedLight = Palette(
    isDark = false,
    rosewater = Color(0xFFBE123C), flamingo = Color(0xFFE11D48), pink = Color(0xFFDB2777), mauve = Color(0xFF1E4FA0),
    red = Color(0xFFC8102E), maroon = Color(0xFFE11D48), peach = Color(0xFFEA580C), yellow = Color(0xFFCA8A04),
    green = Color(0xFF16A34A), teal = Color(0xFF0D9488), sky = Color(0xFF0284C7), sapphire = Color(0xFF0891B2),
    blue = Color(0xFF2563EB), lavender = Color(0xFF7C3AED), text = Color(0xFF1A2238), subtext1 = Color(0xFF323B50),
    subtext0 = Color(0xFF4A5468), overlay2 = Color(0xFF7D828F), overlay1 = Color(0xFF9397A2), overlay0 = Color(0xFFA9ACB5),
    surface2 = Color(0xFFC6C8CF), surface1 = Color(0xFFD7D9DE), surface0 = Color(0xFFE4E6EA), base = Color(0xFFF6F7F9),
    mantle = Color(0xFFE2E6EC), crust = Color(0xFFD4D8DE),
    onPrimary = Color(0xFFFFD23F),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onError = Color.White,
    secondary = Color(0xFFC79200), tertiary = Color(0xFFC8102E), success = Color(0xFF16A34A), warning = Color(0xFFD97706),
)

// Midnight (made for this app): red on near-black; Stop uses a hot orange-red so it stays distinct.
val MidnightDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFFB3B3), flamingo = Color(0xFFFF7A7A), pink = Color(0xFFFF5C8A), mauve = Color(0xFFE5383B),
    red = Color(0xFFFF7B3A), maroon = Color(0xFFFF9A5C), peach = Color(0xFFFF9F43), yellow = Color(0xFFF4C95D),
    green = Color(0xFF6FD38F), teal = Color(0xFF5CCFC0), sky = Color(0xFF7AB8E8), sapphire = Color(0xFF6AA8D8),
    blue = Color(0xFF8AA6FF), lavender = Color(0xFFC49BFF), text = Color(0xFFE8E8EA), subtext1 = Color(0xFFC2C2C6),
    subtext0 = Color(0xFFA3A3A8), overlay2 = Color(0xFF85858B), overlay1 = Color(0xFF6E6E74), overlay0 = Color(0xFF57575D),
    surface2 = Color(0xFF38383C), surface1 = Color(0xFF2A2A2D), surface0 = Color(0xFF1F1F21), base = Color(0xFF161617),
    mantle = Color(0xFF0D0D0E), crust = Color(0xFF070707),
    secondary = Color(0xFFFF5C8A), tertiary = Color(0xFFC2C2C6), success = Color(0xFFFFB3B3), warning = Color(0xFFFF9A5C),
    // Its red slot is orange, to stand apart from the crimson accent; a live purge should still read red.
    live = Color(0xFFE5383B),
)

// Midnight, light: deep red on greys, no other hues.
val MidnightLight = Palette(
    isDark = false,
    rosewater = Color(0xFFB35A5A), flamingo = Color(0xFFD64550), pink = Color(0xFFC9184A), mauve = Color(0xFFC1121F),
    red = Color(0xFFD9480F), maroon = Color(0xFF9B1B1B), peach = Color(0xFFC2410C), yellow = Color(0xFFA4452C),
    green = Color(0xFF5F5F5F), teal = Color(0xFF6E6E6E), sky = Color(0xFF7A7A7A), sapphire = Color(0xFF555555),
    blue = Color(0xFF616161), lavender = Color(0xFF8E4B57), text = Color(0xFF1C1C1E), subtext1 = Color(0xFF3A3A3C),
    subtext0 = Color(0xFF4C4C4F), overlay2 = Color(0xFF636366), overlay1 = Color(0xFF78787C), overlay0 = Color(0xFF8E8E93),
    surface2 = Color(0xFFB8B8BC), surface1 = Color(0xFFCDCDD1), surface0 = Color(0xFFDEDEE2), base = Color(0xFFF4F4F5),
    mantle = Color(0xFFE7E7E9), crust = Color(0xFFD8D8DC),
    secondary = Color(0xFFC9184A), tertiary = Color(0xFF555555), success = Color(0xFFB35A5A), warning = Color(0xFFC2410C),
    live = Color(0xFFC1121F),
)

// Cyberpink, dark (Night City), as in Paid to Poop: neon magenta, cyan and purple on near-black; Stop in cyan.
val CyberpinkDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFFFF00FF),
    red = Color(0xFF00F0FF), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFFDD00),
    green = Color(0xFF00FF9F), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFF9F9F9F), overlay1 = Color(0xFF8A8A8A), overlay0 = Color(0xFF757575),
    surface2 = Color(0xFF595959), surface1 = Color(0xFF484848), surface0 = Color(0xFF3B3B3B), base = Color(0xFF2A2A2A),
    mantle = Color(0xFF0D0D0D), crust = Color(0xFF090909),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onError = Color.Black,
    live = Color(0xFFFF3860),
    secondary = Color(0xFF00F0FF), tertiary = Color(0xFF9600FF), success = Color(0xFF4ADE80), warning = Color(0xFFFFDD00),
)

// Cyberpink, light: Yarnl's light Cyberpunk, fuchsia and cyan on white.
val CyberpinkLight = Palette(
    isDark = false,
    rosewater = Color(0xFFC026D3), flamingo = Color(0xFFDB2777), pink = Color(0xFFDB2777), mauve = Color(0xFFD946EF),
    red = Color(0xFFEF4444), maroon = Color(0xFFDC2626), peach = Color(0xFFEA580C), yellow = Color(0xFFEAB308),
    green = Color(0xFF10B981), teal = Color(0xFF0D9488), sky = Color(0xFF0891B2), sapphire = Color(0xFF0E7490),
    blue = Color(0xFF2563EB), lavender = Color(0xFF9333EA), text = Color(0xFF171717), subtext1 = Color(0xFF404040),
    subtext0 = Color(0xFF0891B2), overlay2 = Color(0xFF737373), overlay1 = Color(0xFF8A8A8A), overlay0 = Color(0xFFA3A3A3),
    surface2 = Color(0xFFD4D4D4), surface1 = Color(0xFFE5E5E5), surface0 = Color(0xFFF5F5F5), base = Color(0xFFFFFFFF),
    mantle = Color(0xFFFAFAFA), crust = Color(0xFFF0F0F0),
    secondary = Color(0xFF06B6D4), tertiary = Color(0xFFEAB308), success = Color(0xFF10B981), warning = Color(0xFFEAB308),
)

// Gruvbox dark (https://github.com/morhetz/gruvbox); orange is the main accent.
val GruvboxDark = Palette(
    isDark = true,
    rosewater = Color(0xFFE9B99F), flamingo = Color(0xFFB16286), pink = Color(0xFFD3869B), mauve = Color(0xFFFE8019),
    red = Color(0xFFFB4934), maroon = Color(0xFFCC241D), peach = Color(0xFFD65D0E), yellow = Color(0xFFFABD2F),
    green = Color(0xFFB8BB26), teal = Color(0xFF8EC07C), sky = Color(0xFF83A598), sapphire = Color(0xFF458588),
    blue = Color(0xFF83A598), lavender = Color(0xFFD3869B), text = Color(0xFFEBDBB2), subtext1 = Color(0xFFD5C4A1),
    subtext0 = Color(0xFFBDAE93), overlay2 = Color(0xFFA89984), overlay1 = Color(0xFF928374), overlay0 = Color(0xFF7C6F64),
    surface2 = Color(0xFF665C54), surface1 = Color(0xFF504945), surface0 = Color(0xFF3C3836), base = Color(0xFF282828),
    mantle = Color(0xFF1D2021), crust = Color(0xFF141617),
    secondary = Color(0xFF83A598), tertiary = Color(0xFF8EC07C), success = Color(0xFFB8BB26), warning = Color(0xFFFABD2F),
)

// Gruvbox light.
val GruvboxLight = Palette(
    isDark = false,
    rosewater = Color(0xFF9D5A3A), flamingo = Color(0xFFB16286), pink = Color(0xFF8F3F71), mauve = Color(0xFFAF3A03),
    red = Color(0xFF9D0006), maroon = Color(0xFFCC241D), peach = Color(0xFFD65D0E), yellow = Color(0xFFB57614),
    green = Color(0xFF79740E), teal = Color(0xFF427B58), sky = Color(0xFF076678), sapphire = Color(0xFF076678),
    blue = Color(0xFF076678), lavender = Color(0xFF8F3F71), text = Color(0xFF3C3836), subtext1 = Color(0xFF504945),
    subtext0 = Color(0xFF665C54), overlay2 = Color(0xFF7C6F64), overlay1 = Color(0xFF928374), overlay0 = Color(0xFFA89984),
    surface2 = Color(0xFFBDAE93), surface1 = Color(0xFFD5C4A1), surface0 = Color(0xFFEBDBB2), base = Color(0xFFF9F5D7),
    mantle = Color(0xFFF2E5BC), crust = Color(0xFFEBDBB2),
    secondary = Color(0xFF076678), tertiary = Color(0xFF427B58), success = Color(0xFF79740E), warning = Color(0xFFB57614),
)

// Nord Polar Night (https://www.nordtheme.com); frost cyan is the main accent.
val NordDark = Palette(
    isDark = true,
    rosewater = Color(0xFFD8B4C8), flamingo = Color(0xFFC895BF), pink = Color(0xFFB48EAD), mauve = Color(0xFF88C0D0),
    red = Color(0xFFBF616A), maroon = Color(0xFFC5727A), peach = Color(0xFFD08770), yellow = Color(0xFFEBCB8B),
    green = Color(0xFFA3BE8C), teal = Color(0xFF8FBCBB), sky = Color(0xFF88C0D0), sapphire = Color(0xFF81A1C1),
    blue = Color(0xFF5E81AC), lavender = Color(0xFFB48EAD), text = Color(0xFFECEFF4), subtext1 = Color(0xFFD8DEE9),
    subtext0 = Color(0xFFB8C0CE), overlay2 = Color(0xFF96A0B5), overlay1 = Color(0xFF7B88A1), overlay0 = Color(0xFF616E88),
    surface2 = Color(0xFF5A657B), surface1 = Color(0xFF4C566A), surface0 = Color(0xFF434C5E), base = Color(0xFF3B4252),
    mantle = Color(0xFF2E3440), crust = Color(0xFF252A33),
    secondary = Color(0xFF81A1C1), tertiary = Color(0xFFB48EAD), success = Color(0xFFA3BE8C), warning = Color(0xFFEBCB8B),
)

// Nord Snow Storm; accents darkened slightly for a light page.
val NordLight = Palette(
    isDark = false,
    rosewater = Color(0xFF9A7B8A), flamingo = Color(0xFFA46B9A), pink = Color(0xFF8F6A8A), mauve = Color(0xFF5E81AC),
    red = Color(0xFFBF616A), maroon = Color(0xFFA5545C), peach = Color(0xFFB86E55), yellow = Color(0xFFA8873A),
    green = Color(0xFF6C8A54), teal = Color(0xFF5B8F8E), sky = Color(0xFF4C8EA0), sapphire = Color(0xFF5E81AC),
    blue = Color(0xFF4C6E9E), lavender = Color(0xFF7F6A9E), text = Color(0xFF2E3440), subtext1 = Color(0xFF434C5E),
    subtext0 = Color(0xFF4C566A), overlay2 = Color(0xFF616E88), overlay1 = Color(0xFF707C92), overlay0 = Color(0xFF8A95A8),
    surface2 = Color(0xFFB6BFCD), surface1 = Color(0xFFCCD3DF), surface0 = Color(0xFFDDE3EC), base = Color(0xFFECEFF4),
    mantle = Color(0xFFE5E9F0), crust = Color(0xFFD8DEE9),
    secondary = Color(0xFF5E81AC), tertiary = Color(0xFF7F6A9E), success = Color(0xFF6C8A54), warning = Color(0xFFA8873A),
)

// Gamer, dark (All-Nighter), as in Paid to Poop: neon green on black.
val GamerDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFF00FF00),
    red = Color(0xFFEF4444), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFF989898), overlay1 = Color(0xFF818181), overlay0 = Color(0xFF6A6A6A),
    surface2 = Color(0xFF4C4C4C), surface1 = Color(0xFF3A3A3A), surface0 = Color(0xFF2C2C2C), base = Color(0xFF1A1A1A),
    mantle = Color(0xFF0A0A0A), crust = Color(0xFF070707),
    onPrimary = Color.Black,
    secondary = Color(0xFF22C55E), tertiary = Color(0xFF00FF00), success = Color(0xFF00FF00), warning = Color(0xFFFBBF24),
)

// Gamer, light (LAN Party), as in Paid to Poop: the same neon green on mid greys, since it can't be read on white.
val GamerLight = Palette(
    isDark = false,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFF00FF00),
    red = Color(0xFFFF6B6B), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFEAEAEA),
    subtext0 = Color(0xFFD6D6D6), overlay2 = Color(0xFFB8B8B8), overlay1 = Color(0xFFA8A8A8), overlay0 = Color(0xFF989898),
    surface2 = Color(0xFF848484), surface1 = Color(0xFF777777), surface0 = Color(0xFF6E6E6E), base = Color(0xFF616161),
    mantle = Color(0xFF4F4F4F), crust = Color(0xFF4A4A4A),
    onPrimary = Color.Black,
    secondary = Color(0xFF22C55E), tertiary = Color(0xFF00FF00), success = Color(0xFF00FF00), warning = Color(0xFFFBBF24),
)

// Halloween, dark (Witching Hour), as in Paid to Poop: pumpkin orange and neon green on black.
val HalloweenDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFFF97316),
    red = Color(0xFFEF4444), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFF09FF00), subtext1 = Color(0xFF09FF00),
    subtext0 = Color(0xFF09FF00), overlay2 = Color(0xFF12980A), overlay1 = Color(0xFF13800D), overlay0 = Color(0xFF156A0F),
    surface2 = Color(0xFF184C12), surface1 = Color(0xFF193914), surface0 = Color(0xFF1A2B15), base = Color(0xFF1C1917),
    mantle = Color(0xFF0A0A0A), crust = Color(0xFF070707),
    onPrimary = Color.Black,
    secondary = Color(0xFF22C55E), tertiary = Color(0xFF22C55E), success = Color(0xFF22C55E), warning = Color(0xFFFBBF24),
)

// Halloween, light (Pumpkin Patch), as in Paid to Poop: the same orange and green on warm mid greys.
val HalloweenLight = Palette(
    isDark = false,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFFF97316),
    red = Color(0xFFEF4444), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFF09FF00), subtext1 = Color(0xFF09FF00),
    subtext0 = Color(0xFF09FF00), overlay2 = Color(0xFF2EB325), overlay1 = Color(0xFF37A22D), overlay0 = Color(0xFF3F9135),
    surface2 = Color(0xFF4A7B40), surface1 = Color(0xFF506E47), surface0 = Color(0xFF55644B), base = Color(0xFF5C5652),
    mantle = Color(0xFF4A4542), crust = Color(0xFF46413E),
    onPrimary = Color.Black,
    secondary = Color(0xFF22C55E), tertiary = Color(0xFF22C55E), success = Color(0xFF22C55E), warning = Color(0xFFFBBF24),
)

// Ocean, dark (Deep Sea), as in Paid to Poop.
val OceanDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFF38BDF8),
    red = Color(0xFFF87171), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFF96AFC0), overlay1 = Color(0xFF7F9EB2), overlay0 = Color(0xFF688CA4),
    surface2 = Color(0xFF497592), surface1 = Color(0xFF376787), surface0 = Color(0xFF295C7E), base = Color(0xFF164E73),
    mantle = Color(0xFF0C1929), crust = Color(0xFF08121D),
    onPrimary = Color.White,
    secondary = Color(0xFF22D3EE), tertiary = Color(0xFF34D399), success = Color(0xFF34D399), warning = Color(0xFFFBBF24),
)

// Ocean, light (Shallows), as in Paid to Poop.
val OceanLight = Palette(
    isDark = false,
    rosewater = Color(0xFFBE123C), flamingo = Color(0xFFE11D48), pink = Color(0xFFDB2777), mauve = Color(0xFF0EA5E9),
    red = Color(0xFFEF4444), maroon = Color(0xFFE11D48), peach = Color(0xFFEA580C), yellow = Color(0xFFCA8A04),
    green = Color(0xFF16A34A), teal = Color(0xFF0D9488), sky = Color(0xFF0284C7), sapphire = Color(0xFF0891B2),
    blue = Color(0xFF2563EB), lavender = Color(0xFF7C3AED), text = Color(0xFF0C4A6E), subtext1 = Color(0xFF385F7C),
    subtext0 = Color(0xFF64748B), overlay2 = Color(0xFF6B96AF), overlay1 = Color(0xFF81A6BD), overlay0 = Color(0xFF96B7CC),
    surface2 = Color(0xFFB1CDDE), surface1 = Color(0xFFC2DAEA), surface0 = Color(0xFFCFE5F2), base = Color(0xFFE0F2FE),
    mantle = Color(0xFFF0F9FF), crust = Color(0xFFE2EAF0),
    onPrimary = Color.White,
    secondary = Color(0xFF06B6D4), tertiary = Color(0xFF10B981), success = Color(0xFF10B981), warning = Color(0xFFD97706),
)

// Plum, dark (Plum Wine), as in Paid to Poop.
val PlumDark = Palette(
    isDark = true,
    rosewater = Color(0xFFFDA4AF), flamingo = Color(0xFFFB7185), pink = Color(0xFFF472B6), mauve = Color(0xFFC084FC),
    red = Color(0xFFF87171), maroon = Color(0xFFFB7185), peach = Color(0xFFFB923C), yellow = Color(0xFFFBBF24),
    green = Color(0xFF4ADE80), teal = Color(0xFF2DD4BF), sky = Color(0xFF38BDF8), sapphire = Color(0xFF22D3EE),
    blue = Color(0xFF60A5FA), lavender = Color(0xFFA78BFA), text = Color(0xFFFFFFFF), subtext1 = Color(0xFFFFFFFF),
    subtext0 = Color(0xFFFFFFFF), overlay2 = Color(0xFFA796B0), overlay1 = Color(0xFF937E9F), overlay0 = Color(0xFF80678D),
    surface2 = Color(0xFF664876), surface1 = Color(0xFF563668), surface0 = Color(0xFF4B285E), base = Color(0xFF3B1550),
    mantle = Color(0xFF1A0A20), crust = Color(0xFF120716),
    onPrimary = Color.White,
    secondary = Color(0xFFD8B4FE), tertiary = Color(0xFFE879F9), success = Color(0xFF4ADE80), warning = Color(0xFFFBBF24),
)

// Plum, light (Plum Blossom), as in Paid to Poop.
val PlumLight = Palette(
    isDark = false,
    rosewater = Color(0xFFBE123C), flamingo = Color(0xFFE11D48), pink = Color(0xFFDB2777), mauve = Color(0xFF9333EA),
    red = Color(0xFFDC2626), maroon = Color(0xFFE11D48), peach = Color(0xFFEA580C), yellow = Color(0xFFCA8A04),
    green = Color(0xFF16A34A), teal = Color(0xFF0D9488), sky = Color(0xFF0284C7), sapphire = Color(0xFF0891B2),
    blue = Color(0xFF2563EB), lavender = Color(0xFF7C3AED), text = Color(0xFF581C87), subtext1 = Color(0xFF6B1FAA),
    subtext0 = Color(0xFF7E22CE), overlay2 = Color(0xFF9E78BD), overlay1 = Color(0xFFAD8CC9), overlay0 = Color(0xFFBDA1D5),
    surface2 = Color(0xFFD1BBE5), surface1 = Color(0xFFDDCBEE), surface0 = Color(0xFFE7D8F5), base = Color(0xFFF3E8FF),
    mantle = Color(0xFFFAF5FF), crust = Color(0xFFEBE6F0),
    onPrimary = Color.White,
    secondary = Color(0xFFA855F7), tertiary = Color(0xFFC026D3), success = Color(0xFF16A34A), warning = Color(0xFFD97706),
)

val LocalPalette = staticCompositionLocalOf { Macchiato }

/**
 * Explainer and note text: the theme's softer body text, apart from labels in onSurfaceVariant
 * (which some themes tint with their accent), so a description never reads as a heading.
 */
val noteColor: Color
    @Composable get() = LocalPalette.current.subtext1

/** A theme with its light and dark versions. */
enum class ThemeFamily(val label: String, val lightName: String, val darkName: String, val light: Palette, val dark: Palette) {
    CATPPUCCIN("Catppuccin", "Latte", "Macchiato", Latte, Macchiato),
    CYBERPINK("Cyberpink", "Corpo Plaza", "Night City", CyberpinkLight, CyberpinkDark),
    DRACULA("Dracula", "Alucard", "Dracula", Alucard, Dracula),
    GAMER("Gamer", "LAN Party", "All-Nighter", GamerLight, GamerDark),
    GRUVBOX("Gruvbox", "Sun-Faded", "Retro Groove", GruvboxLight, GruvboxDark),
    HALLOWEEN("Halloween", "Pumpkin Patch", "Witching Hour", HalloweenLight, HalloweenDark),
    MIDNIGHT("Midnight", "Crimson Dawn", "Blood Moon", MidnightLight, MidnightDark),
    NORD("Nord", "Snow Storm", "Polar Night", NordLight, NordDark),
    NUKED("Nuked", "Atomic Age", "Terminal", NukedLight, NukedDark),
    OCEAN("Ocean", "Shallows", "Deep Sea", OceanLight, OceanDark),
    PLUM("Plum", "Plum Blossom", "Plum Wine", PlumLight, PlumDark),
    SYNTHWAVE("Synthwave", "Sunset Drive", "Midnight Drive", SynthwaveLight, Synthwave),
}

/** The theme saved under [name], including Midnight's old name, Crimson. */
fun themeFamilyNamed(name: String?): ThemeFamily? =
    ThemeFamily.entries.firstOrNull { it.name == (if (name == "CRIMSON") "MIDNIGHT" else name) }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The chosen theme and light/dark mode, loaded before any screen draws. */
object ThemeStore {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode
    private val _family = MutableStateFlow(ThemeFamily.CATPPUCCIN)
    val family: StateFlow<ThemeFamily> = _family

    fun load(context: Context) {
        val prefs = prefs(context)
        // Before themes had families the mode was saved by flavor name.
        _mode.value = when (val name = prefs.getString("theme", null)) {
            "LATTE" -> ThemeMode.LIGHT
            "MACCHIATO" -> ThemeMode.DARK
            else -> ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
        }
        _family.value = themeFamilyNamed(prefs.getString("family", null)) ?: ThemeFamily.CATPPUCCIN
        _amoled.value = prefs.getBoolean("amoled", false)
        _ultra.value = prefs.getBoolean("amoled_ultra", false)
    }

    fun set(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString("theme", mode.name).apply()
        _mode.value = mode
    }

    private val _amoled = MutableStateFlow(false)

    /** Pure black backgrounds for dark themes, for AMOLED screens. */
    val amoled: StateFlow<Boolean> = _amoled

    fun setAmoled(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("amoled", on).apply()
        _amoled.value = on
    }

    private val _ultra = MutableStateFlow(false)

    /** With [amoled], cards go pure black too. */
    val ultra: StateFlow<Boolean> = _ultra

    fun setUltra(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("amoled_ultra", on).apply()
        _ultra.value = on
    }

    fun setFamily(context: Context, family: ThemeFamily) {
        prefs(context).edit().putString("family", family.name).apply()
        _family.value = family
    }

    private fun prefs(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
}

@Composable
fun paletteFor(family: ThemeFamily, mode: ThemeMode, amoled: Boolean = false, ultra: Boolean = false): Palette {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    return if (!dark) family.light else if (amoled) family.dark.amoled(ultra) else family.dark
}

/** AMOLED's card, and the two steps above it, as brightnesses to match; their own color doesn't matter. */
private val AMOLED_CARD = Color(0xFF101010)
private val AMOLED_RAISED = Color(0xFF1B1B1B)
private val AMOLED_RAISED_MORE = Color(0xFF222222)

/** [color] darkened (or lightened) until it's as bright as [target], keeping its tint. */
private fun withBrightnessOf(color: Color, target: Color): Color {
    val goal = target.luminance()
    val toward = if (color.luminance() > goal) Color.Black else Color.White
    var low = 0f
    var high = 1f
    repeat(20) {
        val mid = (low + high) / 2
        val reached = lerp(color, toward, mid).luminance()
        // Moving toward black lowers brightness, toward white raises it.
        if ((toward == Color.Black) == (reached > goal)) low = mid else high = mid
    }
    return lerp(color, toward, (low + high) / 2)
}

/**
 * The dark palette on pure black: the page goes black, and cards and fields stay just lifted
 * off it so they still read as cards. [ultra] makes the cards black as well. Accents and text
 * are untouched.
 */
fun Palette.amoled(ultra: Boolean = false): Palette = copy(
    crust = Color.Black,
    mantle = Color.Black,
    // Every theme's cards land at the same brightness (a shade under Paid to Poop's #121212), keeping their own
    // tint; buttons and chips on them a fixed step lighter. Themes start at different
    // brightnesses, so darkening them all by the same amount left some cards hard to see.
    base = if (ultra) Color.Black else withBrightnessOf(base, AMOLED_CARD),
    surface0 = withBrightnessOf(surface0, AMOLED_RAISED),
    surface1 = withBrightnessOf(surface1, AMOLED_RAISED_MORE),
)

@Composable
fun AppTheme(palette: Palette, content: @Composable () -> Unit) {
    val p = palette
    fun tint(c: Color, amount: Float = 0.22f) = lerp(p.base, c, amount)
    val onAccent = if (p.isDark) p.crust else p.base
    val colors = if (p.isDark) darkColorScheme() else lightColorScheme()
    val scheme = colors.copy(
        primary = p.mauve, onPrimary = p.onPrimary ?: onAccent,
        primaryContainer = tint(p.mauve), onPrimaryContainer = p.text,
        secondary = p.secondary, onSecondary = p.onSecondary ?: onAccent,
        // Tonal buttons sit on the same grey as fields and chips, with accent text: a tint of a
        // color into a dark card reads as muddy rather than as the theme.
        secondaryContainer = p.surface0, onSecondaryContainer = p.mauve,
        tertiary = p.tertiary, onTertiary = p.onTertiary ?: onAccent,
        tertiaryContainer = tint(p.tertiary), onTertiaryContainer = p.text,
        error = p.red, onError = p.onError ?: onAccent,
        errorContainer = tint(p.red), onErrorContainer = p.text,
        background = p.mantle, onBackground = p.text,
        surface = p.mantle, onSurface = p.text,
        surfaceVariant = p.surface0, onSurfaceVariant = p.subtext0,
        surfaceTint = p.mauve,
        inverseSurface = p.text, inverseOnSurface = p.base, inversePrimary = p.mauve,
        outline = p.overlay0, outlineVariant = p.surface1,
        surfaceBright = p.surface0, surfaceDim = p.crust,
        surfaceContainerLowest = p.crust, surfaceContainerLow = p.mantle,
        surfaceContainer = p.base, surfaceContainerHigh = p.surface0, surfaceContainerHighest = p.surface1,
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, shapes = shapes, typography = Typography, content = content)
    }
}

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Material's type scale, all in JetBrains Mono. */
private val Typography = Typography().run {
    fun TextStyle.mono() = copy(fontFamily = JetBrainsMono)
    copy(
        displayLarge = displayLarge.mono(), displayMedium = displayMedium.mono(), displaySmall = displaySmall.mono(),
        headlineLarge = headlineLarge.mono(), headlineMedium = headlineMedium.mono(), headlineSmall = headlineSmall.mono(),
        titleLarge = titleLarge.mono(), titleMedium = titleMedium.mono(), titleSmall = titleSmall.mono(),
        bodyLarge = bodyLarge.mono(), bodyMedium = bodyMedium.mono(), bodySmall = bodySmall.mono(),
        labelLarge = labelLarge.mono(), labelMedium = labelMedium.mono(), labelSmall = labelSmall.mono(),
    )
}
