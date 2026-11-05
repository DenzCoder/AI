package id.asisten.app.ui.profil

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.asisten.app.R
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.pengaturan.UkuranTeks
import id.asisten.app.domain.profil.NamaPengguna
import id.asisten.app.ui.komponen.DialogForm
import id.asisten.app.ui.komponen.FieldTeks
import id.asisten.app.ui.komponen.TeksGalat
import id.asisten.app.ui.memori.MemoriScreen
import id.asisten.app.ui.pengingat.PengingatScreen

@Composable
fun ProfilScreen(
    pengaturan: Pengaturan,
    namaPengguna: String,
    memoriAktif: Boolean,
    onModeTema: (ModeTema) -> Unit,
    onAksen: (Aksen) -> Unit,
    onUkuranTeks: (UkuranTeks) -> Unit,
    onAnimasi: (Boolean) -> Unit,
    onSimpanNama: (String) -> Unit,
    onMemoriAktif: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tampilMemori by rememberSaveable { mutableStateOf(false) }
    var tampilPengingat by rememberSaveable { mutableStateOf(false) }
    var ubahNama by remember { mutableStateOf(false) }

    BackHandler(enabled = tampilMemori) { tampilMemori = false }
    BackHandler(enabled = tampilPengingat) { tampilPengingat = false }

    if (tampilPengingat) {
        PengingatScreen(onKembali = { tampilPengingat = false }, modifier = modifier)
        return
    }

    if (tampilMemori) {
        MemoriScreen(
            memoriAktif = memoriAktif,
            onMemoriAktif = onMemoriAktif,
            onKembali = { tampilMemori = false },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.profil_judul),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Bagian(R.string.profil_nama) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = namaPengguna.ifBlank { stringResource(R.string.profil_nama_kosong) },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { ubahNama = true }) {
                    Text(stringResource(R.string.profil_ubah))
                }
            }
        }

        Bagian(R.string.profil_memori) {
            Text(
                text = stringResource(
                    if (memoriAktif) R.string.profil_memori_aktif else R.string.profil_memori_mati,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { tampilMemori = true }) {
                Text(stringResource(R.string.profil_kelola_memori))
            }
        }

        Bagian(R.string.profil_pengingat) {
            Text(
                text = stringResource(R.string.profil_pengingat_ket),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { tampilPengingat = true }) {
                Text(stringResource(R.string.profil_kelola_pengingat))
            }
        }

        Bagian(R.string.profil_tema) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeTema.entries.forEach { mode ->
                    FilterChip(
                        selected = pengaturan.modeTema == mode,
                        onClick = { onModeTema(mode) },
                        label = { Text(stringResource(mode.label())) },
                    )
                }
            }
        }

        Bagian(R.string.profil_aksen) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Aksen.entries.forEach { aksen ->
                    PilihanAksen(
                        aksen = aksen,
                        terpilih = pengaturan.aksen == aksen,
                        onClick = { onAksen(aksen) },
                    )
                }
            }
        }

        Bagian(R.string.profil_ukuran_teks) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UkuranTeks.entries.forEach { ukuran ->
                    FilterChip(
                        selected = pengaturan.ukuranTeks == ukuran,
                        onClick = { onUkuranTeks(ukuran) },
                        label = { Text(stringResource(ukuran.label())) },
                    )
                }
            }
        }

        Bagian(R.string.profil_animasi) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.profil_animasi_ket),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = pengaturan.animasiAktif, onCheckedChange = onAnimasi)
            }
        }
    }

    if (ubahNama) {
        DialogUbahNama(
            awal = namaPengguna,
            onSimpan = {
                onSimpanNama(it)
                ubahNama = false
            },
            onBatal = { ubahNama = false },
        )
    }
}

@Composable
private fun DialogUbahNama(awal: String, onSimpan: (String) -> Unit, onBatal: () -> Unit) {
    var nama by remember { mutableStateOf(awal) }
    var galat by remember { mutableStateOf<String?>(null) }
    DialogForm(
        judul = stringResource(R.string.profil_nama),
        onBatal = onBatal,
        onSimpan = {
            if (NamaPengguna.valid(nama)) onSimpan(nama) else galat = "Nama tidak boleh kosong."
        },
    ) {
        FieldTeks(
            label = stringResource(R.string.profil_nama),
            nilai = nama,
            onUbah = { nama = it.take(NamaPengguna.MAKS) },
        )
        TeksGalat(galat)
    }
}

@Composable
private fun Bagian(
    @StringRes judul: Int,
    isi: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(judul),
            style = MaterialTheme.typography.titleMedium,
        )
        isi()
    }
}

@Composable
private fun PilihanAksen(
    aksen: Aksen,
    terpilih: Boolean,
    onClick: () -> Unit,
) {
    val warna = Color(aksen.argb)
    val nama = stringResource(aksen.label())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(warna)
                .clickable(role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (terpilih) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.terpilih),
                    tint = if (warna.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White,
                )
            }
        }
        Text(text = nama, style = MaterialTheme.typography.labelMedium)
    }
}

@StringRes
private fun ModeTema.label(): Int = when (this) {
    ModeTema.SISTEM -> R.string.tema_sistem
    ModeTema.TERANG -> R.string.tema_terang
    ModeTema.GELAP -> R.string.tema_gelap
}

@StringRes
private fun UkuranTeks.label(): Int = when (this) {
    UkuranTeks.KECIL -> R.string.teks_kecil
    UkuranTeks.NORMAL -> R.string.teks_normal
    UkuranTeks.BESAR -> R.string.teks_besar
}

@StringRes
private fun Aksen.label(): Int = when (this) {
    Aksen.JINGGA -> R.string.aksen_jingga
    Aksen.UNGU -> R.string.aksen_ungu
    Aksen.HIJAU -> R.string.aksen_hijau
    Aksen.BIRU -> R.string.aksen_biru
}
