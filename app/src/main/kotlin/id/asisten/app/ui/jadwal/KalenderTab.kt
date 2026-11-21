package id.asisten.app.ui.jadwal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.formulir.DrafKegiatan
import id.asisten.app.domain.formulir.FormulirJadwal
import id.asisten.app.domain.formulir.Hasil
import id.asisten.app.domain.jadwal.ItemHarian
import id.asisten.app.domain.jadwal.Kalender
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.ModeKalender
import id.asisten.app.domain.jadwal.NavigasiKalender
import id.asisten.app.domain.jadwal.PenyusunHarian
import id.asisten.app.domain.jadwal.SumberItem
import id.asisten.app.domain.label
import id.asisten.app.domain.waktu.NamaHari
import id.asisten.app.domain.waktu.Waktu
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.DialogForm
import id.asisten.app.ui.komponen.DialogKonfirmasi
import id.asisten.app.ui.komponen.FieldTeks
import id.asisten.app.ui.komponen.TeksGalat
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.wadahViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import id.asisten.app.ui.komponen.TombolNeo
import id.asisten.app.ui.komponen.TombolTeksNeo
import id.asisten.app.ui.komponen.KartuNeo
import id.asisten.app.ui.theme.LocalNeo
import id.asisten.app.ui.theme.tintaKontras

private class FormKegiatan(val lama: Kegiatan?)

@Composable
fun KalenderTab(modifier: Modifier = Modifier) {
    val vm = wadahViewModel { JadwalViewModel(it.jadwal, it.aktivitas) }
    val tanggal by vm.tanggal.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf<FormKegiatan?>(null) }
    var hapus by remember { mutableStateOf<Kegiatan?>(null) }
    val hariIni = LocalDate.now()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "mode") {
            BarisPilihan(
                opsi = ModeKalender.entries,
                terpilih = mode,
                label = { it.label() },
                onPilih = vm::pilihMode,
            )
        }
        item(key = "navigasi") {
            BarisNavigasi(
                label = NavigasiKalender.label(tanggal, mode),
                onMundur = { vm.geser(-1) },
                onMaju = { vm.geser(1) },
                onHariIni = vm::keHariIni,
            )
        }
        item(key = "tambah") {
            TombolNeo(onClick = { form = FormKegiatan(null) }) { Text("Tambah kegiatan") }
        }
        when (mode) {
            ModeKalender.HARI -> tampilanHari(
                tanggal = tanggal,
                data = data,
                onUbah = { form = FormKegiatan(it) },
            )
            ModeKalender.MINGGU -> tampilanMinggu(
                tanggal = tanggal,
                data = data,
                hariIni = hariIni,
                onBuka = vm::bukaHari,
            )
            ModeKalender.BULAN -> tampilanBulan(
                tanggal = tanggal,
                data = data,
                hariIni = hariIni,
                onBuka = vm::bukaHari,
            )
        }
    }

    form?.let { f ->
        FormKegiatanDialog(
            lama = f.lama,
            tanggalDipilih = tanggal,
            data = data,
            onSimpan = {
                vm.simpanKegiatan(it)
                form = null
            },
            onHapus = {
                hapus = it
            },
            onBatal = { form = null },
        )
    }
    hapus?.let { k ->
        DialogKonfirmasi(
            judul = "Hapus kegiatan?",
            pesan = "\"${k.judul}\" akan dihapus" + if (k.hariMingguan != null) " dari semua minggu." else ".",
            onYa = {
                vm.hapusKegiatan(k.id)
                hapus = null
                form = null
            },
            onBatal = { hapus = null },
        )
    }
}

@Composable
private fun BarisNavigasi(
    label: String,
    onMundur: () -> Unit,
    onMaju: () -> Unit,
    onHariIni: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMundur) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Sebelumnya")
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMaju) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Berikutnya")
            }
        }
        TombolTeksNeo(onClick = onHariIni) { Text("Ke hari ini") }
    }
}

// ---- day view ----

