package id.asisten.app.domain.hanako

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenjawabLokalUbahTest {

    private val latar = LatarHanako(
        namaPengguna = "Arju",
        sekarang = LocalDateTime.of(2026, 11, 16, 8, 0),
        tugas = listOf(DataUji.tugasPr),
    )

    @Test
    fun permintaanMengubahDijawabJujurBukanDenganDaftar() {
        for (kalimat in listOf(
            "tambah tugas matematika besok", "Tolong hapus tugas PR", "ubah jadwal les jadi jam 5",
            "ingatkan aku jam 7", "pindahkan rapat ke hari jumat", "atur alarm", "batalkan kegiatan sore",
            "buatkan jadwal belajar",
        )) {
            val balasan = PenjawabLokal.jawab(kalimat, latar)
            assertEquals(kalimat, PenjawabLokal.PESAN_UBAH, balasan.teks)
            assertEquals(EkspresiHanako.BINGUNG, balasan.ekspresi)
            assertFalse(kalimat, balasan.teks.contains("PR Matematika"))
        }
    }

    @Test
    fun pesanMenunjukJalanYangNyata() {
        assertTrue(PenjawabLokal.PESAN_UBAH.contains("ikon gerigi"))
        assertTrue(PenjawabLokal.PESAN_UBAH.contains("tab Jadwal atau Aktivitas"))
        assertTrue(PenjawabLokal.PESAN_UBAH.contains("Profil > Pengingat dan mode"))
        assertTrue(PenjawabLokal.PESAN_UBAH.contains("belum bisa mengubah"))
    }

    @Test
    fun pertanyaanBiasaTetapDijawabDariData() {
        val balasan = PenjawabLokal.jawab("tugas apa yang belum selesai?", latar)
        assertTrue(balasan.teks.contains("PR Matematika"))
        assertTrue(PenjawabLokal.jawab("jadwal hari ini", latar).teks.contains("jadwal"))
        assertTrue(PenjawabLokal.jawab("halo", latar).teks.contains("Arju"))
    }

    @Test
    fun kataYangMiripTetapDiperlakukanSebagaiPertanyaanBiasa() {
        // "aturan" dan "tambahan" hanya mirip awalannya; bukan perintah mengubah.
        val aturan = PenjawabLokal.jawab("apa aturan jadwal sekolah?", latar)
        assertFalse(aturan.teks == PenjawabLokal.PESAN_UBAH)
        assertTrue(aturan.teks.contains("jadwal"))
        val tambahan = PenjawabLokal.jawab("ada tugas tambahan?", latar)
        assertFalse(tambahan.teks == PenjawabLokal.PESAN_UBAH)
        assertTrue(tambahan.teks.contains("PR Matematika"))
    }
}
