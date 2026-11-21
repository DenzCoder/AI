package id.asisten.app.ui.aktivitas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.Aktivitas
import id.asisten.app.domain.aktivitas.FormatAktivitas
import id.asisten.app.domain.aktivitas.JangkaTujuan
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.formulir.DrafTugas
import id.asisten.app.domain.formulir.DrafTujuan
import id.asisten.app.domain.formulir.FormulirAktivitas
import id.asisten.app.domain.formulir.Hasil
import id.asisten.app.domain.label
import id.asisten.app.domain.waktu.Epoch
import id.asisten.app.domain.waktu.Waktu
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.DialogForm
import id.asisten.app.ui.komponen.DialogKonfirmasi
import id.asisten.app.ui.komponen.FieldTeks
import id.asisten.app.ui.komponen.JudulBagian
import id.asisten.app.ui.komponen.TeksGalat
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.komponen.TombolHapus
import id.asisten.app.ui.komponen.TombolUbah
import id.asisten.app.ui.wadahViewModel
import java.time.LocalDate
import id.asisten.app.ui.komponen.TabNeo
import id.asisten.app.ui.komponen.TombolNeo
import id.asisten.app.ui.komponen.TombolTepiNeo
import id.asisten.app.ui.komponen.TombolTeksNeo
import id.asisten.app.ui.komponen.KartuNeo
import id.asisten.app.ui.komponen.BilahProgresNeo

private val NAMA_TAB = listOf("Tugas", "Tujuan", "Riwayat")

/** Aktivitas tab: tasks, homework and exams; goals with milestones; and a completion history. */
@Composable
fun AktivitasScreen(modifier: Modifier = Modifier) {
    val vm = wadahViewModel { AktivitasViewModel(it.aktivitas) }
    val tugas by vm.tugas.collectAsStateWithLifecycle()
    val tujuan by vm.tujuan.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabNeo(daftar = NAMA_TAB, terpilih = tab, onPilih = { tab = it })
        when (tab) {
            0 -> TabTugas(tugas, vm)
            1 -> TabTujuan(tujuan, vm)
            else -> TabRiwayat(tugas, tujuan, vm)
        }
    }
}

// ---- tasks ----

private class FormTugas(val lama: Tugas?)

@Composable
private fun TabTugas(daftar: List<Tugas>, vm: AktivitasViewModel) {
    var filter by remember { mutableStateOf<JenisTugas?>(null) }
    var form by remember { mutableStateOf<FormTugas?>(null) }
    var hapus by remember { mutableStateOf<Tugas?>(null) }
    val hariIni = LocalDate.now()
    val tampil = Aktivitas.urutkanTugas(daftar.filter { !it.selesai && (filter == null || it.jenis == filter) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "filter") {
            BarisPilihan(
                opsi = listOf<JenisTugas?>(null) + JenisTugas.entries,
                terpilih = filter,
                label = { it?.label() ?: "Semua" },
                onPilih = { filter = it },
            )
        }
        item(key = "tambah") {
            TombolNeo(onClick = { form = FormTugas(null) }) { Text("Tambah tugas, PR, atau ujian") }
        }
        if (tampil.isEmpty()) {
            item(key = "kosong") { TeksPetunjuk("Tidak ada yang menunggu. Item yang sudah selesai ada di tab Riwayat.") }
        }
        items(tampil, key = { "t${it.id}" }) { t ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = t.selesai, onCheckedChange = { vm.setTugasSelesai(t, it) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.judul, style = MaterialTheme.typography.titleSmall)
                        val rincian = listOfNotNull(
                            t.jenis.label(),
                            t.mapel.takeIf { it.isNotBlank() },
                            FormatAktivitas.tenggat(t, hariIni),
                            "Prioritas " + t.prioritas.label().lowercase(),
                        ).joinToString(" · ")
                        Text(
                            text = rincian,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TombolUbah({ form = FormTugas(t) }, deskripsi = "Ubah " + t.judul)
                    TombolHapus({ hapus = t }, deskripsi = "Hapus " + t.judul)
                }
            }
        }
    }

    form?.let { f ->
        DialogTugas(
            lama = f.lama,
            onSimpan = {
                vm.simpanTugas(it)
                form = null
            },
            onBatal = { form = null },
        )
    }
    hapus?.let { t ->
        DialogKonfirmasi(
            judul = "Hapus ${t.jenis.label().lowercase()}?",
            pesan = "\"${t.judul}\" akan dihapus.",
            onYa = {
                vm.hapusTugas(t.id)
                hapus = null
            },
            onBatal = { hapus = null },
        )
    }
}

