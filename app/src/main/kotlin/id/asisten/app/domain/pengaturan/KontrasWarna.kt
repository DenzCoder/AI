package id.asisten.app.domain.pengaturan

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Picks readable text for a coloured background, by the WCAG contrast ratio (not by a rough "light or dark" guess).
 * The accent colour is chosen by the user, so the text on buttons that use it must adapt: white on purple, but
 * near-black on orange, green and blue, where white would be hard to read.
 */
object KontrasWarna {
    const val TINTA_GELAP: Long = 0xFF111111

    private fun linear(saluran: Int): Double {
        val s = saluran / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** Relative luminance (0 black .. 1 white) of an 0xAARRGGBB colour; the alpha part is ignored. */
    fun luminansi(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }

    /** Contrast ratio between two colours, from 1 (identical) to 21 (black on white). */
    fun rasio(a: Long, b: Long): Double {
        val la = luminansi(a)
        val lb = luminansi(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** True when near-black text is easier to read than white text on [latar]. */
    fun pakaiTintaGelap(latar: Long): Boolean = rasio(latar, TINTA_GELAP) >= rasio(latar, 0xFFFFFFFF)
}