private fun LazyListScope.tampilanHari(
    tanggal: LocalDate,
    data: DataJadwalUi,
    onUbah: (Kegiatan) -> Unit,
) {
    val jam = data.jamSekolah.firstOrNull { it.hari == tanggal.dayOfWeek }
    val piket = data.piket.filter { it.hari == tanggal.dayOfWeek }
    val tenggat = data.tugas.filter { !it.selesai && it.tenggat == tanggal }
    val daftarBlok = PenyusunHarian.susun(tanggal, data.kegiatan, data.pelajaran)

    item(key = "info-sekolah") {
        KartuNeo(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (jam != null) "Sekolah " + Waktu.rentang(jam.masuk, jam.pulang)
                    else "Jam sekolah hari ini belum diatur",
                    style = MaterialTheme.typography.bodyMedium,
                )
                piket.forEach {
                    Text(text = "Piket: " + it.keterangan, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (tenggat.isNotEmpty()) {
        item(key = "tenggat") {
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Tenggat hari ini", style = MaterialTheme.typography.titleSmall)
                    tenggat.forEach {
                        Text(text = it.jenis.label() + ": " + it.judul, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    if (daftarBlok.isEmpty()) {
        item(key = "kosong") {
            TeksPetunjuk("Belum ada kegiatan atau pelajaran pada hari ini.")
        }
    }
    items(daftarBlok, key = { it.kunci }) { blok ->
        val kegiatan = if (blok.sumber == SumberItem.KEGIATAN) {
            data.kegiatan.firstOrNull { "k${it.id}" == blok.kunci }
        } else {
            null
        }
        KartuBlok(blok, onClick = if (kegiatan != null) ({ onUbah(kegiatan) }) else null)
    }
}

@Composable
private fun KartuBlok(blok: ItemHarian, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    KartuNeo(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = Waktu.rentang(blok.mulai, blok.selesai) + "  ·  " + blok.sumber.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = blok.judul, style = MaterialTheme.typography.titleMedium)
            if (blok.keterangan.isNotBlank()) {
                Text(text = blok.keterangan, style = MaterialTheme.typography.bodyMedium)
            }
            blok.prioritas?.let {
                Text(
                    text = "Prioritas " + it.label().lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (blok.bentrok) {
                Text(
                    text = "Waktunya bertabrakan dengan jadwal lain",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (onClick == null) {
                Text(
                    text = "Ubah di tab Jadwal, bagian Sekolah",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---- week view ----

private fun LazyListScope.tampilanMinggu(
    tanggal: LocalDate,
    data: DataJadwalUi,
    hariIni: LocalDate,
    onBuka: (LocalDate) -> Unit,
) {
    items(Kalender.hariDalamMinggu(tanggal), key = { "hari-${it.toEpochDay()}" }) { hari ->
        val daftarBlok = PenyusunHarian.susun(hari, data.kegiatan, data.pelajaran)
        val jumlahTenggat = data.tugas.count { !it.selesai && it.tenggat == hari }
        KartuNeo(modifier = Modifier.fillMaxWidth(), onClick = { onBuka(hari) }) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = NamaHari.untuk(hari.dayOfWeek) + ", " + hari.dayOfMonth,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (hari == hariIni) FontWeight.Black else FontWeight.Bold,
                    color = if (hari == hariIni) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                )
                if (daftarBlok.isEmpty() && jumlahTenggat == 0) {
                    TeksPetunjuk("Kosong")
                } else {
                    daftarBlok.take(3).forEach {
                        Text(
                            text = Waktu.formatJam(it.mulai) + "  " + it.judul,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (daftarBlok.size > 3) {
                        TeksPetunjuk("+${daftarBlok.size - 3} lainnya")
                    }
                    if (jumlahTenggat > 0) {
                        TeksPetunjuk("$jumlahTenggat tenggat tugas")
                    }
                }
            }
        }
    }
}

// ---- month view ----

private fun LazyListScope.tampilanBulan(
    tanggal: LocalDate,
    data: DataJadwalUi,
    hariIni: LocalDate,
    onBuka: (LocalDate) -> Unit,
) {
    item(key = "kepala-hari") {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { hari ->
                Text(
                    text = NamaHari.singkat(hari),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    val baris = Kalender.gridBulan(YearMonth.from(tanggal)).chunked(7)
    items(baris, key = { pekan -> "pekan-" + (pekan.firstNotNullOfOrNull { it?.toEpochDay() } ?: 0L) }) { pekan ->
        Row(modifier = Modifier.fillMaxWidth()) {
            pekan.forEach { hari ->
                val ada = hari != null &&
                    (
                        PenyusunHarian.susun(hari, data.kegiatan, data.pelajaran).isNotEmpty() ||
                            data.tugas.any { !it.selesai && it.tenggat == hari }
                        )
                SelHari(
                    hari = hari,
                    terpilih = hari != null && hari == tanggal,
                    hariIni = hari != null && hari == hariIni,
                    ada = ada,
                    onClick = { if (hari != null) onBuka(hari) },
                )
            }
        }
    }
    item(key = "petunjuk-bulan") {
        TeksPetunjuk("Titik menandakan hari yang punya kegiatan, pelajaran, atau tenggat. Ketuk hari untuk melihat detailnya.")
    }
}

@Composable
private fun RowScope.SelHari(
    hari: LocalDate?,
    terpilih: Boolean,
    hariIni: Boolean,
    ada: Boolean,
    onClick: () -> Unit,
) {
    val neo = LocalNeo.current
    val skema = MaterialTheme.colorScheme
    val bentuk = RoundedCornerShape(8.dp)
    // The chosen day is filled with the accent colour, today is highlighted yellow, other days stay plain.
    val latar = when {
        terpilih -> skema.primary
        hariIni -> skema.secondaryContainer
        else -> skema.surface
    }
    val tinta = latar.tintaKontras()
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (hari != null) {
                    Modifier
                        .background(latar, bentuk)
                        .border(if (terpilih || hariIni) neo.tebal else 1.dp, if (terpilih || hariIni) neo.garis else neo.garis.copy(alpha = 0.3f), bentuk)
                } else {
                    Modifier
                },
            )
            .clip(bentuk)
            .clickable(enabled = hari != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (hari != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hari.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hariIni || terpilih) FontWeight.Black else FontWeight.Bold,
                    color = tinta,
                )
                if (ada) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (terpilih) tinta else skema.tertiary),
                    )
                }
            }
        }
    }
}

// ---- activity form ----

@Composable
private fun FormKegiatanDialog(
    lama: Kegiatan?,
    tanggalDipilih: LocalDate,
    data: DataJadwalUi,
    onSimpan: (Kegiatan) -> Unit,
    onHapus: (Kegiatan) -> Unit,
    onBatal: () -> Unit,
) {
    var draf by remember {
        mutableStateOf(
            if (lama != null) {
                DrafKegiatan(
                    judul = lama.judul,
                    sekali = lama.tanggal != null,
                    tanggalTeks = Waktu.formatTanggal(lama.tanggal ?: tanggalDipilih),
                    hari = lama.hariMingguan ?: tanggalDipilih.dayOfWeek,
                    mulaiTeks = Waktu.formatJam(lama.mulai),
                    selesaiTeks = Waktu.formatJam(lama.selesai),
                    prioritas = lama.prioritas,
                    catatan = lama.catatan,
                )
            } else {
                DrafKegiatan(
                    tanggalTeks = Waktu.formatTanggal(tanggalDipilih),
                    hari = tanggalDipilih.dayOfWeek,
                )
            },
        )
    }
    var galat by remember { mutableStateOf<String?>(null) }
    val id = lama?.id ?: 0L
    val hasil = FormulirJadwal.kegiatan(id, draf)
    val kandidat = (hasil as? Hasil.Ok)?.nilai

    val acuan = kandidat?.tanggalAcuan(tanggalDipilih)
    val bentrok = if (kandidat != null && acuan != null) {
        PenyusunHarian.bentrokDengan(kandidat, acuan, data.kegiatan, data.pelajaran)
    } else {
        emptyList()
    }
    val saran = if (kandidat != null && acuan != null && bentrok.isNotEmpty()) {
        val durasi = Duration.between(kandidat.mulai, kandidat.selesai).toMinutes().toInt()
        val lain = PenyusunHarian.susun(
            acuan,
            data.kegiatan.filter { id == 0L || it.id != id },
            data.pelajaran,
        )
        PenyusunHarian.saranSlot(lain, durasi)?.let { mulai -> mulai to mulai.plusMinutes(durasi.toLong()) }
    } else {
        null
    }

    DialogForm(
        judul = if (lama == null) "Tambah kegiatan" else "Ubah kegiatan",
        onBatal = onBatal,
        onSimpan = {
            when (hasil) {
                is Hasil.Ok -> onSimpan(hasil.nilai)
                is Hasil.Galat -> galat = hasil.pesan
            }
        },
    ) {
        FieldTeks("Judul", draf.judul, { draf = draf.copy(judul = it) })
        BarisPilihan(
            opsi = listOf(true, false),
            terpilih = draf.sekali,
            label = { if (it) "Sekali" else "Setiap minggu" },
            onPilih = { draf = draf.copy(sekali = it) },
        )
        if (draf.sekali) {
            FieldTeks(
                label = "Tanggal",
                nilai = draf.tanggalTeks,
                onUbah = { draf = draf.copy(tanggalTeks = it) },
                petunjuk = "Format HH-BB-TTTT, contoh 02-11-2026",
            )
        } else {
            BarisPilihan(
                opsi = DayOfWeek.entries,
                terpilih = draf.hari,
                label = { NamaHari.untuk(it) },
                onPilih = { draf = draf.copy(hari = it) },
            )
        }
        FieldTeks(
            label = "Jam mulai",
            nilai = draf.mulaiTeks,
            onUbah = { draf = draf.copy(mulaiTeks = it) },
            petunjuk = "Format JJ:MM, contoh 07:30",
        )
        FieldTeks(
            label = "Jam selesai",
            nilai = draf.selesaiTeks,
            onUbah = { draf = draf.copy(selesaiTeks = it) },
            petunjuk = "Format JJ:MM, contoh 09:00",
        )
        Text(text = "Prioritas", style = MaterialTheme.typography.labelLarge)
        BarisPilihan(
            opsi = Prioritas.entries,
            terpilih = draf.prioritas,
            label = { it.label() },
            onPilih = { draf = draf.copy(prioritas = it) },
        )
        FieldTeks("Catatan (opsional)", draf.catatan, { draf = draf.copy(catatan = it) }, singleLine = false)

        if (bentrok.isNotEmpty()) {
            Text(
                text = "Bentrok dengan: " + bentrok.joinToString("; ") {
                    it.judul + " (" + Waktu.rentang(it.mulai, it.selesai) + ")"
                } + ". Kegiatan tetap bisa disimpan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (saran != null) {
                val (mulaiSaran, selesaiSaran) = saran
                Text(
                    text = "Slot kosong terdekat: " + Waktu.rentang(mulaiSaran, selesaiSaran),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TombolTeksNeo(
                    onClick = {
                        draf = draf.copy(
                            mulaiTeks = Waktu.formatJam(mulaiSaran),
                            selesaiTeks = Waktu.formatJam(selesaiSaran),
                        )
                    },
                ) { Text("Pakai slot ini") }
            } else {
                TeksPetunjuk("Tidak ada slot kosong dengan durasi yang sama antara 06:00 dan 22:00 pada hari itu.")
            }
        }
        TeksGalat(galat)
        if (lama != null) {
            TombolTeksNeo(onClick = { onHapus(lama) }) { Text("Hapus kegiatan") }
        }
    }
}