@Composable
private fun DialogTugas(lama: Tugas?, onSimpan: (Tugas) -> Unit, onBatal: () -> Unit) {
    var draf by remember {
        mutableStateOf(
            if (lama != null) {
                DrafTugas(
                    judul = lama.judul,
                    jenis = lama.jenis,
                    mapel = lama.mapel,
                    tenggatTeks = lama.tenggat?.let { Waktu.formatTanggal(it) } ?: "",
                    prioritas = lama.prioritas,
                    catatan = lama.catatan,
                )
            } else {
                DrafTugas()
            },
        )
    }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = if (lama == null) "Tambah tugas" else "Ubah tugas",
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirAktivitas.tugas(lama?.id ?: 0L, draf, lama)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        BarisPilihan(
            opsi = JenisTugas.entries,
            terpilih = draf.jenis,
            label = { it.label() },
            onPilih = { draf = draf.copy(jenis = it) },
        )
        FieldTeks("Judul", draf.judul, { draf = draf.copy(judul = it) })
        FieldTeks("Mata pelajaran (opsional)", draf.mapel, { draf = draf.copy(mapel = it) })
        FieldTeks(
            label = "Tenggat (opsional)",
            nilai = draf.tenggatTeks,
            onUbah = { draf = draf.copy(tenggatTeks = it) },
            petunjuk = "Format HH-BB-TTTT, contoh 02-11-2026. Kosongkan bila tanpa tenggat.",
        )
        Text(text = "Prioritas", style = MaterialTheme.typography.labelLarge)
        BarisPilihan(
            opsi = Prioritas.entries,
            terpilih = draf.prioritas,
            label = { it.label() },
            onPilih = { draf = draf.copy(prioritas = it) },
        )
        FieldTeks("Catatan (opsional)", draf.catatan, { draf = draf.copy(catatan = it) }, singleLine = false)
        TeksGalat(galat)
    }
}

// ---- goals ----

private class FormTujuan(val lama: Tujuan?)

@Composable
private fun TabTujuan(daftar: List<Tujuan>, vm: AktivitasViewModel) {
    var form by remember { mutableStateOf<FormTujuan?>(null) }
    var hapus by remember { mutableStateOf<Tujuan?>(null) }
    var tambahTonggak by remember { mutableStateOf<Tujuan?>(null) }
    val aktif = daftar.filter { !it.selesai }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "tambah") {
            TombolNeo(onClick = { form = FormTujuan(null) }) { Text("Tambah tujuan") }
        }
        if (aktif.isEmpty()) {
            item(key = "kosong") { TeksPetunjuk("Belum ada tujuan aktif. Tujuan yang sudah tercapai ada di tab Riwayat.") }
        }
        items(aktif, key = { "g${it.id}" }) { tujuan ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tujuan.judul, style = MaterialTheme.typography.titleMedium)
                            val rincian = listOfNotNull(
                                tujuan.jangka.label(),
                                tujuan.tenggat?.let { "Tenggat " + Waktu.formatTanggal(it) },
                            ).joinToString(" · ")
                            Text(
                                text = rincian,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TombolUbah({ form = FormTujuan(tujuan) }, deskripsi = "Ubah " + tujuan.judul)
                        TombolHapus({ hapus = tujuan }, deskripsi = "Hapus " + tujuan.judul)
                    }
                    if (tujuan.tonggak.isNotEmpty()) {
                        BilahProgresNeo(progres = tujuan.kemajuan, modifier = Modifier.fillMaxWidth())
                        Text(
                            text = FormatAktivitas.persen(tujuan.kemajuan).toString() + "% (" +
                                tujuan.tonggak.count { it.selesai } + " dari " + tujuan.tonggak.size + " tonggak)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        TeksPetunjuk("Belum ada tonggak. Tambahkan langkah kecil untuk melacak kemajuan.")
                    }
                    tujuan.tonggak.forEach { tonggak ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = tonggak.selesai,
                                onCheckedChange = { vm.setTonggakSelesai(tonggak, it) },
                            )
                            Text(
                                text = tonggak.judul,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (tonggak.selesai) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f),
                            )
                            TombolHapus({ vm.hapusTonggak(tonggak.id) }, deskripsi = "Hapus tonggak " + tonggak.judul)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TombolTepiNeo(onClick = { tambahTonggak = tujuan }) { Text("Tambah tonggak") }
                        TombolNeo(onClick = { vm.setTujuanSelesai(tujuan, true) }) { Text("Tandai tercapai") }
                    }
                }
            }
        }
    }

    form?.let { f ->
        DialogTujuan(
            lama = f.lama,
            onSimpan = {
                vm.simpanTujuan(it)
                form = null
            },
            onBatal = { form = null },
        )
    }
    tambahTonggak?.let { t ->
        DialogTonggak(
            tujuanId = t.id,
            onSimpan = {
                vm.tambahTonggak(it)
                tambahTonggak = null
            },
            onBatal = { tambahTonggak = null },
        )
    }
    hapus?.let { t ->
        DialogKonfirmasi(
            judul = "Hapus tujuan?",
            pesan = "\"${t.judul}\" beserta semua tonggaknya akan dihapus.",
            onYa = {
                vm.hapusTujuan(t.id)
                hapus = null
            },
            onBatal = { hapus = null },
        )
    }
}

