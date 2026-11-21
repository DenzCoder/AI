package id.asisten.app.ui.jadwal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.asisten.app.ui.komponen.TabNeo
import id.asisten.app.ui.sekolah.SekolahScreen

/** Jadwal tab: personal calendar and the school section (timetable, hours, duty roster). */
@Composable
fun JadwalScreen(modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        TabNeo(daftar = listOf("Kalender", "Sekolah"), terpilih = tab, onPilih = { tab = it })
        if (tab == 0) KalenderTab() else SekolahScreen()
    }
}
