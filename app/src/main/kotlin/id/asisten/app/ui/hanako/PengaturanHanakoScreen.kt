package id.asisten.app.ui.hanako

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.R
import id.asisten.app.domain.hanako.LabelHanako
import id.asisten.app.domain.suara.LabelSuara
import id.asisten.app.domain.suara.PilihanSuara
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.DialogKonfirmasi
import id.asisten.app.ui.komponen.JudulBagian
import id.asisten.app.ui.komponen.JudulHalaman
import id.asisten.app.ui.komponen.TeksPetunjuk

private const val ALAMAT_STUDIO = "https://aistudio.google.com/apikey"

/** Hanako's settings. Everything shown is real state; the key field is never kept in saved instance state. */
@Composable
fun PengaturanHanakoScreen(vm: HanakoViewModel, onKembali: () -> Unit, modifier: Modifier = Modifier) {
    val pengaturan by vm.pengaturanHanako.collectAsStateWithLifecycle()
    val hasilUji by vm.hasilUji.collectAsStateWithLifecycle()
    val pesanKunci by vm.pesanKunci.collectAsStateWithLifecycle()
    val penanganUri = LocalUriHandler.current
    val konteks = LocalContext.current
    val pengaturanSuara by vm.pengaturanSuara.collectAsStateWithLifecycle()
    val statusSuara by vm.statusSuara.collectAsStateWithLifecycle()
    // Starting the speech engine tells us whether Indonesian is available; it is released when the chat is closed.
    LaunchedEffect(Unit) { vm.periksaSuara() }

    // Not rememberSaveable on purpose: the key must not end up in the saved instance state.
    var kunciInput by remember { mutableStateOf("") }
    var setuju by rememberSaveable { mutableStateOf(false) }
    var konfirmasiRiwayat by rememberSaveable { mutableStateOf(false) }
    var konfirmasiKunci by rememberSaveable { mutableStateOf(false) }

    val p = pengaturan ?: return
    var modelInput by remember(p.model) { mutableStateOf(p.model) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onKembali) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.hanako_kembali))
            }
            JudulHalaman(stringResource(R.string.hanako_set_judul))
        }

        // --- Gemini key ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JudulBagian(stringResource(R.string.hanako_set_ai_judul))
            TeksPetunjuk(stringResource(R.string.hanako_set_ai_ket))
            Text(
                text = stringResource(if (p.adaKunci) R.string.hanako_set_kunci_tersimpan else R.string.hanako_set_kunci_belum),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = kunciInput,
                onValueChange = {
                    kunciInput = it.take(300)
                    vm.tutupPesanKunci()
                },
                label = { Text(stringResource(R.string.hanako_set_kunci_label)) },
                placeholder = { Text(stringResource(R.string.hanako_set_kunci_petunjuk)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = setuju, onCheckedChange = { setuju = it })
                Text(
                    text = stringResource(R.string.hanako_set_setuju),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            pesanKunci?.let { pesan ->
                Text(
                    text = stringResource(
                        when (pesan) {
                            PesanKunci.DISIMPAN -> R.string.hanako_set_kunci_disimpan
                            PesanKunci.DITOLAK -> R.string.hanako_set_kunci_ditolak
                            PesanKunci.PERLU_SETUJU -> R.string.hanako_set_perlu_setuju
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pesan == PesanKunci.DISIMPAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        vm.simpanKunci(kunciInput, setuju)
                        kunciInput = ""
                    },
                    enabled = kunciInput.isNotBlank(),
                ) { Text(stringResource(R.string.hanako_set_simpan)) }
                if (p.adaKunci) {
                    OutlinedButton(onClick = { konfirmasiKunci = true }) {
                        Text(stringResource(R.string.hanako_set_hapus_kunci))
                    }
                }
            }
            // openUri throws when the phone has no browser, so the call is guarded.
            TextButton(onClick = { runCatching { penanganUri.openUri(ALAMAT_STUDIO) } }) {
                Text(stringResource(R.string.hanako_set_buka_studio))
            }
        }

        // --- switches ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JudulBagian(stringResource(R.string.hanako_set_perilaku))
            BarisSakelar(
                judul = stringResource(R.string.hanako_set_online),
                keterangan = stringResource(R.string.hanako_set_online_ket),
                nyala = p.onlineAktif && p.adaKunci,
                aktif = p.adaKunci,
                onUbah = vm::ubahOnline,
            )
            BarisSakelar(
                judul = stringResource(R.string.hanako_set_bagikan),
                keterangan = stringResource(R.string.hanako_set_bagikan_ket),
                nyala = p.bagikanData,
                aktif = true,
                onUbah = vm::ubahBagikanData,
            )
        }

        // --- model + connection test ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JudulBagian(stringResource(R.string.hanako_set_model))
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it.take(80) },
                label = { Text(stringResource(R.string.hanako_set_model_label)) },
                supportingText = { Text(stringResource(R.string.hanako_set_model_petunjuk)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.ubahModel(modelInput) },
                    enabled = modelInput.isNotBlank() && modelInput.trim() != p.model,
                ) { Text(stringResource(R.string.hanako_set_model_simpan)) }
                Button(
                    onClick = { vm.ujiKoneksi() },
                    enabled = p.adaKunci && hasilUji != HasilUji.Memproses,
                ) { Text(stringResource(R.string.hanako_set_uji)) }
            }
            when (val h = hasilUji) {
                null -> Unit
                HasilUji.Memproses -> Text(stringResource(R.string.hanako_set_uji_proses), style = MaterialTheme.typography.bodyMedium)
                HasilUji.Berhasil -> Text(
                    text = stringResource(R.string.hanako_set_uji_berhasil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                is HasilUji.Gagal -> Text(
                    text = LabelHanako.galat(h.jenis) + if (h.rinci.isNotBlank()) " (${h.rinci})" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // --- voice ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JudulBagian(stringResource(R.string.hanako_suara_judul))
            TeksPetunjuk(stringResource(R.string.hanako_suara_ket))
            BarisSakelar(
                judul = stringResource(R.string.hanako_suara_otomatis),
                keterangan = stringResource(R.string.hanako_suara_otomatis_ket),
                nyala = pengaturanSuara.bacaOtomatis,
                aktif = true,
                onUbah = vm::ubahBacaOtomatis,
            )
            Text(stringResource(R.string.hanako_suara_kecepatan), style = MaterialTheme.typography.bodyLarge)
            BarisPilihan(
                opsi = PilihanSuara.KECEPATAN.map { it.first },
                terpilih = PilihanSuara.terdekat(pengaturanSuara.kecepatan, PilihanSuara.KECEPATAN),
                label = { v -> PilihanSuara.KECEPATAN.first { it.first == v }.second },
                onPilih = vm::ubahKecepatan,
            )
            Text(stringResource(R.string.hanako_suara_nada), style = MaterialTheme.typography.bodyLarge)
            BarisPilihan(
                opsi = PilihanSuara.NADA.map { it.first },
                terpilih = PilihanSuara.terdekat(pengaturanSuara.nada, PilihanSuara.NADA),
                label = { v -> PilihanSuara.NADA.first { it.first == v }.second },
                onPilih = vm::ubahNada,
            )
            statusSuara?.let { Text(LabelSuara.status(it), style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.bacaKeras(konteks.getString(R.string.hanako_suara_contoh)) }) {
                    Text(stringResource(R.string.hanako_suara_uji))
                }
                if (statusSuara?.let { LabelSuara.bisaPasangData(it) } == true) {
                    OutlinedButton(
                        onClick = {
                            try {
                                konteks.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (e: ActivityNotFoundException) {
                                // No installer on this phone: the status text above already explains what is missing.
                            }
                        },
                    ) { Text(stringResource(R.string.hanako_suara_pasang)) }
                }
            }
        }

        // --- history ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JudulBagian(stringResource(R.string.hanako_set_riwayat))
            TeksPetunjuk(stringResource(R.string.hanako_set_riwayat_ket))
            OutlinedButton(onClick = { konfirmasiRiwayat = true }) {
                Text(stringResource(R.string.hanako_set_hapus_riwayat))
            }
        }
    }

    if (konfirmasiRiwayat) {
        DialogKonfirmasi(
            judul = stringResource(R.string.hanako_set_hapus_riwayat),
            pesan = stringResource(R.string.hanako_set_hapus_riwayat_tanya),
            onYa = {
                vm.hapusRiwayat()
                konfirmasiRiwayat = false
            },
            onBatal = { konfirmasiRiwayat = false },
        )
    }
    if (konfirmasiKunci) {
        DialogKonfirmasi(
            judul = stringResource(R.string.hanako_set_hapus_kunci),
            pesan = stringResource(R.string.hanako_set_hapus_kunci_tanya),
            onYa = {
                vm.hapusKunci()
                konfirmasiKunci = false
            },
            onBatal = { konfirmasiKunci = false },
        )
    }
}

@Composable
private fun BarisSakelar(
    judul: String,
    keterangan: String,
    nyala: Boolean,
    aktif: Boolean,
    onUbah: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = judul, style = MaterialTheme.typography.bodyLarge)
            TeksPetunjuk(keterangan)
        }
        Switch(checked = nyala, onCheckedChange = onUbah, enabled = aktif)
    }
}
