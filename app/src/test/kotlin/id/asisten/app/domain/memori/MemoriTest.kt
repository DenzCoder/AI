package id.asisten.app.domain.memori

import id.asisten.app.domain.profil.NamaPengguna
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoriTest {

    private val biasa = ItemMemori(1, KategoriMemori.PREFERENSI, "Suka belajar malam")
    private val rahasia = ItemMemori(2, KategoriMemori.KONTEKS, "Hal pribadi", sensitif = true)

    @Test
    fun konteksAiKosongBilaMemoriDimatikan() {
        assertTrue(KonteksAi.pilih(listOf(biasa, rahasia), memoriAktif = false).isEmpty())
        assertTrue(KonteksAi.pilih(listOf(biasa, rahasia), memoriAktif = false, sertakanSensitif = true).isEmpty())
    }

    @Test
    fun konteksAiBawaanTidakMenyertakanDataSensitif() {
        assertEquals(listOf(biasa), KonteksAi.pilih(listOf(biasa, rahasia), memoriAktif = true))
    }

    @Test
    fun dataSensitifHanyaMasukBilaSecaraEksplisitDiminta() {
        assertEquals(
            listOf(biasa, rahasia),
            KonteksAi.pilih(listOf(biasa, rahasia), memoriAktif = true, sertakanSensitif = true),
        )
    }

    @Test
    fun bersihkanMemoriMemangkasSpasiDanPanjang() {
        assertEquals("halo", ValidasiMemori.bersihkan("   halo  "))
        assertEquals(ValidasiMemori.MAKS, ValidasiMemori.bersihkan("x".repeat(ValidasiMemori.MAKS + 50)).length)
        assertEquals("", ValidasiMemori.bersihkan("    "))
    }

    @Test
    fun keempatKategoriMemoriAda() {
        assertEquals(4, KategoriMemori.entries.size)
    }

    @Test
    fun namaPenggunaDirapikan() {
        assertEquals("Budi Santoso", NamaPengguna.bersihkan("  Budi    Santoso  "))
        assertEquals(NamaPengguna.MAKS, NamaPengguna.bersihkan("a".repeat(100)).length)
    }

    @Test
    fun namaPenggunaKosongTidakValid() {
        assertFalse(NamaPengguna.valid(""))
        assertFalse(NamaPengguna.valid("   \t "))
        assertTrue(NamaPengguna.valid(" Rina "))
    }
}
