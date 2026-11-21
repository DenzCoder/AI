package id.asisten.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.asisten.app.domain.hanako.EkspresiHanako
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.profil.NamaPengguna
import id.asisten.app.ui.hanako.HanakoAvatar
import id.asisten.app.ui.komponen.BarisPilihan
import id.asisten.app.ui.komponen.FieldTeks
import id.asisten.app.ui.komponen.JudulBagian
import id.asisten.app.ui.komponen.JudulHalaman
import id.asisten.app.ui.komponen.TeksPetunjuk
import id.asisten.app.ui.komponen.TombolNeo
import id.asisten.app.ui.komponen.TombolTeksNeo

private const val JUMLAH_LANGKAH = 5
private const val LANGKAH_NAMA = 1

/**
 * First-launch flow. Nothing is required except a preferred name; appearance is applied live,
 * and memory is opt-in (off unless the user switches it on here).
 */
@Composable
fun OnboardingScreen(
    pengaturan: Pengaturan,
    onModeTema: (ModeTema) -> Unit,
    onAksen: (Aksen) -> Unit,
    onSelesai: (nama: String, memoriAktif: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var langkah by rememberSaveable { mutableStateOf(0) }
    var nama by rememberSaveable { mutableStateOf("") }
    var memori by rememberSaveable { mutableStateOf(false) }
    val namaValid = NamaPengguna.valid(nama)

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HanakoAvatar(
            ekspresi = when (langkah) {
                0 -> EkspresiHanako.SENYUM
                JUMLAH_LANGKAH - 1 -> EkspresiHanako.TERTAWA
                3 -> EkspresiHanako.BERPIKIR
                else -> EkspresiHanako.NORMAL
            },
            lebar = 96.dp,
        )
        Text(
            text = "Langkah ${langkah + 1} dari $JUMLAH_LANGKAH",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (langkah) {
                0 -> {
                    JudulHalaman("Halo, aku Hanako!")
                    Text(
                        text = "Aku akan menemanimu di ASISTEN, aplikasi untuk mengatur jadwal, tugas, " +
                            "dan kebiasaan sehari-hari. Kamu tidak perlu membuat akun. " +
                            "Semua datamu disimpan di HP ini.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                1 -> {
                    JudulHalaman("Aku harus panggil kamu siapa?")
                    FieldTeks(
                        label = "Nama panggilan",
                        nilai = nama,
                        onUbah = { nama = it.take(NamaPengguna.MAKS) },
                        petunjuk = "Maksimal ${NamaPengguna.MAKS} huruf. Bisa diubah nanti di Profil.",
                    )
                }

                2 -> {
                    JudulHalaman("Atur tampilan")
                    JudulBagian("Tema")
                    BarisPilihan(
                        opsi = ModeTema.entries,
                        terpilih = pengaturan.modeTema,
                        label = { labelTema(it) },
                        onPilih = onModeTema,
                    )
                    JudulBagian("Warna aksen")
                    BarisPilihan(
                        opsi = Aksen.entries,
                        terpilih = pengaturan.aksen,
                        label = { labelAksen(it) },
                        onPilih = onAksen,
                    )
                    TeksPetunjuk("Pengaturan lain, seperti ukuran teks dan animasi, ada di tab Profil.")
                }

                3 -> {
                    JudulHalaman("Privasi dan memori")
                    Text(
                        text = "Datamu tersimpan di perangkat ini. Isi jurnal tidak akan dikirim ke AI " +
                            "kecuali kamu sendiri yang memintanya.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Hanako bisa mengingat hal yang kamu izinkan, misalnya preferensi dan rutinitas, " +
                            "supaya saran darinya lebih cocok. Kamu bisa melihat, mengubah, atau menghapus " +
                            "ingatan itu kapan saja di Profil, bagian Memori Hanako.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Izinkan Hanako mengingat",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = memori, onCheckedChange = { memori = it })
                    }
                    TeksPetunjuk("Bawaannya mati. Fitur percakapan AI menyusul di pembaruan berikutnya, tetapi pilihan ini sudah berlaku.")
                }

                else -> {
                    JudulHalaman("Hampir selesai")
                    Text(
                        text = "Jadwal sekolah bisa kamu atur kapan saja di tab Jadwal, bagian Sekolah. " +
                            "Pengaturan sholat dan kebiasaan akan hadir di pembaruan berikutnya.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (langkah > 0) {
                TombolTeksNeo(onClick = { langkah -= 1 }) { Text("Kembali") }
            } else {
                Text("")
            }
            val terakhir = langkah == JUMLAH_LANGKAH - 1
            TombolNeo(
                onClick = { if (terakhir) onSelesai(nama, memori) else langkah += 1 },
                enabled = langkah != LANGKAH_NAMA || namaValid,
            ) {
                Text(if (terakhir) "Mulai" else "Lanjut")
            }
        }
    }
}

private fun labelTema(mode: ModeTema): String = when (mode) {
    ModeTema.SISTEM -> "Ikuti sistem"
    ModeTema.TERANG -> "Terang"
    ModeTema.GELAP -> "Gelap"
}

private fun labelAksen(aksen: Aksen): String = when (aksen) {
    Aksen.JINGGA -> "Jingga"
    Aksen.UNGU -> "Ungu"
    Aksen.HIJAU -> "Hijau"
    Aksen.BIRU -> "Biru"
}
