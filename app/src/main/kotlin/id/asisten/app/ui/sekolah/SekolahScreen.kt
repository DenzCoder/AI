package id.asisten.app.ui.sekolah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.formulir.DrafPelajaran
import id.asisten.app.domain.formulir.FormulirJadwal
import id.asisten.app.domain.formulir.Hasil
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.waktu.NamaHari
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
import java.time.DayOfWeek
import java.time.LocalDate
import id.asisten.app.ui.komponen.TombolNeo
import id.asisten.app.ui.komponen.TombolTepiNeo
import id.asisten.app.ui.komponen.TombolTeksNeo
import id.asisten.app.ui.komponen.KartuNeo

private class FormPelajaran(val lama: Pelajaran?, val istirahatAwal: Boolean)

/**
 * School section: entry/dismissal hours per weekday, the subject timetable (with break slots)
 * and the duty roster. Nothing is pre-filled: every value comes from the user.
 */
@Composable
fun SekolahScreen(modifier: Modifier = Modifier) {
    val vm = wadahViewModel { SekolahViewModel(it.jadwal) }
    val data by vm.data.collectAsStateWithLifecycle()
    var hariPilihan by rememberSaveable { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    val hari = DayOfWeek.of(hariPilihan)

    var dialogJam by remember { mutableStateOf<DayOfWeek?>(null) }
    var formPelajaran by remember { mutableStateOf<FormPelajaran?>(null) }
    var hapusPelajaran by remember { mutableStateOf<Pelajaran?>(null) }
    var dialogPiket by remember { mutableStateOf(false) }

    val pelajaranHari = data.pelajaran.filter { it.hari == hari }.sortedBy { it.mulai }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---- school hours ----
        item(key = "judul-jam") { JudulBagian("Jam sekolah") }
        item(key = "petunjuk-jam") {
            TeksPetunjuk("Atur jam masuk dan pulang untuk setiap hari. Hari tanpa jam dianggap tidak ada sekolah.")
        }
        items(DayOfWeek.entries, key = { "jam-${it.value}" }) { h ->
            val jam = data.jamSekolah.firstOrNull { it.hari == h }
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(NamaHari.untuk(h), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (jam != null) Waktu.rentang(jam.masuk, jam.pulang) else "Belum diatur",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TombolUbah({ dialogJam = h }, deskripsi = "Ubah jam sekolah " + NamaHari.untuk(h))
                }
            }
        }

        // ---- timetable ----
        item(key = "judul-pelajaran") { JudulBagian("Jadwal pelajaran") }
        item(key = "pilih-hari") {
            BarisPilihan(
                opsi = DayOfWeek.entries,
                terpilih = hari,
                label = { NamaHari.untuk(it) },
                onPilih = { hariPilihan = it.value },
            )
        }
        if (pelajaranHari.isEmpty()) {
            item(key = "kosong-pelajaran") {
                TeksPetunjuk("Belum ada pelajaran untuk hari " + NamaHari.untuk(hari) + ".")
            }
        }
        items(pelajaranHari, key = { "pel-${it.id}" }) { p ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Waktu.rentang(p.mulai, p.selesai),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(p.nama, style = MaterialTheme.typography.titleSmall)
                        val rincian = listOf(p.guru, p.ruang).filter { it.isNotBlank() }.joinToString(" · ")
                        if (rincian.isNotEmpty()) {
                            Text(rincian, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TombolUbah({ formPelajaran = FormPelajaran(p, p.istirahat) }, deskripsi = "Ubah " + p.nama)
                    TombolHapus({ hapusPelajaran = p }, deskripsi = "Hapus " + p.nama)
                }
            }
        }
        item(key = "tambah-pelajaran") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolNeo(onClick = { formPelajaran = FormPelajaran(null, false) }) { Text("Tambah pelajaran") }
                TombolTepiNeo(onClick = { formPelajaran = FormPelajaran(null, true) }) { Text("Tambah istirahat") }
            }
        }

        // ---- duty roster ----
        item(key = "judul-piket") { JudulBagian("Jadwal piket") }
        if (data.piket.isEmpty()) {
            item(key = "kosong-piket") { TeksPetunjuk("Belum ada jadwal piket.") }
        }
        items(data.piket.sortedBy { it.hari.value }, key = { "pik-${it.id}" }) { piket ->
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(NamaHari.untuk(piket.hari), style = MaterialTheme.typography.titleSmall)
                        Text(piket.keterangan, style = MaterialTheme.typography.bodyMedium)
                    }
                    TombolHapus({ vm.hapusPiket(piket.id) }, deskripsi = "Hapus piket " + NamaHari.untuk(piket.hari))
                }
            }
        }
        item(key = "tambah-piket") {
            TombolNeo(onClick = { dialogPiket = true }) { Text("Tambah piket") }
        }
    }

    dialogJam?.let { h ->
        DialogJamSekolah(
            hari = h,
            lama = data.jamSekolah.firstOrNull { it.hari == h },
            onSimpan = {
                vm.simpanJam(it)
                dialogJam = null
            },
            onHapus = {
                vm.hapusJam(h)
                dialogJam = null
            },
            onBatal = { dialogJam = null },
        )
    }
    formPelajaran?.let { f ->
        DialogPelajaran(
            lama = f.lama,
            hari = f.lama?.hari ?: hari,
            istirahatAwal = f.istirahatAwal,
            onSimpan = {
                vm.simpanPelajaran(it)
                formPelajaran = null
            },
            onBatal = { formPelajaran = null },
        )
    }
    hapusPelajaran?.let { p ->
        DialogKonfirmasi(
            judul = "Hapus pelajaran?",
            pesan = "\"${p.nama}\" pada hari ${NamaHari.untuk(p.hari)} akan dihapus.",
            onYa = {
                vm.hapusPelajaran(p.id)
                hapusPelajaran = null
            },
            onBatal = { hapusPelajaran = null },
        )
    }
    if (dialogPiket) {
        DialogPiket(
            hariAwal = hari,
            onSimpan = {
                vm.tambahPiket(it)
                dialogPiket = false
            },
            onBatal = { dialogPiket = false },
        )
    }
}

