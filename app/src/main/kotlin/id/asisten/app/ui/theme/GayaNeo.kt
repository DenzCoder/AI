package id.asisten.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.asisten.app.domain.pengaturan.KontrasWarna

/** The near-black ink of the neo-brutalist look: borders, hard shadows and text on light surfaces. */
internal val Tinta = Color(KontrasWarna.TINTA_GELAP)

/**
 * Text colour for a coloured background: near-black or white, whichever is easier to read (WCAG contrast).
 * The accent colour belongs to the user, so text on it must adapt rather than being fixed.
 */
fun Color.tintaKontras(): Color =
    if (KontrasWarna.pakaiTintaGelap(toArgb().toLong())) Tinta else Color.White

/**
 * The few values the neo-brutalist components share: outline colour, hard (blur-free) shadow colour,
 * the colour of a disabled control, and the line and shadow thickness. Light and dark mode differ only here.
 */
@Immutable
class GayaNeo(
    val garis: Color,
    val bayangan: Color,
    val mati: Color,
    val tebal: Dp = 2.dp,
    val geser: Dp = 4.dp,
)

val LocalNeo = staticCompositionLocalOf {
    GayaNeo(garis = Tinta, bayangan = Tinta, mati = Color(0xFFE4DFD0))
}
