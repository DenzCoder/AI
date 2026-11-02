package id.asisten.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import id.asisten.app.domain.pengaturan.Pengaturan

private val Navy = Color(0xFF1F2A52)
private val Ungu = Color(0xFF7E57C2)

private fun kontras(warna: Color): Color =
    if (warna.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White

private fun skemaTerang(aksen: Color): ColorScheme {
    val wadah = lerp(aksen, Color.White, 0.8f)
    return lightColorScheme(
        primary = aksen,
        onPrimary = kontras(aksen),
        primaryContainer = wadah,
        onPrimaryContainer = Navy,
        secondary = Navy,
        onSecondary = Color.White,
        tertiary = Ungu,
        onTertiary = Color.White,
    )
}

private fun skemaGelap(aksen: Color): ColorScheme {
    val wadah = lerp(aksen, Color.Black, 0.6f)
    return darkColorScheme(
        primary = aksen,
        onPrimary = kontras(aksen),
        primaryContainer = wadah,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFB8C4FF),
        onSecondary = Navy,
        tertiary = Color(0xFFD0BCFF),
        onTertiary = Color(0xFF2E1A5C),
    )
}

/**
 * App theme: light/dark, accent color and text-size scale all come from [pengaturan].
 * [darkTheme] is resolved by the caller because "follow system" needs the system setting.
 */
@Composable
fun AsistenTheme(
    pengaturan: Pengaturan,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val aksen = Color(pengaturan.aksen.argb)
    val skema = remember(aksen, darkTheme) {
        if (darkTheme) skemaGelap(aksen) else skemaTerang(aksen)
    }
    val density = LocalDensity.current
    val kepadatan = remember(density, pengaturan.ukuranTeks) {
        Density(density.density, density.fontScale * pengaturan.ukuranTeks.skala)
    }

    CompositionLocalProvider(LocalDensity provides kepadatan) {
        MaterialTheme(
            colorScheme = skema,
            content = content,
        )
    }
}
