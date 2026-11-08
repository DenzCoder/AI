package id.asisten.app.domain.suara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PengaturanSuaraTest {

    @Test
    fun bawaanAdalahTidakMembacaOtomatisDenganSuaraNormal() {
        val p = PengaturanSuara()
        assertFalse(p.bacaOtomatis)
        assertEquals(1.0f, p.kecepatan)
        assertEquals(1.0f, p.nada)
    }

    @Test
    fun normalisasiMenjepitNilaiAneh() {
        assertEquals(PengaturanSuara.MIN, PengaturanSuara(kecepatan = 0.01f).normalisasi().kecepatan)
        assertEquals(PengaturanSuara.MAKS, PengaturanSuara(nada = 99f).normalisasi().nada)
        assertEquals(1.0f, PengaturanSuara(kecepatan = Float.NaN).normalisasi().kecepatan)
        assertEquals(1.0f, PengaturanSuara(nada = Float.POSITIVE_INFINITY).normalisasi().nada)
        assertEquals(1.25f, PengaturanSuara(kecepatan = 1.25f).normalisasi().kecepatan)
    }

    @Test
    fun pilihanTerdekatUntukNilaiTersimpanYangTidakPersis() {
        assertEquals(1.0f, PilihanSuara.terdekat(1.02f, PilihanSuara.KECEPATAN))
        assertEquals(0.8f, PilihanSuara.terdekat(0.5f, PilihanSuara.KECEPATAN))
        assertEquals(1.25f, PilihanSuara.terdekat(2.0f, PilihanSuara.KECEPATAN))
        assertEquals(1.15f, PilihanSuara.terdekat(1.2f, PilihanSuara.NADA))
    }

    @Test
    fun semuaPilihanMasukRentangYangDiterimaMesin() {
        for ((nilai, _) in PilihanSuara.KECEPATAN + PilihanSuara.NADA) {
            assertTrue(nilai in PengaturanSuara.MIN..PengaturanSuara.MAKS)
        }
    }

    @Test
    fun statusMesinDariKodeBahasa() {
        assertEquals(StatusSuara.MESIN_GAGAL, StatusSuara.dari(false, 0))
        assertEquals(StatusSuara.SIAP, StatusSuara.dari(true, 0))
        assertEquals(StatusSuara.SIAP, StatusSuara.dari(true, 1))
        assertEquals(StatusSuara.SIAP, StatusSuara.dari(true, 2))
        assertEquals(StatusSuara.DATA_BAHASA_KURANG, StatusSuara.dari(true, StatusSuara.LANG_MISSING_DATA))
        assertEquals(StatusSuara.BAHASA_TIDAK_ADA, StatusSuara.dari(true, StatusSuara.LANG_NOT_SUPPORTED))
        assertEquals(StatusSuara.BAHASA_TIDAK_ADA, StatusSuara.dari(true, -99))
    }

    @Test
    fun labelStatusUnikDanHanyaDataKurangYangBisaDipasang() {
        val label = StatusSuara.entries.map { LabelSuara.status(it) }
        assertEquals(label.size, label.toSet().size)
        assertTrue(label.all { it.isNotBlank() })
        for (s in StatusSuara.entries) assertEquals(s == StatusSuara.DATA_BAHASA_KURANG, LabelSuara.bisaPasangData(s))
    }

    @Test
    fun pilihHasilDiktean() {
        assertNull(PenyisipDiktean.pilihHasil(null))
        assertNull(PenyisipDiktean.pilihHasil(emptyList()))
        assertNull(PenyisipDiktean.pilihHasil(listOf("", "   ")))
        assertEquals("halo dunia", PenyisipDiktean.pilihHasil(listOf("", "  halo dunia ", "lain")))
    }

    @Test
    fun gabungDikteanKeKolomKosongMemakaiHurufKapital() {
        assertEquals("Apa jadwalku hari ini", PenyisipDiktean.gabung("", "apa jadwalku hari ini"))
        assertEquals("Halo", PenyisipDiktean.gabung("   ", " halo "))
    }

    @Test
    fun gabungDikteanSetelahKalimatSelesaiAtauDiTengahKalimat() {
        assertEquals("Halo. Apa kabar", PenyisipDiktean.gabung("Halo.", "apa kabar"))
        assertEquals("Tolong carikan jadwal besok", PenyisipDiktean.gabung("Tolong carikan", "jadwal besok"))
        assertEquals("Tolong carikan jadwal", PenyisipDiktean.gabung("Tolong carikan  ", "jadwal"))
    }

    @Test
    fun dikteanKosongTidakMengubahTeksLama() {
        assertEquals("teks lama", PenyisipDiktean.gabung("teks lama", "   "))
        assertNotEquals("", PenyisipDiktean.gabung("a", "b"))
    }

    @Test
    fun hasilGabunganDibatasi() {
        val hasil = PenyisipDiktean.gabung("x".repeat(1990), "y".repeat(100))
        assertEquals(PenyisipDiktean.MAKS_PANJANG, hasil.length)
    }
}
