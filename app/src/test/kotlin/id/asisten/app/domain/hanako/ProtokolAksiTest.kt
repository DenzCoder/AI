package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtokolAksiTest {

    private fun respons(vararg bagian: String) =
        """{"candidates":[{"content":{"role":"model","parts":[${bagian.joinToString(",")}]},"finishReason":"STOP"}]}"""

    private fun berhasil(badan: String): HasilAi.Berhasil {
        val hasil = UraiResponsGemini.urai(200, badan)
        assertTrue("diharapkan berhasil, tetapi $hasil", hasil is HasilAi.Berhasil)
        return hasil as HasilAi.Berhasil
    }

    @Test
    fun teksDanPanggilanFungsiSekaligus() {
        val h = berhasil(
            respons(
                """{"text":"[senyum] Aku usulkan tugas baru."}""",
                """{"functionCall":{"name":"tambah_tugas","args":{"judul":"PR Fisika","tenggat":"2026-11-20"}}}""",
            ),
        )
        assertEquals("[senyum] Aku usulkan tugas baru.", h.teks)
        assertEquals(1, h.panggilan.size)
        assertEquals("tambah_tugas", h.panggilan[0].nama)
        assertEquals("PR Fisika", h.panggilan[0].args["judul"])
        assertEquals("2026-11-20", h.panggilan[0].args["tenggat"])
    }

    @Test
    fun hanyaPanggilanFungsiTanpaTeksTetapBerhasil() {
        val h = berhasil(respons("""{"functionCall":{"name":"hapus_tugas","args":{"id":3}}}"""))
        assertEquals("", h.teks)
        assertEquals(listOf(PanggilanFungsi("hapus_tugas", mapOf("id" to 3.0))), h.panggilan)
    }

    @Test
    fun beberapaPanggilanBerurutan() {
        val h = berhasil(
            respons(
                """{"functionCall":{"name":"tandai_tugas","args":{"id":1,"selesai":true}}}""",
                """{"functionCall":{"name":"mulai_mode","args":{"mode":"FOKUS","menit":25}}}""",
            ),
        )
        assertEquals(listOf("tandai_tugas", "mulai_mode"), h.panggilan.map { it.nama })
        assertEquals(true, h.panggilan[0].args["selesai"])
    }

    @Test
    fun panggilanTanpaArgsAtauDenganArgsRusakMenjadiArgsKosong() {
        val h = berhasil(
            respons(
                """{"functionCall":{"name":"akhiri_mode"}}""",
                """{"functionCall":{"name":"atur_pengingat","args":"bukan objek"}}""",
            ),
        )
        assertEquals(listOf("akhiri_mode", "atur_pengingat"), h.panggilan.map { it.nama })
        assertTrue(h.panggilan.all { it.args.isEmpty() })
    }

    @Test
    fun panggilanTanpaNamaDilewati() {
        val h = berhasil(
            respons(
                """{"functionCall":{"args":{"id":1}}}""",
                """{"functionCall":{"name":5,"args":{}}}""",
                """{"text":"halo"}""",
            ),
        )
        assertEquals("halo", h.teks)
        assertTrue(h.panggilan.isEmpty())
    }

    @Test
    fun bagianPikiranDanTandaTanganDiabaikan() {
        val h = berhasil(
            respons(
                """{"thought":true,"text":"memikirkan sesuatu"}""",
                """{"functionCall":{"name":"hapus_kegiatan","args":{"id":10}},"thoughtSignature":"abc=="}""",
            ),
        )
        assertEquals("", h.teks)
        assertEquals(1, h.panggilan.size)
    }

    @Test
    fun tanpaTeksDanTanpaPanggilanTetapKosong() {
        val hasil = UraiResponsGemini.urai(200, respons("""{"functionCall":{"args":{}}}"""))
        assertEquals(JenisGalat.KOSONG, (hasil as HasilAi.Gagal).jenis)
        val diblokir = UraiResponsGemini.urai(200, """{"candidates":[{"finishReason":"SAFETY"}]}""")
        assertEquals(JenisGalat.DIBLOKIR, (diblokir as HasilAi.Gagal).jenis)
    }

    @Test
    fun hasilBerhasilTanpaPanggilanTetapKompatibel() {
        assertEquals(HasilAi.Berhasil("a", emptyList()), HasilAi.Berhasil("a"))
    }

    @Test
    fun badanTanpaAlatTidakMemuatTools() {
        val badan = PermintaanGemini.badan("sistem", listOf(PesanChat(PeranChat.PENGGUNA, "hai")))
        assertFalse(badan.contains("tools"))
        assertFalse(badan.contains("functionDeclarations"))
    }

    @Test
    fun badanDenganAlatMemuatSemuaFungsiDanTetapJsonSah() {
        val badan = PermintaanGemini.badan("sistem", listOf(PesanChat(PeranChat.PENGGUNA, "hai")), alat = true)
        val json = MiniJson.urai(badan) as Map<*, *>
        val deklarasi = MiniJson.ambil(json, "tools", 0, "functionDeclarations") as List<*>
        assertEquals(NamaAksi.SEMUA, deklarasi.map { (it as Map<*, *>)["name"] })
        assertTrue(json.containsKey("systemInstruction"))
        assertTrue(json.containsKey("contents"))
        assertTrue(json.containsKey("generationConfig"))
        assertFalse(badan.lowercase().contains("key"))
        assertFalse(badan.lowercase().contains("thinking"))
    }

    @Test
    fun suhuLebihRendahBilaAlatDikirim() {
        val pesan = listOf(PesanChat(PeranChat.PENGGUNA, "hai"))
        val biasa = MiniJson.urai(PermintaanGemini.badan("s", pesan)) as Map<*, *>
        val denganAlat = MiniJson.urai(PermintaanGemini.badan("s", pesan, alat = true)) as Map<*, *>
        assertEquals(0.8, MiniJson.ambil(biasa, "generationConfig", "temperature"))
        assertEquals(0.4, MiniJson.ambil(denganAlat, "generationConfig", "temperature"))
    }
}
