package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenyediaAiTest {

    @Test
    fun kodeUnikDanDikenaliKembali() {
        assertEquals(PenyediaAi.entries.size, PenyediaAi.entries.map { it.kode }.toSet().size)
        for (p in PenyediaAi.entries) assertEquals(p, PenyediaAi.dariKode(p.kode))
    }

    @Test
    fun kodeDikenalSebagaiMana() {
        // kode ini disimpan di DataStore: berubah berarti pilihan pengguna hilang
        assertEquals(listOf("gemini", "groq", "deepseek"), PenyediaAi.entries.map { it.kode })
    }

    @Test
    fun kodeTidakDikenalAtauKosongKembaliKeGemini() {
        assertEquals(PenyediaAi.GEMINI, PenyediaAi.dariKode(null))
        assertEquals(PenyediaAi.GEMINI, PenyediaAi.dariKode(""))
        assertEquals(PenyediaAi.GEMINI, PenyediaAi.dariKode("openai"))
        assertEquals(PenyediaAi.GEMINI, PenyediaAi.dariKode("GROQ"))
    }

    @Test
    fun modelBawaanSahDanTercantumDiSaran() {
        for (p in PenyediaAi.entries) {
            assertTrue(p.nama, NamaModel.sah(p.modelBawaan, p))
            assertEquals(p.nama, p.modelBawaan, p.saranModel.first())
            assertTrue(p.nama, p.saranModel.all { NamaModel.sah(it, p) })
            assertEquals(p.nama, p.saranModel.size, p.saranModel.toSet().size)
        }
    }

    @Test
    fun hanyaGeminiMemakaiProtokolLama() {
        assertFalse(PenyediaAi.GEMINI.gayaOpenAi)
        assertTrue(PenyediaAi.GROQ.gayaOpenAi)
        assertTrue(PenyediaAi.DEEPSEEK.gayaOpenAi)
    }

    @Test
    fun catatanDataHanyaUntukDeepSeekDanMenyebutTiongkok() {
        assertNull(PenyediaAi.GEMINI.catatanData)
        assertNull(PenyediaAi.GROQ.catatanData)
        assertTrue(PenyediaAi.DEEPSEEK.catatanData!!.contains("Tiongkok"))
    }

    @Test
    fun namaDanHalamanKunciTerisi() {
        for (p in PenyediaAi.entries) {
            assertTrue(p.nama.isNotBlank())
            assertNotNull(p.halamanKunci)
            assertFalse(p.halamanKunci.startsWith("http"))
        }
    }

    @Test
    fun pengaturanBawaanMemakaiModelBawaanPenyediaYangDipilih() {
        assertEquals("llama-3.3-70b-versatile", PengaturanHanako(penyedia = PenyediaAi.GROQ).model)
        assertEquals("deepseek-flash", PengaturanHanako(penyedia = PenyediaAi.DEEPSEEK).model)
        assertEquals(NamaModel.BAWAAN, PengaturanHanako().model)
        assertTrue(PengaturanHanako().penyediaBerkunci.isEmpty())
    }

    @Test
    fun namaModelPerLayanan() {
        // Gemini: nama masuk ke alamat, jadi tanpa garis miring
        assertFalse(NamaModel.sah("openai/gpt-oss-20b", PenyediaAi.GEMINI))
        assertTrue(NamaModel.sah("openai/gpt-oss-20b", PenyediaAi.GROQ))
        assertTrue(NamaModel.sah("meta-llama/llama-4-scout-17b-16e-instruct", PenyediaAi.GROQ))
        for (buruk in listOf("", " ", "a b", "/x", "-x", "a?b", "a\"b", "a\nb", "a".repeat(65), "é")) {
            for (p in PenyediaAi.entries) assertFalse("'$buruk' ${p.nama}", NamaModel.sah(buruk, p))
        }
        // awalan models/ hanya dibuang untuk Gemini
        assertEquals("gemini-x", NamaModel.bersihkan(" models/gemini-x ", PenyediaAi.GEMINI))
        assertEquals("models/x", NamaModel.bersihkan(" models/x ", PenyediaAi.GROQ))
        assertEquals(64, NamaModel.bersihkan("a".repeat(100), PenyediaAi.DEEPSEEK).length)
    }

    @Test
    fun galatTiapLayananMenyebutNamanyaBukanLayananLain() {
        for (p in PenyediaAi.entries) {
            val pesan = JenisGalat.entries.map { LabelHanako.galat(it, p) }
            assertEquals(p.nama, pesan.size, pesan.toSet().size)
            for (j in JenisGalat.entries) {
                if (j == JenisGalat.JARINGAN) continue
                val teks = LabelHanako.galat(j, p)
                assertTrue("$j ${p.nama}: $teks", teks.contains(p.nama))
                for (lain in PenyediaAi.entries.filter { it != p }) assertFalse("$j: $teks", teks.contains(lain.nama))
            }
            assertEquals("Terhubung ke ${p.nama}", LabelHanako.status(true, p))
        }
    }

    @Test
    fun saldoHabisTidakMenawarkanCobaLagiTetapiMenyarankanGantiLayanan() {
        assertTrue(LabelHanako.perluPengaturan(JenisGalat.SALDO_HABIS))
        assertFalse(LabelHanako.bisaDiulang(JenisGalat.SALDO_HABIS))
        assertTrue(LabelHanako.galat(JenisGalat.SALDO_HABIS, PenyediaAi.DEEPSEEK).contains("layanan AI lain"))
    }
}
