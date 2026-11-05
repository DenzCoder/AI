package id.asisten.app.ui.beranda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.R
import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.aktivitas.FormatAktivitas
import id.asisten.app.domain.dasbor.RingkasanBeranda
import id.asisten.app.domain.dasbor.WidgetBeranda
import id.asisten.app.domain.hanako.PilihEkspresi
import id.asisten.app.domain.waktu.NamaHari
import id.asisten.app.domain.waktu.Waktu
import id.asisten.app.ui.hanako.HanakoAvatar
import id.asisten.app.ui.wadahViewModel
import java.time.LocalTime

private const val BATAS_JADWAL = 5

@Composable
fun BerandaScreen(modifier: Modifier = Modifier) {
    val viewModel = wadahViewModel { BerandaViewModel(it.pengaturan, it.jadwal, it.aktivitas) }
    val sapaan by viewModel.sapaan.collectAsStateWithLifecycle()
    val nama by viewModel.nama.collectAsStateWithLifecycle()
    val riwayat by viewModel.riwayat.collectAsStateWithLifecycle()
    val mengatur by viewModel.mengatur.collectAsStateWithLifecycle()
    val ringkasan by viewModel.ringkasan.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.segarkan() }

    if (mengatur) {
        PanelAturWidget(
            urutan = riwayat.sekarang.urutan,
            tersembunyi = riwayat.sekarang.tersembunyi,
            dapatDiurungkan = riwayat.dapatDiurungkan,
            onPindah = viewModel::pindah,
            onTampil = viewModel::aturTampil,
            onUrungkan = viewModel::urungkan,
            onStandar = viewModel::kembalikanStandar,
            onSelesai = viewModel::selesaiMengatur,
            modifier = modifier,
        )
    } else {
        Dasbor(
            sapaan = sapaan,
            nama = nama,
            ringkasan = ringkasan,
            terlihat = riwayat.sekarang.terlihat,
            onAtur = viewModel::mulaiMengatur,
            modifier = modifier,
        )
    }
}

@Composable
private fun Dasbor(
    sapaan: Sapaan,
    nama: String,
    ringkasan: RingkasanBeranda,
    terlihat: List<WidgetBeranda>,
    onAtur: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val salam = stringResource(sapaan.teks())
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (nama.isBlank()) salam else "$salam, $nama",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAtur) {
                    Text(stringResource(R.string.beranda_atur_widget))
                }
            }
        }
        item(key = "hanako") { KartuHanako() }
        if (terlihat.isEmpty()) {
            item(key = "kosong") {
                Text(
                    text = stringResource(R.string.atur_semua_tersembunyi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(terlihat, key = { it.name }) { widget -> KartuWidget(widget, ringkasan) }
    }
}

@Composable
private fun KartuHanako() {
    // The expression follows the time of day; it is chosen once when the card first appears.
    val ekspresi = remember { PilihEkspresi.untukWaktu(LocalTime.now()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HanakoAvatar(ekspresi = ekspresi, lebar = 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.hanako_kartu_judul),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.hanako_kartu_isi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun KartuWidget(widget: WidgetBeranda, ringkasan: RingkasanBeranda) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(widget.judul()),
                style = MaterialTheme.typography.titleMedium,
            )
            IsiWidget(widget, ringkasan)
        }
    }
}

/** Real data for the four cards that have a module; the rest keep an honest "not available yet" note. */
@Composable
private fun IsiWidget(widget: WidgetBeranda, r: RingkasanBeranda) {
    when (widget) {
        WidgetBeranda.JADWAL_HARI_INI -> {
            if (r.jadwalHariIni.isEmpty()) {
                Keterangan(widget)
            } else {
                r.jadwalHariIni.take(BATAS_JADWAL).forEach { blok ->
                    Text(
                        text = Waktu.formatJam(blok.mulai) + "  " + blok.judul + if (blok.bentrok) "  (bentrok)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (r.jadwalHariIni.size > BATAS_JADWAL) {
                    Text(
                        text = "+${r.jadwalHariIni.size - BATAS_JADWAL} lainnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        WidgetBeranda.TUGAS -> {
            if (r.tugasTerdekat.isEmpty()) {
                Keterangan(widget)
            } else {
                r.tugasTerdekat.forEach { tugas ->
                    Text(
                        text = tugas.judul + " – " + FormatAktivitas.tenggat(tugas, r.hariIni),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        WidgetBeranda.SEKOLAH -> {
            val jam = r.jamSekolahHariIni
            if (jam == null && r.piketHariIni.isEmpty()) {
                Keterangan(widget)
            } else {
                if (jam != null) {
                    Text(
                        text = "Sekolah " + Waktu.rentang(jam.masuk, jam.pulang),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                r.piketHariIni.forEach { piket ->
                    Text(
                        text = "Piket " + NamaHari.untuk(piket.hari) + ": " + piket.keterangan,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        WidgetBeranda.TUJUAN -> {
            if (r.tujuanAktif.isEmpty()) {
                Keterangan(widget)
            } else {
                r.tujuanAktif.forEach { tujuan ->
                    Text(text = tujuan.judul, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { tujuan.kemajuan },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = FormatAktivitas.persen(tujuan.kemajuan).toString() + "%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        WidgetBeranda.SHOLAT, WidgetBeranda.KEBIASAAN, WidgetBeranda.KEUANGAN -> Keterangan(widget)
    }
}

@Composable
private fun Keterangan(widget: WidgetBeranda) {
    Text(
        text = stringResource(widget.keterangan()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PanelAturWidget(
    urutan: List<WidgetBeranda>,
    tersembunyi: Set<WidgetBeranda>,
    dapatDiurungkan: Boolean,
    onPindah: (WidgetBeranda, Int) -> Unit,
    onTampil: (WidgetBeranda, Boolean) -> Unit,
    onUrungkan: () -> Unit,
    onStandar: () -> Unit,
    onSelesai: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.atur_judul),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onSelesai) { Text(stringResource(R.string.atur_selesai)) }
            }
        }
        item(key = "keterangan") {
            Text(
                text = stringResource(R.string.atur_keterangan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "aksi") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUrungkan, enabled = dapatDiurungkan) {
                    Text(stringResource(R.string.atur_urungkan))
                }
                OutlinedButton(onClick = onStandar) {
                    Text(stringResource(R.string.atur_standar))
                }
            }
        }
        items(urutan, key = { it.name }) { widget ->
            val nama = stringResource(widget.judul())
            val indeks = urutan.indexOf(widget)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = nama,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onPindah(widget, -1) },
                        enabled = indeks > 0,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.atur_naik, nama),
                        )
                    }
                    IconButton(
                        onClick = { onPindah(widget, +1) },
                        enabled = indeks < urutan.lastIndex,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.atur_turun, nama),
                        )
                    }
                    Switch(
                        checked = widget !in tersembunyi,
                        onCheckedChange = { onTampil(widget, it) },
                    )
                }
            }
        }
    }
}

private fun Sapaan.teks(): Int = when (this) {
    Sapaan.PAGI -> R.string.sapaan_pagi
    Sapaan.SIANG -> R.string.sapaan_siang
    Sapaan.SORE -> R.string.sapaan_sore
    Sapaan.MALAM -> R.string.sapaan_malam
}
