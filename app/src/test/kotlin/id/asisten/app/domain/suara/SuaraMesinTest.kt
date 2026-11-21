package id.asisten.app.domain.suara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuaraMesinTest {

    private fun v(id: String, kualitas: Int = 400, net: Boolean = false) = InfoVoice(id, kualitas, net)

    @Test
    fun bahasaIndonesiaDikenaliDenganKodeLamaDanBaru() {
        assertTrue(DaftarVoice.bahasaIndonesia("id"))
        assertTrue(DaftarVoice.bahasaIndonesia("in"))
        for (lain in listOf("en", "ms", "jv", "", "ID", null)) assertFalse("$lain", DaftarVoice.bahasaIndonesia(lain))
    }

    @Test
    fun daftarDiurutkanTetapDanTanpaPengulangan() {
        val a = DaftarVoice.susun(listOf(v("id-id-x-idc-network"), v("id-id-x-ide-local"), v("id-id-x-idc-network"), v("id-id-x-idd-local")))
        val b = DaftarVoice.susun(listOf(v("id-id-x-idd-local"), v("id-id-x-idc-network"), v("id-id-x-ide-local")))
        assertEquals(a, b)
        assertEquals(listOf("id-id-x-idc-network", "id-id-x-idd-local", "id-id-x-ide-local"), a.map { it.id })
    }

    @Test
    fun namaKosongDibuang() {
        assertTrue(DaftarVoice.susun(listOf(v(""), v("  "))).isEmpty())
    }

    @Test
    fun suaraTersimpanYangSudahTidakAdaKembaliKeBawaan() {
        val daftar = DaftarVoice.susun(listOf(v("a"), v("b")))
        assertEquals("b", DaftarVoice.yangDipilih(daftar, "b")?.id)
        assertNull(DaftarVoice.yangDipilih(daftar, "hilang"))
        assertNull(DaftarVoice.yangDipilih(daftar, null))
        assertNull(DaftarVoice.yangDipilih(emptyList(), "b"))
    }

    @Test
    fun keteranganMenyebutMutuDanKebutuhanInternetTanpaMenebakJenisKelamin() {
        assertEquals("kualitas tinggi, bisa tanpa internet", DaftarVoice.keterangan(v("x", 400, false)))
        assertEquals("kualitas tinggi, butuh internet", DaftarVoice.keterangan(v("x", 500, true)))
        assertEquals("kualitas normal, bisa tanpa internet", DaftarVoice.keterangan(v("x", 300)))
        assertEquals("kualitas sederhana, butuh internet", DaftarVoice.keterangan(v("x", 100, true)))
        for (m in listOf(100, 200, 300, 400, 500)) {
            val t = DaftarVoice.keterangan(v("x", m)).lowercase()
            assertFalse(t, t.contains("perempuan") || t.contains("laki") || t.contains("cewek") || t.contains("cowok"))
        }
        assertEquals("Suara 3", DaftarVoice.nama(3))
    }

    // ---------- paket dan penyimpanan ----------

    @Test
    fun paketCocokDenganNilaiNyataBukanTebakan() {
        assertEquals(PaketSuara.NORMAL, PaketSuara.cocok(PengaturanSuara()))
        assertEquals(PaketSuara.IMUT, PaketSuara.cocok(PengaturanSuara(kecepatan = 1.1f, nada = 1.4f)))
        assertEquals(PaketSuara.IMUT_BANGET, PaketSuara.cocok(PengaturanSuara(kecepatan = 1.15f, nada = 1.7f)))
        assertNull(PaketSuara.cocok(PengaturanSuara(kecepatan = 1.0f, nada = 1.4f)))
    }

    @Test
    fun paketImutBerada_diRentangYangDiterimaMesin() {
        for (p in PaketSuara.SEMUA) {
            assertEquals(p.nama, p.kecepatan, PengaturanSuara(kecepatan = p.kecepatan).normalisasi().kecepatan, 0f)
            assertEquals(p.nama, p.nada, PengaturanSuara(nada = p.nada).normalisasi().nada, 0f)
        }
        assertTrue(PaketSuara.IMUT.nada > PaketSuara.NORMAL.nada)
        assertTrue(PaketSuara.IMUT_BANGET.nada > PaketSuara.IMUT.nada)
        assertEquals(PaketSuara.SEMUA.size, PaketSuara.SEMUA.map { it.nama }.toSet().size)
    }

    @Test
    fun pilihanNadaMemuatTingkatImutDanSemuaDalamRentang() {
        val nama = PilihanSuara.NADA.map { it.second }
        assertTrue("Imut" in nama && "Imut banget" in nama)
        for ((nilai, _) in PilihanSuara.NADA) assertEquals(nilai, PengaturanSuara(nada = nilai).normalisasi().nada, 0f)
        assertEquals(1.4f, PilihanSuara.terdekat(1.38f, PilihanSuara.NADA), 0f)
        assertEquals(1.7f, PilihanSuara.terdekat(2.0f, PilihanSuara.NADA), 0f)
    }

    @Test
    fun namaSuaraDibersihkanDanDipotong() {
        assertNull(PengaturanSuara(suara = "  ").normalisasi().suara)
        assertNull(PengaturanSuara(suara = "").normalisasi().suara)
        assertNull(PengaturanSuara(suara = null).normalisasi().suara)
        assertEquals("id-id-x-idc-network", PengaturanSuara(suara = " id-id-x-idc-network ").normalisasi().suara)
        assertEquals(PengaturanSuara.MAKS_NAMA_SUARA, PengaturanSuara(suara = "x".repeat(500)).normalisasi().suara!!.length)
    }
}
