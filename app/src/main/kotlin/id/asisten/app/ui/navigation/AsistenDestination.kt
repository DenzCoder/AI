package id.asisten.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import id.asisten.app.R

/** The five main tabs. Plain string routes avoid an extra serialization dependency. */
enum class AsistenDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    BERANDA("beranda", R.string.nav_beranda, Icons.Filled.Home),
    JADWAL("jadwal", R.string.nav_jadwal, Icons.Filled.DateRange),
    AKTIVITAS("aktivitas", R.string.nav_aktivitas, Icons.AutoMirrored.Filled.List),
    HANAKO("hanako", R.string.nav_hanako, Icons.Filled.Face),
    PROFIL("profil", R.string.nav_profil, Icons.Filled.Person),
}