@Composable
private fun DialogTujuan(lama: Tujuan?, onSimpan: (Tujuan) -> Unit, onBatal: () -> Unit) {
    var draf by remember {
        mutableStateOf(
            if (lama != null) {
                DrafTujuan(
                    judul = lama.judul,
                    jangka = lama.jangka,
                    tenggatTeks = lama.tenggat?.let { Waktu.formatTanggal(it) } ?: "",
                )
            } else {
                DrafTujuan()
            },
        )
    }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = if (lama == null) "Tambah tujuan" else "Ubah tujuan",
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirAktivitas.tujuan(lama?.id ?: 0L, draf, lama)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        FieldTeks("Judul tujuan", draf.judul, { draf = draf.copy(judul = it) })
        BarisPilihan(
            opsi = JangkaTujuan.entries,
            terpilih = draf.jangka,
            label = { it.label() },
            onPilih = { draf = draf.copy(jangka = it) },
        )
        FieldTeks(
            label = "Tenggat (opsional)",
            nilai = draf.tenggatTeks,
            onUbah = { draf = draf.copy(tenggatTeks = it) },
            petunjuk = "Format HH-BB-TTTT, contoh 02-11-2026. Kosongkan bila tanpa tenggat.",
        )
        TeksGalat(galat)
    }
}

@Composable
private fun DialogTonggak(tujuanId: Long, onSimpan: (Tonggak) -> Unit, onBatal: () -> Unit) {
    var judul by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = "Tambah tonggak",
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirAktivitas.tonggak(tujuanId, judul)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        FieldTeks("Langkah yang harus dicapai", judul, { judul = it })
        TeksGalat(galat)
    }
}

// ---- history ----

@Composable
private fun TabRiwayat(tugas: List<Tugas>, tujuan: List<Tujuan>, vm: AktivitasViewModel) {
    val tugasSelesai = tugas.filter { it.selesai }.sortedByDescending { it.selesaiPada ?: 0L }
    val tujuanTercapai = Aktivitas.riwayatPencapaian(tujuan)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "judul-tugas") { JudulBagian("Tugas selesai") }
        if (tugasSelesai.isEmpty()) {
            item(key = "kosong-tugas") { TeksPetunjuk("Belum ada tugas yang diselesaikan.") }
        }
        items(tugasSelesai, key = { "rt${it.id}" }) { t ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.judul, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = t.jenis.label() + tanggalSelesai(t.selesaiPada),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TombolTeksNeo(onClick = { vm.setTugasSelesai(t, false) }) { Text("Batalkan") }
                }
            }
        }
        item(key = "judul-tujuan") { JudulBagian("Tujuan tercapai") }
        if (tujuanTercapai.isEmpty()) {
            item(key = "kosong-tujuan") { TeksPetunjuk("Belum ada tujuan yang tercapai.") }
        }
        items(tujuanTercapai, key = { "rg${it.id}" }) { g ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(g.judul, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = g.jangka.label() + tanggalSelesai(g.selesaiPada),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TombolTeksNeo(onClick = { vm.setTujuanSelesai(g, false) }) { Text("Batalkan") }
                }
            }
        }
    }
}

private fun tanggalSelesai(selesaiPada: Long?): String =
    if (selesaiPada == null) "" else " · selesai " + Waktu.formatTanggal(Epoch.tanggal(selesaiPada))
