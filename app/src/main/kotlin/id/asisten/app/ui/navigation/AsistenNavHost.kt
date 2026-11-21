package id.asisten.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.profil.ProfilPengguna
import id.asisten.app.ui.aktivitas.AktivitasScreen
import id.asisten.app.ui.beranda.BerandaScreen
import id.asisten.app.ui.hanako.HanakoScreen
import id.asisten.app.ui.jadwal.JadwalScreen
import id.asisten.app.ui.pengaturan.PengaturanViewModel
import id.asisten.app.ui.profil.ProfilScreen
import id.asisten.app.ui.theme.LocalNeo

/**
 * [tujuanBuka] is the route of a tab requested from outside (a notification). The app navigates there once and
 * then calls [onTujuanDipakai] so a screen rotation or recomposition does not repeat it.
 */
@Composable
fun AsistenApp(
    pengaturanViewModel: PengaturanViewModel,
    tujuanBuka: String? = null,
    onTujuanDipakai: () -> Unit = {},
) {
    val pengaturanTersimpan by pengaturanViewModel.pengaturan.collectAsStateWithLifecycle()
    val profilTersimpan by pengaturanViewModel.profil.collectAsStateWithLifecycle()
    val pengaturan = pengaturanTersimpan ?: Pengaturan()
    val profil = profilTersimpan ?: ProfilPengguna()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val animasi = pengaturan.animasiAktif

    LaunchedEffect(tujuanBuka) {
        if (tujuanBuka != null) {
            val tujuan = AsistenDestination.entries.firstOrNull { it.route == tujuanBuka }
            if (tujuan != null) {
                navController.navigate(tujuan.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onTujuanDipakai()
        }
    }

    Scaffold(
        bottomBar = {
            val skema = MaterialTheme.colorScheme
            val garisNeo = LocalNeo.current.garis
            NavigationBar(
                // A thick ink line along the top edge, drawn over the bar's own background.
                modifier = Modifier.drawWithContent {
                    drawContent()
                    val tebal = 3.dp.toPx()
                    drawLine(
                        color = garisNeo,
                        start = Offset(0f, tebal / 2),
                        end = Offset(size.width, tebal / 2),
                        strokeWidth = tebal,
                    )
                },
                containerColor = skema.surface,
                contentColor = skema.onSurface,
                tonalElevation = 0.dp,
            ) {
                val warnaItem = NavigationBarItemDefaults.colors(
                    selectedIconColor = skema.onPrimary,
                    selectedTextColor = skema.onSurface,
                    indicatorColor = skema.primary,
                    unselectedIconColor = skema.onSurfaceVariant,
                    unselectedTextColor = skema.onSurfaceVariant,
                )
                AsistenDestination.entries.forEach { tujuan ->
                    val label = stringResource(tujuan.label)
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tujuan.route } == true,
                        onClick = {
                            navController.navigate(tujuan.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tujuan.icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = warnaItem,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AsistenDestination.BERANDA.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { if (animasi) fadeIn() else EnterTransition.None },
            exitTransition = { if (animasi) fadeOut() else ExitTransition.None },
            popEnterTransition = { if (animasi) fadeIn() else EnterTransition.None },
            popExitTransition = { if (animasi) fadeOut() else ExitTransition.None },
        ) {
            composable(AsistenDestination.BERANDA.route) { BerandaScreen() }
            composable(AsistenDestination.JADWAL.route) { JadwalScreen() }
            composable(AsistenDestination.AKTIVITAS.route) { AktivitasScreen() }
            composable(AsistenDestination.HANAKO.route) { HanakoScreen() }
            composable(AsistenDestination.PROFIL.route) {
                ProfilScreen(
                    pengaturan = pengaturan,
                    namaPengguna = profil.nama,
                    memoriAktif = profil.memoriAktif,
                    onModeTema = pengaturanViewModel::ubahModeTema,
                    onAksen = pengaturanViewModel::ubahAksen,
                    onUkuranTeks = pengaturanViewModel::ubahUkuranTeks,
                    onAnimasi = pengaturanViewModel::ubahAnimasi,
                    onSimpanNama = pengaturanViewModel::simpanNama,
                    onMemoriAktif = pengaturanViewModel::ubahMemoriAktif,
                )
            }
        }
    }
}
