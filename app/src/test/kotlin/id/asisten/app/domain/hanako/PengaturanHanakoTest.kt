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
        assertEquals("ABCDEFGHIJ1234567890", KunciAi.bersihkan("  \"ABCDEFGHIJ\n1234567890\"  "))
        assertEquals("ABC", KunciAi.bersihkan("'A B C'"))
    }

    @Test
    fun kunciYangTerlihatTidakMasukAkalDitolak() {
        assertTrue(KunciAi.masukAkal("A".repeat(39)))
        assertTrue(KunciAi.masukAkal("AIzaSyA-bcd_efg.hij~klmnopqrstuvwx"))
        assertFalse(KunciAi.masukAkal(""))
        assertFalse(KunciAi.masukAkal("pendek"))
        assertFalse(KunciAi.masukAkal("A".repeat(19)))
        assertFalse(KunciAi.masukAkal("A".repeat(257)))
        assertFalse(KunciAi.masukAkal("A".repeat(20) + " spasi"))
        assertFalse(KunciAi.masukAkal("A".repeat(20) + "é"))
    }

    @Test
    fun kunciPanjangSekaliDipotongSehinggaDitolak() {
        val bersih = KunciAi.bersihkan("A".repeat(10_000))
        assertTrue(bersih.length <= KunciAi.MAKS_PANJANG + 1)
        assertFalse(KunciAi.masukAkal(bersih))
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
