package id.asisten.app.ui.pengingat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.hanako.PilihEkspresi
import id.asisten.app.domain.pengingat.GeserMenit
import id.asisten.app.domain.pengingat.LabelPengingat
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PilihanPengingat
import id.asisten.app.domain.pengingat.PenyusunNotifikasi
import id.asisten.app.notifikasi.CekIzin
import id.asisten.app.ui.hanako.HanakoAvatar
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.JudulHalaman
import id.asisten.app.ui.komponen.JudulBagian
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.wadahViewModel
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Reminder and mode settings. Everything here is real: the permission status is read from the system,
 * the upcoming list is computed by the same logic the alarm uses, and the test button posts through the same channels.
 */
@Composable
fun PengingatScreen(
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = wadahViewModel { PengingatViewModel(it.pengaturan, it.penjadwal, it.suaraPengingat, it.pengelolaSuara) }
    val pengaturan by vm.pengaturan.collectAsStateWithLifecycle()
    val berikutnya by vm.berikutnya.collectAsStateWithLifecycle()
    val hasilUji by vm.hasilUji.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notifikasiOk by remember { mutableStateOf(CekIzin.notifikasiDiizinkan(context)) }
    var alarmOk by remember { mutableStateOf(CekIzin.alarmPresisiDiizinkan(context)) }
    var pesan by remember { mutableStateOf<String?>(null) }
    var sekarang by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Permissions are changed in system settings, so they are read again whenever the screen comes back.
    val segarkanIzin = {
        notifikasiOk = CekIzin.notifikasiDiizinkan(context)
        alarmOk = CekIzin.alarmPresisiDiizinkan(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        segarkanIzin()
        sekarang = System.currentTimeMillis()
    }
    // Keeps the remaining time of Fokus/Istirahat up to date while the screen is open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            sekarang = System.currentTimeMillis()
        }
    }
    val peluncurIzin = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        segarkanIzin()
    }

    val p = pengaturan ?: return
    val zona = ZoneId.systemDefault()
    val modeAktif = p.modeEfektif(sekarang)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onKembali) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            JudulHalaman("Pengingat dan mode")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HanakoAvatar(ekspresi = PilihEkspresi.untukPengingat(modeAktif, notifikasiOk), lebar = 72.dp)
                Text(
                    text = LabelPengingat.sapaanHanako(p, sekarang, notifikasiOk),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        BagianPengingat("Izin") {
            if (notifikasiOk) {
                TeksPetunjuk("Notifikasi diizinkan.")
            } else {
                TeksPetunjuk("Notifikasi dimatikan untuk ASISTEN. Pengingat tidak akan terlihat sampai kamu mengizinkannya.")
                if (CekIzin.perluMintaIzinNotifikasi()) {
                    Button(onClick = { peluncurIzin.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Izinkan notifikasi")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        pesan = if (bukaHalaman(context, intent)) null else PESAN_GAGAL_BUKA
                    },
                ) { Text("Buka pengaturan notifikasi") }
            }
            if (alarmOk) {
                TeksPetunjuk("Alarm presisi diizinkan: pengingat berbunyi tepat waktu.")
            } else {
                TeksPetunjuk(
                    "Alarm presisi belum diizinkan. Pengingat tetap berjalan, tetapi bisa terlambat beberapa menit.",
                )
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}"),
                        )
                        pesan = if (bukaHalaman(context, intent)) null else PESAN_GAGAL_BUKA
                    },
                ) { Text("Izinkan alarm presisi") }
            }
            TeksPetunjuk(
                "Jika pengingat sering terlambat atau tidak muncul, izinkan ASISTEN berjalan di latar belakang " +
                    "dan matikan penghematan baterainya (di realme: Pengaturan > Baterai).",
            )
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    pesan = if (bukaHalaman(context, intent)) null else PESAN_GAGAL_BUKA
                },
            ) { Text("Buka pengaturan baterai") }
            pesan?.let { TeksPetunjuk(it) }
        }

        BagianPengingat("Pengingat") {
            BarisSakelar("Aktifkan pengingat", p.aktif) { aktif -> vm.ubah { it.copy(aktif = aktif) } }
        }

        if (p.aktif) {
            BagianPengingat("Kegiatan") {
                TeksPetunjuk("Kapan Hanako mengingatkan sebelum kegiatan di Jadwal dimulai.")
                BarisPilihan(
                    opsi = PilihanPengingat.SEBELUM_KEGIATAN,
                    terpilih = p.menitSebelumKegiatan,
                    label = { LabelPengingat.selang(it) },
                    onPilih = { menit -> vm.ubah { it.copy(menitSebelumKegiatan = menit) } },
                )
            }

            BagianPengingat("Tugas dan ujian") {
                BarisSakelar("Ingatkan tenggat tugas", p.tugasAktif) { aktif -> vm.ubah { it.copy(tugasAktif = aktif) } }
                if (p.tugasAktif) {
                    TeksPetunjuk("Dua pengingat untuk tiap tugas yang belum selesai dan punya tenggat.")
                    PemilihJam("Sore sebelum tenggat", p.menitTugasSebelum) { menit ->
                        vm.ubah { it.copy(menitTugasSebelum = menit) }
                    }
                    PemilihJam("Pagi hari tenggat", p.menitTugasHariH) { menit ->
                        vm.ubah { it.copy(menitTugasHariH = menit) }
                    }
                }
            }

            BagianPengingat("Sekolah") {
                BarisSakelar("Ingatkan persiapan sekolah", p.sekolahAktif) { aktif ->
                    vm.ubah { it.copy(sekolahAktif = aktif) }
                }
                if (p.sekolahAktif) {
                    TeksPetunjuk(
                        "Pengingat sebelum jam masuk di hari yang punya jam sekolah. Piket hari itu ikut disebut. " +
                            "Belum ada kalender libur, jadi hari libur tetap diingatkan.",
                    )
                    BarisPilihan(
                        opsi = PilihanPengingat.SEBELUM_SEKOLAH,
                        terpilih = p.menitSebelumSekolah,
                        label = { LabelPengingat.selang(it) },
                        onPilih = { menit -> vm.ubah { it.copy(menitSebelumSekolah = menit) } },
                    )
                }
            }

            BagianPengingat("Tunda") {
                TeksPetunjuk("Lama tunda saat kamu menekan Tunda di notifikasi.")
                BarisPilihan(
                    opsi = PilihanPengingat.TUNDA,
                    terpilih = p.tundaMenit,
                    label = { LabelPengingat.durasi(it) },
                    onPilih = { menit -> vm.ubah { it.copy(tundaMenit = menit) } },
                )
            }

            BagianPengingat("Jam tenang") {
                BarisSakelar("Aktifkan jam tenang", p.jamTenangAktif) { aktif ->
                    vm.ubah { it.copy(jamTenangAktif = aktif) }
                }
                TeksPetunjuk("Pada jam tenang pengingat tetap muncul, tetapi tanpa suara dan getar.")
                if (p.jamTenangAktif) {
                    PemilihJam("Mulai", p.jamTenangMulai) { menit -> vm.ubah { it.copy(jamTenangMulai = menit) } }
                    PemilihJam("Selesai", p.jamTenangSelesai) { menit -> vm.ubah { it.copy(jamTenangSelesai = menit) } }
                    if (p.jamTenangMulai == p.jamTenangSelesai) {
                        TeksPetunjuk("Jam mulai dan selesai sama, jadi jam tenang tidak berlaku.")
                    }
                }
            }
        }

        BagianPengingat("Mode") {
            TeksPetunjuk(LabelPengingat.statusMode(p, sekarang))
            TeksPetunjuk("Fokus: hanya pengingat berprioritas Tinggi yang berbunyi, yang lain muncul tanpa suara.")
            BarisTombol {
                PilihanPengingat.DURASI_FOKUS.forEach { menit ->
                    OutlinedButton(onClick = { vm.mulaiMode(ModePengingat.FOKUS, menit) }) {
                        Text("Fokus ${LabelPengingat.durasi(menit)}")
                    }
                }
            }
            TeksPetunjuk("Istirahat: semua pengingat muncul tanpa suara.")
            BarisTombol {
                PilihanPengingat.DURASI_ISTIRAHAT.forEach { menit ->
                    OutlinedButton(onClick = { vm.mulaiMode(ModePengingat.ISTIRAHAT, menit) }) {
                        Text("Istirahat ${LabelPengingat.durasi(menit)}")
                    }
                }
            }
            if (modeAktif != ModePengingat.NORMAL) {
                Button(onClick = { vm.akhiriMode() }) { Text("Akhiri mode sekarang") }
            }
        }

        BagianPengingat("Pengingat berikutnya") {
            if (berikutnya.isEmpty()) {
                TeksPetunjuk(
                    if (p.aktif) {
                        "Belum ada pengingat dalam 14 hari ke depan. Tambahkan kegiatan di Jadwal atau tugas dengan tenggat di Aktivitas."
                    } else {
                        "Pengingat sedang dimatikan."
                    },
                )
            } else {
                berikutnya.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = PenyusunNotifikasi.labelWaktu(item.pengingat.waktu, zona),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = item.judul, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = item.isi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        BagianSuaraPengingat(vm)

        BagianPengingat("Uji notifikasi") {
            TeksPetunjuk("Kirim satu notifikasi sekarang untuk memeriksa izin, suara, dan mode yang sedang aktif.")
            OutlinedButton(onClick = { vm.kirimUji(CekIzin.notifikasiDiizinkan(context)) }) {
                Text("Kirim notifikasi uji")
            }
            hasilUji?.let { TeksPetunjuk(it) }
        }
    }
}

private const val PESAN_GAGAL_BUKA = "Halaman pengaturan itu tidak ditemukan di perangkat ini. Buka lewat Pengaturan HP secara manual."

private fun bukaHalaman(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}

@Composable
internal fun BagianPengingat(judul: String, isi: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JudulBagian(judul)
        isi()
    }
}

@Composable
internal fun BarisSakelar(teks: String, aktif: Boolean, onUbah: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = teks, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = aktif, onCheckedChange = onUbah)
    }
}

/** Time of day in 30-minute steps; wraps around midnight. */
@Composable
private fun PemilihJam(label: String, menit: Int, onUbah: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { onUbah(GeserMenit.mundur(menit)) }) { Text("-30 mnt") }
            Text(text = GeserMenit.format(menit), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onUbah(GeserMenit.maju(menit)) }) { Text("+30 mnt") }
        }
    }
}

/** Buttons in one row that scrolls sideways on narrow screens. */
@Composable
private fun BarisTombol(isi: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        isi()
    }
}
