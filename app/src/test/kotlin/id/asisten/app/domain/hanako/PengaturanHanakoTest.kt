package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PengaturanHanakoTest {

    @Test
    fun onlineHanyaJikaSakelarMenyalaDanKunciAda() {
        assertFalse(PengaturanHanako().online)
        assertFalse(PengaturanHanako(onlineAktif = true, adaKunci = false).online)
        assertFalse(PengaturanHanako(onlineAktif = false, adaKunci = true).online)
        assertTrue(PengaturanHanako(onlineAktif = true, adaKunci = true).online)
    }

    @Test
    fun bawaanAdalahAmanMatiDanTanpaKunci() {
        val p = PengaturanHanako()
        assertFalse(p.onlineAktif)
        assertFalse(p.adaKunci)
        assertEquals(NamaModel.BAWAAN, p.model)
        assertTrue(NamaModel.sah(NamaModel.BAWAAN))
    }

    @Test
    fun namaModelDibersihkanDanDiperiksa() {
        assertEquals("gemini-x", NamaModel.bersihkan("  models/gemini-x  "))
        assertEquals(64, NamaModel.bersihkan("a".repeat(100)).length)
        assertTrue(NamaModel.sah("gemini-3.6-flash"))
        assertTrue(NamaModel.sah("gemini_2.5-pro"))
        for (buruk in listOf("", " ", "a b", "../x", "x/y", "x?y", "x:y", "-x", ".x", "é", "a".repeat(65))) {
            assertFalse("'$buruk'", NamaModel.sah(buruk))
        }
    }

    @Test
    fun kunciDibersihkanDariSpasiDanTandaKutip() {
        assertEquals("ABCDEFGHIJ1234567890", KunciGemini.bersihkan("  \"ABCDEFGHIJ\n1234567890\"  "))
        assertEquals("ABC", KunciGemini.bersihkan("'A B C'"))
    }

    @Test
    fun kunciYangTerlihatTidakMasukAkalDitolak() {
        assertTrue(KunciGemini.masukAkal("A".repeat(39)))
        assertTrue(KunciGemini.masukAkal("AIzaSyA-bcd_efg.hij~klmnopqrstuvwx"))
        assertFalse(KunciGemini.masukAkal(""))
        assertFalse(KunciGemini.masukAkal("pendek"))
        assertFalse(KunciGemini.masukAkal("A".repeat(19)))
        assertFalse(KunciGemini.masukAkal("A".repeat(257)))
        assertFalse(KunciGemini.masukAkal("A".repeat(20) + " spasi"))
        assertFalse(KunciGemini.masukAkal("A".repeat(20) + "é"))
    }

    @Test
    fun kunciPanjangSekaliDipotongSehinggaDitolak() {
        val bersih = KunciGemini.bersihkan("A".repeat(10_000))
        assertTrue(bersih.length <= KunciGemini.MAKS_PANJANG + 1)
        assertFalse(KunciGemini.masukAkal(bersih))
    }

    @Test
    fun bisaMengusulkanButuhOnlineDanSaklarAksi() {
        val online = PengaturanHanako(onlineAktif = true, adaKunci = true)
        assertTrue(online.bisaMengusulkan)
        assertFalse(online.copy(aksiAktif = false).bisaMengusulkan)
        assertFalse(online.copy(onlineAktif = false).bisaMengusulkan)
        assertFalse(online.copy(adaKunci = false).bisaMengusulkan)
        assertTrue(PengaturanHanako().aksiAktif)
    }
}
