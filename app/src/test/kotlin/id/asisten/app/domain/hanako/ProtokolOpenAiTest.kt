package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtokolOpenAiTest {

    private fun u(t: String) = PesanChat(PeranChat.PENGGUNA, t)
    private fun h(t: String) = PesanChat(PeranChat.HANAKO, t)

    private fun badan(p: PenyediaAi, alat: Boolean = false, riwayat: List<PesanChat> = listOf(u("halo"))) =
        MiniJson.urai(PermintaanAi.badan(p, "model-uji", "sistem", riwayat, alat = alat)) as Map<*, *>

    private fun gagal(h: HasilAi) = h as HasilAi.Gagal
    private fun sukses(h: HasilAi) = h as HasilAi.Berhasil

    private fun pilihan(pesan: String) = """{"choices":[{"message":$pesan,"finish_reason":"stop"}]}"""

    // ---------- permintaan ----------

    @Test
    fun badanGroqMemakaiMaxCompletionTokensDanTanpaThinking() {
        val j = badan(PenyediaAi.GROQ)
        assertEquals("model-uji", j["model"])
        assertEquals(PermintaanAi.MAKS_TOKEN.toDouble(), j["max_completion_tokens"])
        assertFalse(j.containsKey("max_tokens"))
        assertFalse(j.containsKey("thinking"))
        assertFalse(j.containsKey("tools"))
        assertEquals(PermintaanAi.SUHU, j["temperature"])
        assertEquals(false, j["stream"])
    }

    @Test
    fun badanDeepSeekMematikanThinkingDanMemakaiMaxTokens() {
        val j = badan(PenyediaAi.DEEPSEEK)
        assertEquals(mapOf("type" to "disabled"), j["thinking"])
        assertEquals(PermintaanAi.MAKS_TOKEN.toDouble(), j["max_tokens"])
        assertFalse(j.containsKey("max_completion_tokens"))
    }

    @Test
    fun pesanDimulaiDenganSistemLaluPenggunaDanAsisten() {
        val j = badan(PenyediaAi.GROQ, riwayat = listOf(h("sapaan awal"), u("satu"), h("dua"), u("tiga")))
        val pesan = j["messages"] as List<*>
        assertEquals(
            listOf("system" to "sistem", "user" to "satu", "assistant" to "dua", "user" to "tiga"),
            pesan.map { (it as Map<*, *>).let { m -> m["role"] to m["content"] } },
        )
    }

    @Test
    fun teksBerisiKutipBarisBaruDanEmojiTetapJsonSah() {
        val aneh = "halo \"dunia\"\nbaris\t2 \\ 😀 é"
        val j = badan(PenyediaAi.DEEPSEEK, riwayat = listOf(u(aneh)))
        assertEquals(aneh, MiniJson.ambil(j, "messages", 1, "content"))
    }

    @Test
    fun alatMemuatSemuaFungsiDalamBentukOpenAiDanSuhuLebihRendah() {
        for (p in listOf(PenyediaAi.GROQ, PenyediaAi.DEEPSEEK)) {
            val j = badan(p, alat = true)
            val alat = j["tools"] as List<*>
            assertEquals(NamaAksi.SEMUA, alat.map { MiniJson.ambil(it, "function", "name") })
            assertTrue(alat.all { (it as Map<*, *>)["type"] == "function" })
            assertEquals(PermintaanAi.SUHU_ALAT, j["temperature"])
        }
    }

    @Test
    fun badanTidakMemuatKunciApaPun() {
        for (p in listOf(PenyediaAi.GROQ, PenyediaAi.DEEPSEEK)) {
            val teks = PermintaanAi.badan(p, "m", "s", listOf(u("hai")), alat = true).lowercase()
            assertFalse(teks.contains("api_key"))
            assertFalse(teks.contains("bearer"))
        }
    }

    @Test
    fun geminiTetapMemakaiFormatLamaMelaluiPintuMasukBersama() {
        val pesan = listOf(u("hai"))
        assertEquals(
            PermintaanGemini.badan("s", pesan, alat = true),
            PermintaanAi.badan(PenyediaAi.GEMINI, "diabaikan", "s", pesan, alat = true),
        )
    }

    @Test
    fun formatOpenAiMenolakGemini() {
        try {
            PermintaanOpenAi.badan(PenyediaAi.GEMINI, "m", "s", listOf(u("hai")))
            org.junit.Assert.fail("Seharusnya ditolak")
        } catch (e: IllegalArgumentException) {
            // diharapkan
        }
    }

    // ---------- balasan berhasil ----------

    @Test
    fun teksBiasaDibaca() {
        val h = sukses(UraiResponsOpenAi.urai(200, pilihan("""{"role":"assistant","content":"  [senyum] Halo!  "}""")))
        assertEquals("[senyum] Halo!", h.teks)
        assertTrue(h.panggilan.isEmpty())
    }

    @Test
    fun blokPikirDibuangDariTeks() {
        val h = sukses(UraiResponsOpenAi.urai(200, pilihan("""{"content":"<think>hmm\nmungkin</think>\n\n[senyum] Jawabannya 4."}""")))
        assertEquals("[senyum] Jawabannya 4.", h.teks)
        val terpotong = gagal(UraiResponsOpenAi.urai(200, pilihan("""{"content":"<think>masih berpikir tanpa akhir"}""")))
        assertEquals(JenisGalat.KOSONG, terpotong.jenis)
        val sebagian = sukses(UraiResponsOpenAi.urai(200, pilihan("""{"content":"Halo <think>terpotong"}""")))
        assertEquals("Halo", sebagian.teks)
    }

    @Test
    fun panggilanFungsiDibacaDariArgumenBerbentukTeks() {
        val pesan = """{"content":null,"tool_calls":[{"id":"c1","type":"function","function":{"name":"tandai_tugas","arguments":"{\"id\":1,\"selesai\":true}"}}]}"""
        val h = sukses(UraiResponsOpenAi.urai(200, pilihan(pesan)))
        assertEquals("", h.teks)
        assertEquals(listOf(PanggilanFungsi("tandai_tugas", mapOf("id" to 1.0, "selesai" to true))), h.panggilan)
    }

    @Test
    fun teksDanBeberapaPanggilanDigabung() {
        val pesan = """{"content":"Aku usulkan ini.","tool_calls":[
            {"function":{"name":"hapus_tugas","arguments":"{\"id\":1}"}},
            {"function":{"name":"akhiri_mode","arguments":""}},
            {"function":{"name":"mulai_mode","arguments":"{\"mode\":\"FOKUS\"}"}}]}"""
        val h = sukses(UraiResponsOpenAi.urai(200, pilihan(pesan)))
        assertEquals("Aku usulkan ini.", h.teks)
        assertEquals(listOf("hapus_tugas", "akhiri_mode", "mulai_mode"), h.panggilan.map { it.nama })
        assertEquals(emptyMap<String, Any?>(), h.panggilan[1].args)
    }

    @Test
    fun argumenRusakMembuatPanggilanDibuangBukanDianggapKosong() {
        for (rusak in listOf("{\\\"id\\\":", "[1,2]", "\\\"teks\\\"", "123")) {
            val pesan = """{"content":null,"tool_calls":[{"function":{"name":"akhiri_mode","arguments":"$rusak"}}]}"""
            val g = gagal(UraiResponsOpenAi.urai(200, pilihan(pesan)))
            assertEquals(rusak, JenisGalat.KOSONG, g.jenis)
        }
        // panggilan yang rusak tidak menghalangi panggilan lain yang sah
        val campur = """{"tool_calls":[{"function":{"name":"akhiri_mode","arguments":"{rusak"}},{"function":{"name":"hapus_tugas","arguments":"{\"id\":2}"}}]}"""
        assertEquals(listOf("hapus_tugas"), sukses(UraiResponsOpenAi.urai(200, pilihan(campur))).panggilan.map { it.nama })
    }

    @Test
    fun panggilanTanpaNamaAtauFungsiDilewati() {
        val pesan = """{"content":"ok","tool_calls":[{"id":"x"},{"function":{"arguments":"{}"}},"bukan objek"]}"""
        val h = sukses(UraiResponsOpenAi.urai(200, pilihan(pesan)))
        assertTrue(h.panggilan.isEmpty())
        assertEquals("ok", h.teks)
    }

    @Test
    fun panggilanDariGroqDapatDiterimaPenguraiAksi() {
        val pesan = """{"tool_calls":[{"function":{"name":"tandai_tugas","arguments":"{\"id\":1,\"selesai\":true}"}}]}"""
        val panggilan = sukses(UraiResponsOpenAi.urai(200, pilihan(pesan))).panggilan.single()
        val hasil = UraiAksi.urai(panggilan, DataUji.data())
        assertTrue(hasil.toString(), hasil is HasilUrai.Sah)
    }

    @Test
    fun balasanKosongDanDiblokir() {
        assertEquals(JenisGalat.KOSONG, gagal(UraiResponsOpenAi.urai(200, """{"choices":[]}""")).jenis)
        assertEquals(JenisGalat.KOSONG, gagal(UraiResponsOpenAi.urai(200, pilihan("""{"content":""}"""))).jenis)
        val dipotong = gagal(UraiResponsOpenAi.urai(200, """{"choices":[{"message":{"content":null},"finish_reason":"length"}]}"""))
        assertEquals(JenisGalat.KOSONG, dipotong.jenis)
        assertEquals("length", dipotong.rinci)
        val blok = gagal(UraiResponsOpenAi.urai(200, """{"choices":[{"message":{"content":""},"finish_reason":"content_filter"}]}"""))
        assertEquals(JenisGalat.DIBLOKIR, blok.jenis)
    }

    @Test
    fun balasanBukanJsonAtauGalatDalamBadan200() {
        assertEquals(JenisGalat.TIDAK_DIKENAL, gagal(UraiResponsOpenAi.urai(200, "<html>proxy</html>")).jenis)
        assertEquals(JenisGalat.TIDAK_DIKENAL, gagal(UraiResponsOpenAi.urai(200, "[1]")).jenis)
        val g = gagal(UraiResponsOpenAi.urai(200, """{"error":{"message":"ada masalah","type":"server_error"}}"""))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
        assertEquals("ada masalah", g.rinci)
    }

    // ---------- galat ----------

    private fun galat(kode: Int, badan: String) = gagal(UraiResponsOpenAi.urai(kode, badan))

    @Test
    fun kodeHttpDipetakanKeJenisGalat() {
        val pesan = """{"error":{"message":"x"}}"""
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, galat(401, pesan).jenis)
        assertEquals(JenisGalat.SALDO_HABIS, galat(402, pesan).jenis)
        assertEquals(JenisGalat.AKSES_DITOLAK, galat(403, pesan).jenis)
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, galat(404, pesan).jenis)
        assertEquals(JenisGalat.PERMINTAAN_DITOLAK, galat(400, pesan).jenis)
        assertEquals(JenisGalat.PERMINTAAN_DITOLAK, galat(413, pesan).jenis)
        assertEquals(JenisGalat.PERMINTAAN_DITOLAK, galat(422, pesan).jenis)
        assertEquals(JenisGalat.KUOTA, galat(429, pesan).jenis)
        assertEquals(JenisGalat.SERVER, galat(500, pesan).jenis)
        assertEquals(JenisGalat.SERVER, galat(503, pesan).jenis)
        assertEquals(JenisGalat.TIDAK_DIKENAL, galat(302, "").jenis)
    }

    @Test
    fun modelTidakAdaDikenaliMeskipKodenya400() {
        val groq = galat(400, """{"error":{"message":"The model `x` has been decommissioned","type":"invalid_request_error","code":"model_decommissioned"}}""")
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, groq.jenis)
        val groq2 = galat(404, """{"error":{"message":"The model `x` does not exist or you do not have access to it.","code":"model_not_found"}}""")
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, groq2.jenis)
        val deepseek = galat(400, """{"error":{"message":"Model Not Exist","type":"invalid_request_error"}}""")
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, deepseek.jenis)
        assertEquals(JenisGalat.PERMINTAAN_DITOLAK, galat(400, """{"error":{"message":"messages must not be empty"}}""").jenis)
    }

    @Test
    fun kunciSalahDikenaliDariSandiMeskipKodeLain() {
        val g = galat(400, """{"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"invalid_api_key"}}""")
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, g.jenis)
    }

    @Test
    fun saldoHabisDikenaliDariPesan() {
        assertEquals(JenisGalat.SALDO_HABIS, galat(400, """{"error":{"message":"Insufficient Balance"}}""").jenis)
    }

    @Test
    fun rincianDipotongDanBisaBerupaTeksBiasaAtauKodeSaja() {
        val panjang = "x".repeat(900)
        assertEquals(200, galat(400, """{"error":{"message":"$panjang"}}""").rinci.length)
        assertEquals("pesan telanjang", galat(500, """{"error":"pesan telanjang"}""").rinci)
        assertEquals("HTTP 502", galat(502, "Bad Gateway").rinci)
        assertEquals("HTTP 500", galat(500, "").rinci)
    }

    @Test
    fun pintuMasukBersamaMemilihPenguraiYangBenar() {
        val gemini = """{"candidates":[{"content":{"parts":[{"text":"hai"}]}}]}"""
        val openAi = pilihan("""{"content":"hai"}""")
        assertEquals(HasilAi.Berhasil("hai"), UraiResponsAi.urai(PenyediaAi.GEMINI, 200, gemini))
        assertEquals(HasilAi.Berhasil("hai"), UraiResponsAi.urai(PenyediaAi.GROQ, 200, openAi))
        assertEquals(HasilAi.Berhasil("hai"), UraiResponsAi.urai(PenyediaAi.DEEPSEEK, 200, openAi))
        // format yang salah tidak pernah dianggap berhasil
        assertTrue(UraiResponsAi.urai(PenyediaAi.GROQ, 200, gemini) is HasilAi.Gagal)
        assertTrue(UraiResponsAi.urai(PenyediaAi.GEMINI, 200, openAi) is HasilAi.Gagal)
        // 401 berarti kunci salah di Groq, tetapi akses ditolak di Gemini (perilaku lama tidak berubah)
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, gagal(UraiResponsAi.urai(PenyediaAi.GROQ, 401, "{}")).jenis)
        assertEquals(JenisGalat.AKSES_DITOLAK, gagal(UraiResponsAi.urai(PenyediaAi.GEMINI, 401, "{}")).jenis)
    }
}
