package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtokolGeminiTest {

    private fun u(isi: String) = PesanChat(PeranChat.PENGGUNA, isi)
    private fun h(isi: String) = PesanChat(PeranChat.HANAKO, isi, EkspresiHanako.SENYUM)

    private fun gagal(hasil: HasilAi) = hasil as HasilAi.Gagal

    // --- request body ---

    @Test
    fun badanPermintaanAdalahJsonSahDenganSemuaBagian() {
        val sistem = "Kamu Hanako.\nJawab \"singkat\" ya."
        val json = MiniJson.urai(PermintaanGemini.badan(sistem, listOf(u("halo \"dunia\"\nbaris dua"), h("hai"), u("apa kabar"))))
        assertEquals(sistem, MiniJson.ambil(json, "systemInstruction", "parts", 0, "text"))
        assertEquals("user", MiniJson.ambil(json, "contents", 0, "role"))
        assertEquals("halo \"dunia\"\nbaris dua", MiniJson.ambil(json, "contents", 0, "parts", 0, "text"))
        assertEquals("model", MiniJson.ambil(json, "contents", 1, "role"))
        assertEquals("user", MiniJson.ambil(json, "contents", 2, "role"))
        assertEquals(PermintaanGemini.MAKS_TOKEN.toDouble(), MiniJson.ambil(json, "generationConfig", "maxOutputTokens"))
        assertEquals(PermintaanGemini.SUHU, MiniJson.ambil(json, "generationConfig", "temperature"))
    }

    @Test
    fun badanPermintaanTidakPernahMemuatKunciApiAtauParameterBerpikir() {
        val badan = PermintaanGemini.badan("sistem", listOf(u("halo")))
        assertTrue(!badan.contains("key", ignoreCase = true))
        assertTrue(!badan.contains("thinking", ignoreCase = true))
    }

    @Test
    fun badanPermintaanMemperbaikiUrutanPeranUntukGemini() {
        val json = MiniJson.urai(PermintaanGemini.badan("s", listOf(h("sapaan awal"), u("satu"), u("dua"), h("jawab"))))
        assertEquals("user", MiniJson.ambil(json, "contents", 0, "role"))
        assertEquals("satu\n\ndua", MiniJson.ambil(json, "contents", 0, "parts", 0, "text"))
        assertEquals("model", MiniJson.ambil(json, "contents", 1, "role"))
        assertNull(MiniJson.ambil(json, "contents", 2))
    }

    // --- successful responses ---

    @Test
    fun membacaTeksJawabanBiasa() {
        val badan = """{"candidates":[{"content":{"role":"model","parts":[{"text":"[senyum] Halo!"}]},"finishReason":"STOP"}]}"""
        assertEquals(HasilAi.Berhasil("[senyum] Halo!"), UraiResponsGemini.urai(200, badan))
    }

    @Test
    fun menggabungkanBeberapaBagianDanMelewatiPikiran() {
        val badan = """{"candidates":[{"content":{"parts":[
            {"text":"rahasia pikiran","thought":true},{"text":"Halo "},{"text":"dunia"}]}}]}"""
        assertEquals(HasilAi.Berhasil("Halo dunia"), UraiResponsGemini.urai(200, badan))
    }

    @Test
    fun jawabanTerpotongTapiAdaTeksTetapDipakai() {
        val badan = """{"candidates":[{"content":{"parts":[{"text":"Separuh jawaban"}]},"finishReason":"MAX_TOKENS"}]}"""
        assertEquals(HasilAi.Berhasil("Separuh jawaban"), UraiResponsGemini.urai(200, badan))
    }

    @Test
    fun tanpaTeksKarenaTokenHabisDianggapKosong() {
        val badan = """{"candidates":[{"content":{"role":"model"},"finishReason":"MAX_TOKENS"}]}"""
        val g = gagal(UraiResponsGemini.urai(200, badan))
        assertEquals(JenisGalat.KOSONG, g.jenis)
        assertEquals("MAX_TOKENS", g.rinci)
    }

    @Test
    fun pesanDiblokirOlehGoogle() {
        val a = gagal(UraiResponsGemini.urai(200, """{"promptFeedback":{"blockReason":"SAFETY"}}"""))
        assertEquals(JenisGalat.DIBLOKIR, a.jenis)
        val b = gagal(UraiResponsGemini.urai(200, """{"candidates":[{"finishReason":"SAFETY"}]}"""))
        assertEquals(JenisGalat.DIBLOKIR, b.jenis)
    }

    @Test
    fun balasanSuksesYangBukanJsonDitolakDenganJujur() {
        assertEquals(JenisGalat.TIDAK_DIKENAL, gagal(UraiResponsGemini.urai(200, "<html>proxy</html>")).jenis)
        assertEquals(JenisGalat.TIDAK_DIKENAL, gagal(UraiResponsGemini.urai(200, "[1,2]")).jenis)
        assertEquals(JenisGalat.KOSONG, gagal(UraiResponsGemini.urai(200, "{}")).jenis)
    }

    // --- errors ---

    @Test
    fun kunciTidakValidDikenali() {
        val badan = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT",
            "details":[{"reason":"API_KEY_INVALID"}]}}"""
        val g = gagal(UraiResponsGemini.urai(400, badan))
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, g.jenis)
        assertTrue(g.rinci.startsWith("API key not valid"))
    }

    @Test
    fun kodeGalatDipetakanKeJenisYangTepat() {
        fun jenis(kode: Int, status: String = "", pesan: String = "x") =
            gagal(UraiResponsGemini.urai(kode, """{"error":{"code":$kode,"message":"$pesan","status":"$status"}}""")).jenis
        assertEquals(JenisGalat.KUOTA, jenis(429, "RESOURCE_EXHAUSTED"))
        assertEquals(JenisGalat.AKSES_DITOLAK, jenis(403, "PERMISSION_DENIED"))
        assertEquals(JenisGalat.AKSES_DITOLAK, jenis(401, "UNAUTHENTICATED"))
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, jenis(404, "NOT_FOUND"))
        assertEquals(JenisGalat.PERMINTAAN_DITOLAK, jenis(400, "INVALID_ARGUMENT"))
        assertEquals(JenisGalat.SERVER, jenis(500, "INTERNAL"))
        assertEquals(JenisGalat.SERVER, jenis(503, "UNAVAILABLE"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, jenis(418))
    }

    @Test
    fun galatTanpaBadanJsonTetapDipetakan() {
        val g = gagal(UraiResponsGemini.urai(502, "Bad Gateway"))
        assertEquals(JenisGalat.SERVER, g.jenis)
        assertEquals("HTTP 502", g.rinci)
    }

    @Test
    fun rincianGalatDipotong() {
        val pesan = "a".repeat(1000)
        val g = gagal(UraiResponsGemini.urai(500, """{"error":{"message":"$pesan"}}"""))
        assertEquals(200, g.rinci.length)
    }

    // --- mood marker ---

    @Test
    fun penandaSuasanaDiambilDanDibuangDariTeks() {
        assertEquals(BalasanUrai("Halo!", EkspresiHanako.SENYUM), UraiBalasan.urai("[senyum] Halo!"))
        assertEquals(BalasanUrai("Halo", EkspresiHanako.TERTAWA), UraiBalasan.urai("[TERTAWA]\nHalo"))
        assertEquals(BalasanUrai("Hmm...", EkspresiHanako.BERPIKIR), UraiBalasan.urai("  [Berpikir]   Hmm...  "))
    }

    @Test
    fun semuaDuaBelasEkspresiBisaDipilihLewatPenanda() {
        for (e in EkspresiHanako.entries) {
            assertEquals(e, UraiBalasan.urai("[${e.name.lowercase()}] x").ekspresi)
        }
    }

    @Test
    fun tanpaPenandaYangDikenalTeksTidakDiubah() {
        assertEquals(BalasanUrai("[1] satu dua", null), UraiBalasan.urai("[1] satu dua"))
        assertEquals(BalasanUrai("[entah] teks", null), UraiBalasan.urai("[entah] teks"))
        assertEquals(BalasanUrai("Halo [senyum]", null), UraiBalasan.urai("Halo [senyum]"))
        assertEquals(BalasanUrai("Tanpa penanda", null), UraiBalasan.urai("  Tanpa penanda "))
    }

    @Test
    fun hanyaPenandaMenghasilkanTeksKosong() {
        val b = UraiBalasan.urai("[senyum]")
        assertEquals("", b.teks)
        assertEquals(EkspresiHanako.SENYUM, b.ekspresi)
    }
}
