package id.asisten.app.data.ai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import id.asisten.app.domain.hanako.HasilAi
import id.asisten.app.domain.hanako.JenisGalat
import id.asisten.app.domain.hanako.PenyediaAi
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Tests the HTTP client against a real server on the loopback interface: nothing leaves this machine. */
class KlienAiTest {

    private lateinit var server: HttpServer
    private val jumlahPermintaan = AtomicInteger()

    @Volatile private var metode = ""
    @Volatile private var jalur = ""
    @Volatile private var kueri: String? = null
    @Volatile private var headerKunci: String? = null
    @Volatile private var headerOtorisasi: String? = null
    @Volatile private var jenisIsi: String? = null
    @Volatile private var badanDiterima = ""
    @Volatile private var kode = 200
    @Volatile private var balasan = ""
    @Volatile private var jedaMs = 0L
    @Volatile private var headerLokasi: String? = null

    private val G = PenyediaAi.GEMINI
    private val kunci = "KUNCI-UJI-1234567890-ABCDEFG"
    private val suksesOpenAi = """{"choices":[{"message":{"role":"assistant","content":"[senyum] Halo!"},"finish_reason":"stop"}]}"""
    private val sukses = """{"candidates":[{"content":{"parts":[{"text":"[senyum] Halo!"}]},"finishReason":"STOP"}]}"""

