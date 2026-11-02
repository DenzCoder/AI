package id.asisten.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.asisten.app.R
import id.asisten.app.ui.beranda.BerandaScreen
import id.asisten.app.ui.screens.BagianScreen

@Composable
fun AsistenApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AsistenDestination.BERANDA.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AsistenDestination.BERANDA.route) { BerandaScreen() }
            composable(AsistenDestination.JADWAL.route) {
                BagianScreen(R.string.jadwal_judul, R.string.jadwal_keterangan)
            }
            composable(AsistenDestination.AKTIVITAS.route) {
                BagianScreen(R.string.aktivitas_judul, R.string.aktivitas_keterangan)
            }
            composable(AsistenDestination.HANAKO.route) {
                BagianScreen(R.string.hanako_judul, R.string.hanako_keterangan)
            }
            composable(AsistenDestination.PROFIL.route) {
                BagianScreen(R.string.profil_judul, R.string.profil_keterangan)
            }
        }
    }
}
