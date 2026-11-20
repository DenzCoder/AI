package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelHanakoTest {

    @Test
    fun setiapJenisGalatPunyaPesanUnikDanTidakKosong() {
        val pesan = JenisGalat.entries.map { LabelHanako.galat(it) }
        assertTrue(pesan.all { it.isNotBlank() })
        assertEquals(pesan.size, pesan.toSet().size)
    }

    @Test
    fun pesanGalatTidakMemuatKunciAtauKodeInternal() {
        for (jenis in JenisGalat.entries) {
            val p = LabelHanako.galat(jenis)
            assertFalse(p.contains("AIza"))
            assertFalse(p.contains("gsk_"))
            assertFalse(p.contains("Exception"))
        }
    }

    @Test
    fun galatTerbagiAntaraPengaturanUlangDanTidakDua() {
        assertTrue(LabelHanako.perluPengaturan(JenisGalat.KUNCI_TIDAK_VALID))
        assertTrue(LabelHanako.perluPengaturan(JenisGalat.MODEL_TIDAK_ADA))
        assertTrue(LabelHanako.perluPengaturan(JenisGalat.TANPA_KUNCI))
        assertTrue(LabelHanako.perluPengaturan(JenisGalat.SALDO_HABIS))
        assertFalse(LabelHanako.perluPengaturan(JenisGalat.JARINGAN))

        assertTrue(LabelHanako.bisaDiulang(JenisGalat.JARINGAN))
        assertTrue(LabelHanako.bisaDiulang(JenisGalat.KUOTA))
        assertTrue(LabelHanako.bisaDiulang(JenisGalat.WAKTU_HABIS))
        assertFalse(LabelHanako.bisaDiulang(JenisGalat.KUNCI_TIDAK_VALID))
        assertFalse(LabelHanako.bisaDiulang(JenisGalat.DIBLOKIR))
        for (j in JenisGalat.entries) {
            // tidak ada galat yang menawarkan "coba lagi" sekaligus "buka pengaturan"
            assertFalse(LabelHanako.perluPengaturan(j) && LabelHanako.bisaDiulang(j))
        }
    }

    @Test
    fun statusMenyebutModeDenganJelas() {
        assertTrue(LabelHanako.status(true).contains("Gemini"))
        assertTrue(LabelHanako.status(false).contains("lokal"))
    }
}
