package app.ghars.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.ghars.R

private fun readex(weight: FontWeight) = Font(
    resId = R.font.readex_pro,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Readex Pro (variable font), falling back to Plex for any missing glyph. */
val GharsDisplayFamily = FontFamily(
    readex(FontWeight.SemiBold),
    readex(FontWeight.Bold),
)

val GharsTextFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

/** Type styles from docs/design/tokens.json. Sizes are sp so they follow the system font scale. */
@Immutable
data class GharsTypography(
    /** Countdown only. Tabular figures so digits do not jitter. */
    val timer: TextStyle = TextStyle(
        fontFamily = GharsDisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 72.sp, lineHeight = 76.sp, fontFeatureSettings = "tnum",
    ),
    val display: TextStyle = TextStyle(
        fontFamily = GharsDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 48.sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = GharsDisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = GharsDisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = GharsTextFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 26.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = GharsTextFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = GharsTextFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp,
    ),
)
