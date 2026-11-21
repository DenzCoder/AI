package id.asisten.app.ui.memori

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.label
import id.asisten.app.domain.memori.ItemMemori
import id.asisten.app.domain.memori.KategoriMemori
import id.asisten.app.domain.memori.ValidasiMemori
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.DialogForm
import id.asisten.app.ui.komponen.DialogKonfirmasi
import id.asisten.app.ui.komponen.FieldTeks
import id.asisten.app.ui.komponen.JudulHalaman
import id.asisten.app.ui.komponen.TeksGalat
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.komponen.TombolHapus
import id.asisten.app.ui.komponen.TombolUbah
import id.asisten.app.ui.wadahViewModel
import id.asisten.app.ui.komponen.TombolNeo
import id.asisten.app.ui.komponen.TombolTepiNeo
import id.asisten.app.ui.komponen.KartuNeo

private class FormMemori(val lama: ItemMemori?)

/**
 * Lets the user see, add, correct and delete everything Hanako may remember, and switch memory off.
 * Switching memory off keeps the list visible so it can still be reviewed or cleared.
 */
@Composable
fun MemoriScreen(
    memoriAktif: Boolean,
    onMemoriAktif: (Boolean) -> Unit,
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = wadahViewModel { MemoriViewModel(it.memori) }
    val daftar by vm.daftar.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf<FormMemori?>(null) }
    var hapus by remember { mutableStateOf<ItemMemori?>(null) }
    var hapusSemua by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "kepala") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onKembali) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                JudulHalaman("Memori Hanako")
            }
        }
        item(key = "saklar") {
            KartuNeo(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Izinkan Hanako mengingat",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = memoriAktif, onCheckedChange = onMemoriAktif)
                    }
                    TeksPetunjuk(
                        if (memoriAktif) {
                            "Memori aktif. Memori yang kamu tandai sensitif tidak ikut dipakai sebagai konteks AI biasa."
                        } else {
                            "Memori mati. Hanako tidak memakai satu pun isi daftar di bawah."
                        },
                    )
                }
            }
        }
        item(key = "aksi") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolNeo(onClick = { form = FormMemori(null) }) { Text("Tambah memori") }
                if (daftar.isNotEmpty()) {
                    TombolTepiNeo(onClick = { hapusSemua = true }) { Text("Hapus semua") }
                }
            }
        }
        if (daftar.isEmpty()) {
            item(key = "kosong") {
                TeksPetunjuk("Belum ada memori. Tambahkan hal yang ingin Hanako ingat, misalnya kebiasaan belajarmu.")
            }
        }
        KategoriMemori.entries.forEach { kategori ->
            val isi = daftar.filter { it.kategori == kategori }
            if (isi.isNotEmpty()) {
                item(key = "judul-${kategori.name}") {
                    Text(
                        text = kategori.label(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                items(isi, key = { "m${it.id}" }) { memori ->
                    KartuMemori(
                        memori = memori,
                        onUbah = { form = FormMemori(memori) },
                        onHapus = { hapus = memori },
                    )
                }
            }
        }
    }

    form?.let { f ->
        FormMemoriDialog(
            lama = f.lama,
            onSimpan = {
                vm.simpan(it)
                form = null
            },
            onBatal = { form = null },
        )
    }
    hapus?.let { m ->
        DialogKonfirmasi(
            judul = "Hapus memori?",
            pesan = "\"${m.isi}\" akan dihapus dari ingatan Hanako.",
            onYa = {
                vm.hapus(m.id)
                hapus = null
            },
            onBatal = { hapus = null },
        )
    }
    if (hapusSemua) {
        DialogKonfirmasi(
            judul = "Hapus semua memori?",
            pesan = "Seluruh isi memori akan dihapus dan tidak bisa dikembalikan.",
            onYa = {
                vm.hapusSemua()
                hapusSemua = false
            },
            onBatal = { hapusSemua = false },
            labelYa = "Hapus semua",
        )
    }
}

@Composable
private fun KartuMemori(memori: ItemMemori, onUbah: () -> Unit, onHapus: () -> Unit) {
    KartuNeo(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = memori.isi, style = MaterialTheme.typography.bodyLarge)
                if (memori.sensitif) {
                    Text(
                        text = "Sensitif: tidak dipakai sebagai konteks AI biasa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            TombolUbah(onUbah, deskripsi = "Ubah memori")
            TombolHapus(onHapus, deskripsi = "Hapus memori")
        }
    }
}

@Composable
private fun FormMemoriDialog(
    lama: ItemMemori?,
    onSimpan: (ItemMemori) -> Unit,
    onBatal: () -> Unit,
) {
    var kategori by remember { mutableStateOf(lama?.kategori ?: KategoriMemori.PREFERENSI) }
    var isi by remember { mutableStateOf(lama?.isi ?: "") }
    var sensitif by remember { mutableStateOf(lama?.sensitif ?: false) }
    var galat by remember { mutableStateOf<String?>(null) }

    DialogForm(
        judul = if (lama == null) "Tambah memori" else "Ubah memori",
        onBatal = onBatal,
        onSimpan = {
            val bersih = ValidasiMemori.bersihkan(isi)
            if (bersih.isEmpty()) {
                galat = "Isi memori tidak boleh kosong."
            } else {
                onSimpan(
                    ItemMemori(
                        id = lama?.id ?: 0L,
                        kategori = kategori,
                        isi = bersih,
                        sensitif = sensitif,
                        dibuat = lama?.dibuat ?: 0L,
                    ),
                )
            }
        },
    ) {
        BarisPilihan(
            opsi = KategoriMemori.entries,
            terpilih = kategori,
            label = { it.label() },
            onPilih = { kategori = it },
        )
        FieldTeks(
            label = "Isi memori",
            nilai = isi,
            onUbah = { isi = it.take(ValidasiMemori.MAKS) },
            singleLine = false,
            petunjuk = "Maksimal ${ValidasiMemori.MAKS} huruf.",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tandai sensitif",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = sensitif, onCheckedChange = { sensitif = it })
        }
        TeksGalat(galat)
    }
}