@Composable
private fun DialogJamSekolah(
    hari: DayOfWeek,
    lama: JamSekolah?,
    onSimpan: (JamSekolah) -> Unit,
    onHapus: () -> Unit,
    onBatal: () -> Unit,
) {
    var masuk by remember { mutableStateOf(lama?.let { Waktu.formatJam(it.masuk) } ?: "") }
    var pulang by remember { mutableStateOf(lama?.let { Waktu.formatJam(it.pulang) } ?: "") }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = "Jam sekolah hari " + NamaHari.untuk(hari),
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirJadwal.jamSekolah(hari, masuk, pulang)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        FieldTeks("Jam masuk", masuk, { masuk = it }, petunjuk = "Format JJ:MM, contoh 07:00")
        FieldTeks("Jam pulang", pulang, { pulang = it }, petunjuk = "Format JJ:MM, contoh 14:00")
        TeksGalat(galat)
        if (lama != null) {
            TombolTeksNeo(onClick = onHapus) { Text("Hapus jam sekolah hari ini") }
        }
    }
}

@Composable
private fun DialogPelajaran(
    lama: Pelajaran?,
    hari: DayOfWeek,
    istirahatAwal: Boolean,
    onSimpan: (Pelajaran) -> Unit,
    onBatal: () -> Unit,
) {
    var draf by remember {
        mutableStateOf(
            if (lama != null) {
                DrafPelajaran(
                    nama = lama.nama,
                    guru = lama.guru,
                    ruang = lama.ruang,
                    mulaiTeks = Waktu.formatJam(lama.mulai),
                    selesaiTeks = Waktu.formatJam(lama.selesai),
                    istirahat = lama.istirahat,
                )
            } else {
                DrafPelajaran(istirahat = istirahatAwal)
            },
        )
    }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = (if (draf.istirahat) "Istirahat" else "Pelajaran") + " hari " + NamaHari.untuk(hari),
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirJadwal.pelajaran(lama?.id ?: 0L, hari, draf)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ini waktu istirahat",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = draf.istirahat, onCheckedChange = { draf = draf.copy(istirahat = it) })
        }
        FieldTeks(
            label = if (draf.istirahat) "Nama (kosongkan untuk \"Istirahat\")" else "Mata pelajaran",
            nilai = draf.nama,
            onUbah = { draf = draf.copy(nama = it) },
        )
        if (!draf.istirahat) {
            FieldTeks("Guru (opsional)", draf.guru, { draf = draf.copy(guru = it) })
            FieldTeks("Ruang (opsional)", draf.ruang, { draf = draf.copy(ruang = it) })
        }
        FieldTeks("Jam mulai", draf.mulaiTeks, { draf = draf.copy(mulaiTeks = it) }, petunjuk = "Format JJ:MM, contoh 07:00")
        FieldTeks("Jam selesai", draf.selesaiTeks, { draf = draf.copy(selesaiTeks = it) }, petunjuk = "Format JJ:MM, contoh 07:45")
        TeksGalat(galat)
    }
}

@Composable
private fun DialogPiket(
    hariAwal: DayOfWeek,
    onSimpan: (id.asisten.app.domain.jadwal.Piket) -> Unit,
    onBatal: () -> Unit,
) {
    var hari by remember { mutableStateOf(hariAwal) }
    var keterangan by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = "Tambah piket",
        onBatal = onBatal,
        onSimpan = {
            when (val hasil = FormulirJadwal.piket(0L, hari, keterangan)) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        BarisPilihan(
            opsi = DayOfWeek.entries,
            terpilih = hari,
            label = { NamaHari.untuk(it) },
            onPilih = { hari = it },
        )
        FieldTeks("Keterangan piket", keterangan, { keterangan = it }, petunjuk = "Contoh: menyapu kelas")
        TeksGalat(galat)
    }
}
