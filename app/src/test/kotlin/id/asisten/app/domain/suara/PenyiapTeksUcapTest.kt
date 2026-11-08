package id.asisten.app.domain.suara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenyiapTeksUcapTest {

    @Test
    fun teksBiasaTidakBerubahKecualiTitikAkhirDitambahkan() {
        assertEquals("Halo, apa kabar?", PenyiapTeksUcap.siapkan("Halo, apa kabar?"))
        assertEquals("Siap.", PenyiapTeksUcap.siapkan("Siap"))
    }

    @Test
    fun rentangJamDiucapkanDenganKataSampai() {
        assertEquals("Matematika 7 sampai 8 lewat 30.", PenyiapTeksUcap.siapkan("Matematika 07:00–08:30"))
        assertEquals("Fisika 9 lewat 15 sampai 10 lewat 45.", PenyiapTeksUcap.siapkan("Fisika 09:15 - 10:45"))
    }

    @Test
    fun jamTunggalDiucapkanTanpaTitikDua() {
        assertEquals("Sekarang pukul 16 lewat 20.", PenyiapTeksUcap.siapkan("Sekarang pukul 16:20"))
        assertEquals("Bangun pukul 5.", PenyiapTeksUcap.siapkan("Bangun pukul 05:00"))
    }

    @Test
    fun daftarBerpoinMenjadiKalimatTerpisah() {
        val hasil = PenyiapTeksUcap.siapkan("Jadwalmu hari ini:\n• 07:00–08:30 Matematika\n• 09:00–10:00 Fisika (Pak Budi)\n- Olahraga\n1. Makan")
        assertEquals(
            "Jadwalmu hari ini: 7 sampai 8 lewat 30 Matematika. 9 sampai 10 Fisika (Pak Budi). Olahraga. Makan.",
            hasil,
        )
        assertFalse(hasil.contains("•"))
    }

    @Test
    fun tandaMarkdownDibuang() {
        assertEquals("Ini penting dan kode.", PenyiapTeksUcap.siapkan("## Ini **penting** dan `kode`"))
        assertEquals("nama variabel.", PenyiapTeksUcap.siapkan("nama_variabel"))
        assertEquals("miring.", PenyiapTeksUcap.siapkan("_miring_"))
        assertEquals("Satu. Dua.", PenyiapTeksUcap.siapkan("* Satu\n* Dua"))
    }

    @Test
    fun emojiDibuangTanpaMerusakHurufBerAksen() {
        assertEquals("Semangat ya! Café.", PenyiapTeksUcap.siapkan("Semangat ya! 😀👍 Café"))
        assertEquals("Hore.", PenyiapTeksUcap.siapkan("Hore ❤️"))
        assertEquals("Hore.", PenyiapTeksUcap.siapkan("Hore ✨"))
    }

    @Test
    fun alamatWebDiucapkanSebagaiTautan() {
        assertEquals("Buka tautan sekarang.", PenyiapTeksUcap.siapkan("Buka https://aistudio.google.com/apikey?x=1 sekarang"))
    }

    @Test
    fun tandaBacaPanjangDanPersenDiganti() {
        assertEquals("Selesai, lalu istirahat.", PenyiapTeksUcap.siapkan("Selesai — lalu istirahat"))
        assertEquals("Kemajuan 50 persen.", PenyiapTeksUcap.siapkan("Kemajuan 50%"))
    }

    @Test
    fun kosongAtauHanyaSimbolMenghasilkanTeksKosong() {
        assertEquals("", PenyiapTeksUcap.siapkan(""))
        assertEquals("", PenyiapTeksUcap.siapkan("  \n 😀 "))
    }

    @Test
    fun teksPanjangDipotongDiBatasKalimat() {
        val kalimat = "Ini kalimat contoh yang cukup panjang. "
        val hasil = PenyiapTeksUcap.siapkan(kalimat.repeat(200))
        assertTrue(hasil.length <= PenyiapTeksUcap.MAKS_PANJANG)
        assertTrue(hasil.endsWith("panjang."))
    }

    @Test
    fun teksPanjangTanpaTitikTetapDipotongAman() {
        val hasil = PenyiapTeksUcap.siapkan("a".repeat(10_000))
        assertTrue(hasil.length <= PenyiapTeksUcap.MAKS_PANJANG + 1)
    }
}
