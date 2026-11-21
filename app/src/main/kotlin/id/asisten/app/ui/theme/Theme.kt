package id.asisten.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import id.asisten.app.domain.pengaturan.Pengaturan

// Neo-brutalism: flat cream paper, white cards, thick ink outlines, hard offset shadows, loud accent colours.
private val Krem = Color(0xFFFFF3D6)
private val Pasir = Color(0xFFF3E7C3)
private val Kuning = Color(0xFFFFD83D)
private val KremTeks = Color(0xFFF6F1E3)

private fun skemaTerang(aksen: Color): ColorScheme =
    lightColorScheme(
        primary = aksen,
        onPrimary = aksen.tintaKontras(),
        primaryContainer = lerp(aksen, Color.White, 0.72f),
        onPrimaryContainer = Tinta,
        secondary = Kuning,
        onSecondary = Tinta,
        secondaryContainer = Color(0xFFFFE680),
        onSecondaryContainer = Tinta,
        tertiary = Color(0xFFB0124F),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFC2DA),
        onTertiaryContainer = Tinta,
        background = Krem,
        onBackground = Tinta,
        surface = Color.White,
        onSurface = Tinta,
        surfaceVariant = Pasir,
        onSurfaceVariant = Color(0xFF3D3A33),
        error = Color(0xFFC62828),
        onError = Color.White,
        errorContainer = Color(0xFFFFC9C9),
        onErrorContainer = Tinta,
        outline = Tinta,
        outlineVariant = Color(0xFF6B6558),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Pasir,
    )

private fun skemaGelap(aksen: Color): ColorScheme =
    darkColorScheme(
        primary = aksen,
        onPrimary = aksen.tintaKontras(),
        primaryContainer = lerp(aksen, Color.Black, 0.6f),
        onPrimaryContainer = Color.White,
        secondary = Kuning,
        onSecondary = Tinta,
        secondaryContainer = Color(0xFF5C4B00),
        onSecondaryContainer = KremTeks,
        tertiary = Color(0xFFFF8FB8),
        onTertiary = Tinta,
        tertiaryContainer = Color(0xFF5A1F3A),
        onTertiaryContainer = KremTeks,
        background = Color(0xFF14121A),
        onBackground = KremTeks,
        surface = Color(0xFF211E2B),
        onSurface = KremTeks,
        surfaceVariant = Color(0xFF2E2A3B),
        onSurfaceVariant = Color(0xFFD6D0C2),
        error = Color(0xFFFF6B6B),
        onError = Tinta,
        errorContainer = Color(0xFF5A1A1A),
        onErrorContainer = KremTeks,
        outline = KremTeks,
        outlineVariant = Color(0xFF8C8678),
        surfaceContainerLowest = Color(0xFF211E2B),
        surfaceContainerLow = Color(0xFF211E2B),
        surfaceContainer = Color(0xFF211E2B),
        surfaceContainerHigh = Color(0xFF211E2B),
        surfaceContainerHighest = Color(0xFF2E2A3B),
    )

private fun gayaTerang() = GayaNeo(garis = Tinta, bayangan = Tinta, mati = Color(0xFFE4DFD0))

// In dark mode a black shadow would vanish, so the hard shadow takes the accent colour and the outline turns cream.
private fun gayaGelap(aksen: Color) = GayaNeo(garis = KremTeks, bayangan = aksen, mati = Color(0xFF3A3648))

private val Bentuk = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

private val Tipografi: Typography = run {
    val dasar = Typography()
    dasar.copy(
        headlineLarge = dasar.headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = dasar.headlineMedium.copy(fontWeight = FontWeight.Black),
        headlineSmall = dasar.headlineSmall.copy(fontWeight = FontWeight.Black),
        titleLarge = dasar.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = dasar.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleSmall = dasar.titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = dasar.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        labelMedium = dasar.labelMedium.copy(fontWeight = FontWeight.Bold),
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
    val gaya = remember(aksen, darkTheme) {
        if (darkTheme) gayaGelap(aksen) else gayaTerang()
    }
    val density = LocalDensity.current
    val kepadatan = remember(density, pengaturan.ukuranTeks) {
        Density(density.density, density.fontScale * pengaturan.ukuranTeks.skala)
    }

    CompositionLocalProvider(LocalDensity provides kepadatan, LocalNeo provides gaya) {
        MaterialTheme(
            colorScheme = skema,
            shapes = Bentuk,
            typography = Tipografi,
            content = content,
        )
    }
}
