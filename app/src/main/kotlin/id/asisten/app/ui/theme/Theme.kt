package id.asisten.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF1F2A52)
private val Orange = Color(0xFFF28C38)
private val Purple = Color(0xFF7E57C2)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Purple,
    onTertiary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Navy,
    secondary = Orange,
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF2E1A5C),
)

/**
 * Phase 1 base theme: follows the system light/dark setting.
 * The full design system (accent colors, text size, animation toggle) is built in Phase 2.
 */
@Composable
fun AsistenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