    @Before
    fun nyalakan() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        server.createContext("/") { x: HttpExchange ->
            jumlahPermintaan.incrementAndGet()
            metode = x.requestMethod
            jalur = x.requestURI.path
            kueri = x.requestURI.rawQuery
            headerKunci = x.requestHeaders.getFirst("x-goog-api-key")
            headerOtorisasi = x.requestHeaders.getFirst("Authorization")
            jenisIsi = x.requestHeaders.getFirst("Content-Type")
            badanDiterima = String(x.requestBody.readBytes(), Charsets.UTF_8)
            if (jedaMs > 0) Thread.sleep(jedaMs)
            headerLokasi?.let { x.responseHeaders.add("Location", it) }
            val isi = balasan.toByteArray(Charsets.UTF_8)
            x.sendResponseHeaders(kode, if (isi.isEmpty()) -1 else isi.size.toLong())
            if (isi.isNotEmpty()) x.responseBody.use { it.write(isi) } else x.close()
        }
        server.start()
        kode = 200
        balasan = sukses
    }

    @After
    fun matikan() {
        server.stop(0)
    }

    private fun alamatUji(port: Int = server.address.port) = mapOf(
        PenyediaAi.GEMINI to "http://127.0.0.1:$port/v1beta",
        PenyediaAi.GROQ to "http://127.0.0.1:$port/openai/v1",
        PenyediaAi.DEEPSEEK to "http://127.0.0.1:$port",
    )

    private fun klien(batasBaca: Int = 5_000) = KlienAi(alamatUji(), batasSambungMs = 2_000, batasBacaMs = batasBaca)

    private fun gagal(h: HasilAi) = h as HasilAi.Gagal

    @Test
    fun permintaanBerhasilDanDikirimDenganBentukYangBenar() {
        val badan = """{"contents":[{"role":"user","parts":[{"text":"Halo 😀 é"}]}]}"""
        val hasil = klien().kirimSinkron(G, kunci, "gemini-uji", badan)
        assertEquals(HasilAi.Berhasil("[senyum] Halo!"), hasil)
        assertEquals("POST", metode)
        assertEquals("/v1beta/models/gemini-uji:generateContent", jalur)
        assertEquals(kunci, headerKunci)
        assertTrue(jenisIsi!!.startsWith("application/json"))
        assertEquals(badan, badanDiterima)
    }

    @Test
    fun kunciTidakPernahMasukKeAlamat() {
        klien().kirimSinkron(G, kunci, "gemini-uji", "{}")
        assertNull(kueri)
        assertFalse(jalur.contains(kunci))
    }

    @Test
    fun kunciTidakValidDikenali() {
        kode = 400
        balasan = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, gagal(klien().kirimSinkron(G, kunci, "m", "{}")).jenis)
    }

    @Test
    fun kodeServerDipetakan() {
        for ((k, jenis) in listOf(
            429 to JenisGalat.KUOTA,
            404 to JenisGalat.MODEL_TIDAK_ADA,
            403 to JenisGalat.AKSES_DITOLAK,
            503 to JenisGalat.SERVER,
        )) {
            kode = k
            balasan = """{"error":{"message":"x"}}"""
            assertEquals("HTTP $k", jenis, gagal(klien().kirimSinkron(G, kunci, "m", "{}")).jenis)
        }
    }

    @Test
    fun galatTanpaBadanTidakMelempar() {
        kode = 500
        balasan = ""
        assertEquals(JenisGalat.SERVER, gagal(klien().kirimSinkron(G, kunci, "m", "{}")).jenis)
    }

    @Test
    fun kunciDiSensorJikaServerMengulangnyaDalamPesanGalat() {
        kode = 400
        balasan = """{"error":{"message":"masalah dengan $kunci di sini"}}"""
        val g = gagal(klien().kirimSinkron(G, kunci, "m", "{}"))
        assertFalse(g.rinci.contains(kunci))
        assertTrue(g.rinci.contains("***"))
    }

    @Test
    fun balasanLambatMenjadiWaktuHabis() {
        jedaMs = 1_500
        val g = gagal(klien(batasBaca = 300).kirimSinkron(G, kunci, "m", "{}"))
        assertEquals(JenisGalat.WAKTU_HABIS, g.jenis)
    }

    @Test
    fun serverTidakAdaMenjadiMasalahJaringan() {
        val port = server.address.port
        server.stop(0)
        val k = KlienAi(alamatUji(port), batasSambungMs = 1_000, batasBacaMs = 1_000)
        assertEquals(JenisGalat.JARINGAN, gagal(k.kirimSinkron(G, kunci, "m", "{}")).jenis)
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0).also { it.start() } // agar @After aman
    }

    @Test
    fun kunciKosongDanNamaModelBurukDitolakTanpaMenghubungiServer() {
        assertEquals(JenisGalat.TANPA_KUNCI, gagal(klien().kirimSinkron(G, "   ", "m", "{}")).jenis)
        for (nama in listOf("", "a b", "../rahasia", "m?x=1", "m/../../x", "m:generate", "models/m", "-m")) {
            assertEquals(nama, JenisGalat.MODEL_TIDAK_ADA, gagal(klien().kirimSinkron(G, kunci, nama, "{}")).jenis)
        }
        assertEquals(0, jumlahPermintaan.get())
    }

    @Test
    fun pengalihanTidakDiikutiSehinggaKunciTidakBocorKeAlamatLain() {
        kode = 302
        balasan = ""
        headerLokasi = "http://127.0.0.1:1/penjahat"
        val g = gagal(klien().kirimSinkron(G, kunci, "m", "{}"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
        assertEquals(1, jumlahPermintaan.get())
    }

    @Test
    fun balasanRaksasaDipotongDanTidakMenghabiskanMemori() {
        balasan = "a".repeat(3_000_000)
        val g = gagal(klien().kirimSinkron(G, kunci, "m", "{}"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
    }

    @Test
    fun hanyaHttpsDiterimaKecualiLoopbackUntukUji() {
        KlienAi()
        KlienAi(mapOf(PenyediaAi.GEMINI to "https://generativelanguage.googleapis.com/v1beta"))
        for (alamat in listOf("http://example.com", "ftp://x", "http://localhost:80", "")) {
            try {
                KlienAi(mapOf(PenyediaAi.GROQ to alamat))
                fail("Seharusnya ditolak: $alamat")
            } catch (e: IllegalArgumentException) {
                // diharapkan
            }
        }
    }

    @Test
    fun versiSuspendBekerjaDariCoroutine() = runBlocking {
        val hasil = klien().kirim(G, kunci, "gemini-uji", "{}")
        assertEquals(HasilAi.Berhasil("[senyum] Halo!"), hasil)
    }

    @Test
    fun groqDikirimKeAlamatChatCompletionsDenganBearer() {
        balasan = suksesOpenAi
        val badan = """{"model":"openai/gpt-oss-20b","messages":[]}"""
        val hasil = klien().kirimSinkron(PenyediaAi.GROQ, kunci, "openai/gpt-oss-20b", badan)
        assertEquals(HasilAi.Berhasil("[senyum] Halo!"), hasil)
        assertEquals("POST", metode)
        assertEquals("/openai/v1/chat/completions", jalur)
        assertEquals("Bearer $kunci", headerOtorisasi)
        assertNull("kunci Gemini tidak boleh ikut", headerKunci)
        assertNull(kueri)
        assertEquals(badan, badanDiterima)
    }

    @Test
    fun deepSeekDikirimKeAlamatChatCompletionsDenganBearer() {
        balasan = suksesOpenAi
        val hasil = klien().kirimSinkron(PenyediaAi.DEEPSEEK, kunci, "deepseek-flash", "{}")
        assertEquals(HasilAi.Berhasil("[senyum] Halo!"), hasil)
        assertEquals("/chat/completions", jalur)
        assertEquals("Bearer $kunci", headerOtorisasi)
        assertNull(headerKunci)
    }

    @Test
    fun geminiTidakMengirimHeaderBearer() {
        klien().kirimSinkron(G, kunci, "gemini-uji", "{}")
        assertNull(headerOtorisasi)
    }

    @Test
    fun kodeGalatOpenAiDipetakanPerLayanan() {
        for (penyedia in listOf(PenyediaAi.GROQ, PenyediaAi.DEEPSEEK)) {
            for ((k, jenis) in listOf(
                401 to JenisGalat.KUNCI_TIDAK_VALID,
                402 to JenisGalat.SALDO_HABIS,
                429 to JenisGalat.KUOTA,
                404 to JenisGalat.MODEL_TIDAK_ADA,
                403 to JenisGalat.AKSES_DITOLAK,
                503 to JenisGalat.SERVER,
            )) {
                kode = k
                balasan = """{"error":{"message":"x"}}"""
                assertEquals("${penyedia.nama} HTTP $k", jenis, gagal(klien().kirimSinkron(penyedia, kunci, "m", "{}")).jenis)
            }
        }
    }

    @Test
    fun kunciOpenAiTidakMasukAlamatDanDisensorDariPesanGalat() {
        kode = 400
        balasan = """{"error":{"message":"masalah dengan $kunci di sini"}}"""
        val g = gagal(klien().kirimSinkron(PenyediaAi.GROQ, kunci, "m", "{}"))
        assertFalse(g.rinci.contains(kunci))
        assertTrue(g.rinci.contains("***"))
        assertFalse(jalur.contains(kunci))
        assertNull(kueri)
    }

    @Test
    fun namaModelDenganGarisMiringHanyaDiterimaLayananBerformatOpenAi() {
        assertEquals(JenisGalat.MODEL_TIDAK_ADA, gagal(klien().kirimSinkron(G, kunci, "openai/gpt-oss-20b", "{}")).jenis)
        assertEquals(0, jumlahPermintaan.get())
        balasan = suksesOpenAi
        assertTrue(klien().kirimSinkron(PenyediaAi.GROQ, kunci, "openai/gpt-oss-20b", "{}") is HasilAi.Berhasil)
        for (nama in listOf("", "a b", "m?x=1", "-m", "m\"x", "a".repeat(65))) {
            assertEquals(nama, JenisGalat.MODEL_TIDAK_ADA, gagal(klien().kirimSinkron(PenyediaAi.DEEPSEEK, kunci, nama, "{}")).jenis)
        }
    }

    @Test
    fun pengalihanTidakDiikutiUntukLayananOpenAi() {
        kode = 307
        balasan = ""
        headerLokasi = "http://127.0.0.1:1/penjahat"
        val g = gagal(klien().kirimSinkron(PenyediaAi.DEEPSEEK, kunci, "deepseek-flash", "{}"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
        assertEquals(1, jumlahPermintaan.get())
    }

    @Test
    fun setiapLayananPunyaAlamatDasarHttpsTetap() {
        for (p in PenyediaAi.entries) {
            assertTrue(p.nama, p.urlDasar.startsWith("https://"))
            assertFalse(p.nama, p.urlDasar.endsWith("/"))
        }
    }
}
