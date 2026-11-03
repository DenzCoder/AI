package id.asisten.app.ui.jadwal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.asisten.app.ui.sekolah.SekolahScreen

/** Jadwal tab: personal calendar and the school section (timetable, hours, duty roster). */
@Composable
fun JadwalScreen(modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Kalender") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Sekolah") })
        }
        if (tab == 0) KalenderTab() else SekolahScreen()
    }
}
