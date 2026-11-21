package id.asisten.app.ui.pengingat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.pengingat.DaftarNada
import id.asisten.app.domain.pengingat.LabelSuaraPengingat
import id.asisten.app.domain.pengingat.SuaraPengingat
import id.asisten.app.notifikasi.PemutarContoh
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.komponen.TombolTepiNeo
import id.asisten.app.ui.komponen.TombolTeksNeo

private const val KUNCI_BAWAAN = "bawaan"
private const val KUNCI_SISTEM = "sistem"
private const val KUNCI_BERKAS = "berkas"

/**
 * Choose what the reminder plays: the phone's default, a tone from the app, a tone from the system picker or music
 * from the phone. Every row can be previewed on the stream the notification will use.
 */
@Composable
internal fun BagianSuaraPengingat(vm: PengingatViewModel) {
    val pengaturan by vm.suara.collectAsStateWithLifecycle()
    val pesan by vm.pesanSuara.collectAsStateWithLifecycle()
    val menyalin by vm.sedangMenyalin.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pemutar = remember { PemutarContoh(context) }
    var diputar by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { pemutar.berhenti() } }

    val peluncurNada = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasil ->
        if (hasil.resultCode == Activity.RESULT_OK) {
            val uri = hasil.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            if (uri != null) vm.pilihNadaSistem(uri)
        }
    }
    val peluncurMusik = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pilihMusik(uri)
    }

    val s = pengaturan ?: return
    val suara = s.suara

    fun putar(kunci: String, uri: Uri?) {
        if (diputar == kunci) {
            pemutar.berhenti()
            return
        }
        pemutar.berhenti()
        if (uri == null) {
            vm.laporGagalPutar()
            return
        }
        vm.tutupPesanSuara()
        diputar = kunci
        if (!pemutar.putar(uri, s.volumeAlarm) { diputar = null }) {
            diputar = null
            vm.laporGagalPutar()
        }
    }

    fun bukaPemilihNada() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih nada pengingat")
            (suara as? SuaraPengingat.Sistem)?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it.uri)) }
        }
        try {
            peluncurNada.launch(intent)
        } catch (e: ActivityNotFoundException) {
            vm.laporPemilihTidakAda()
        }
    }

    fun bukaPemilihMusik() {
        try {
            peluncurMusik.launch(arrayOf("audio/*"))
        } catch (e: ActivityNotFoundException) {
            vm.laporPemilihTidakAda()
        }
    }

    BagianPengingat("Suara pengingat") {
        TeksPetunjuk(
            "Suara yang dibunyikan saat pengingat muncul. Saat jam tenang, mode Fokus, atau mode Istirahat, " +
                "pengingat tetap senyap.",
        )
        Text(
            text = "Sekarang: " + LabelSuaraPengingat.ringkas(s) + " (" + LabelSuaraPengingat.jenis(suara).lowercase() + ")",
            style = MaterialTheme.typography.bodyLarge,
        )

        BarisSuara(
            judul = "Bawaan ponsel",
            keterangan = null,
            terpilih = suara == SuaraPengingat.Bawaan,
            sedangDiputar = diputar == KUNCI_BAWAAN,
            bisaPutar = true,
            onPilih = vm::pilihBawaan,
            onPutar = { putar(KUNCI_BAWAAN, Settings.System.DEFAULT_NOTIFICATION_URI) },
        )
        for (nada in DaftarNada.SEMUA) {
            val pilihan = SuaraPengingat.Aplikasi(nada.kode)
            BarisSuara(
                judul = nada.nama,
                keterangan = nada.keterangan,
                terpilih = suara == pilihan,
                sedangDiputar = diputar == nada.kode,
                bisaPutar = true,
                onPilih = { vm.pilihNadaAplikasi(nada.kode) },
                onPutar = { putar(nada.kode, vm.alamatContoh(pilihan)) },
            )
        }
        val sistem = suara as? SuaraPengingat.Sistem
        BarisSuara(
            judul = "Nada sistem",
            keterangan = sistem?.let { "Dipilih: ${it.nama}" } ?: "Pilih dari daftar nada milik Android.",
            terpilih = sistem != null,
            sedangDiputar = diputar == KUNCI_SISTEM,
            bisaPutar = sistem != null,
            onPilih = { if (sistem == null) bukaPemilihNada() },
            onPutar = { putar(KUNCI_SISTEM, vm.alamatContoh(suara)) },
        ) {
            TombolTepiNeo(onClick = ::bukaPemilihNada) { Text(if (sistem == null) "Pilih" else "Ganti") }
        }
        val berkas = suara as? SuaraPengingat.Berkas
        BarisSuara(
            judul = "Musik dari ponsel",
            keterangan = when {
                menyalin -> "Menyalin musik…"
                berkas != null -> "Dipilih: ${berkas.nama}"
                else -> "Pilih lagu atau rekamanmu (mp3, m4a, wav, ogg; maksimal 15 MB). Aplikasi menyimpan salinannya."
            },
            terpilih = berkas != null,
            sedangDiputar = diputar == KUNCI_BERKAS,
            bisaPutar = berkas != null,
            onPilih = { if (berkas == null && !menyalin) bukaPemilihMusik() },
            onPutar = { putar(KUNCI_BERKAS, vm.alamatContoh(suara)) },
        ) {
            TombolTepiNeo(onClick = ::bukaPemilihMusik, enabled = !menyalin) { Text(if (berkas == null) "Pilih" else "Ganti") }
        }

        pesan?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

        BarisSakelar("Bunyi di volume alarm", s.volumeAlarm, vm::ubahVolumeAlarm)
        TeksPetunjuk(
            "Bunyi mengikuti volume alarm, bukan volume notifikasi, jadi lebih mudah terdengar. " +
                "Pengaturan Jangan Ganggu di ponselmu tetap berlaku.",
        )
        TeksPetunjuk(
            "Suara juga bisa diubah lewat pengaturan notifikasi ponsel (saluran \"Pengingat\"). " +
                "Kalau diubah di sana, pengaturan ponsel yang dipakai sampai kamu memilih suara lain di sini.",
        )
    }
}

@Composable
private fun BarisSuara(
    judul: String,
    keterangan: String?,
    terpilih: Boolean,
    sedangDiputar: Boolean,
    bisaPutar: Boolean,
    onPilih: () -> Unit,
    onPutar: () -> Unit,
    aksiTambahan: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = terpilih, onClick = onPilih, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = terpilih, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = judul, style = MaterialTheme.typography.bodyLarge)
            if (keterangan != null) TeksPetunjuk(keterangan)
        }
        if (bisaPutar) TombolTeksNeo(onClick = onPutar) { Text(if (sedangDiputar) "Henti" else "Putar") }
        aksiTambahan?.invoke()
    }
}
