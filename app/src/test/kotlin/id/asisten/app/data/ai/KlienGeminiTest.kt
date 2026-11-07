package id.asisten.app.data.ai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import id.asisten.app.domain.hanako.HasilGemini
import id.asisten.app.domain.hanako.JenisGalat
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
class KlienGeminiTest {

    private lateinit var server: HttpServer
    private val jumlahPermintaan = AtomicInteger()

    @Volatile private var metode = ""
    @Volatile private var jalur = ""
    @Volatile private var kueri: String? = null
    @Volatile private var headerKunci: String? = null
    @Volatile private var jenisIsi: String? = null
    @Volatile private var badanDiterima = ""
    @Volatile private var kode = 200
    @Volatile private var balasan = ""
    @Volatile private var jedaMs = 0L
    @Volatile private var headerLokasi: String? = null

    private val kunci = "KUNCI-UJI-1234567890-ABCDEFG"
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

    private fun klien(batasBaca: Int = 5_000) =
        KlienGemini("http://127.0.0.1:${server.address.port}/v1beta", batasSambungMs = 2_000, batasBacaMs = batasBaca)

    private fun gagal(h: HasilGemini) = h as HasilGemini.Gagal

    @Test
    fun permintaanBerhasilDanDikirimDenganBentukYangBenar() {
        val badan = """{"contents":[{"role":"user","parts":[{"text":"Halo 😀 é"}]}]}"""
        val hasil = klien().kirimSinkron(kunci, "gemini-uji", badan)
        assertEquals(HasilGemini.Berhasil("[senyum] Halo!"), hasil)
        assertEquals("POST", metode)
        assertEquals("/v1beta/models/gemini-uji:generateContent", jalur)
        assertEquals(kunci, headerKunci)
        assertTrue(jenisIsi!!.startsWith("application/json"))
        assertEquals(badan, badanDiterima)
    }

    @Test
    fun kunciTidakPernahMasukKeAlamat() {
        klien().kirimSinkron(kunci, "gemini-uji", "{}")
        assertNull(kueri)
        assertFalse(jalur.contains(kunci))
    }

    @Test
    fun kunciTidakValidDikenali() {
        kode = 400
        balasan = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
        assertEquals(JenisGalat.KUNCI_TIDAK_VALID, gagal(klien().kirimSinkron(kunci, "m", "{}")).jenis)
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
            assertEquals("HTTP $k", jenis, gagal(klien().kirimSinkron(kunci, "m", "{}")).jenis)
        }
    }

    @Test
    fun galatTanpaBadanTidakMelempar() {
        kode = 500
        balasan = ""
        assertEquals(JenisGalat.SERVER, gagal(klien().kirimSinkron(kunci, "m", "{}")).jenis)
    }

    @Test
    fun kunciDiSensorJikaServerMengulangnyaDalamPesanGalat() {
        kode = 400
        balasan = """{"error":{"message":"masalah dengan $kunci di sini"}}"""
        val g = gagal(klien().kirimSinkron(kunci, "m", "{}"))
        assertFalse(g.rinci.contains(kunci))
        assertTrue(g.rinci.contains("***"))
    }

    @Test
    fun balasanLambatMenjadiWaktuHabis() {
        jedaMs = 1_500
        val g = gagal(klien(batasBaca = 300).kirimSinkron(kunci, "m", "{}"))
        assertEquals(JenisGalat.WAKTU_HABIS, g.jenis)
    }

    @Test
    fun serverTidakAdaMenjadiMasalahJaringan() {
        val port = server.address.port
        server.stop(0)
        val k = KlienGemini("http://127.0.0.1:$port/v1beta", batasSambungMs = 1_000, batasBacaMs = 1_000)
        assertEquals(JenisGalat.JARINGAN, gagal(k.kirimSinkron(kunci, "m", "{}")).jenis)
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0).also { it.start() } // agar @After aman
    }

    @Test
    fun kunciKosongDanNamaModelBurukDitolakTanpaMenghubungiServer() {
        assertEquals(JenisGalat.TANPA_KUNCI, gagal(klien().kirimSinkron("   ", "m", "{}")).jenis)
        for (nama in listOf("", "a b", "../rahasia", "m?x=1", "m/../../x", "m:generate", "models/m", "-m")) {
            assertEquals(nama, JenisGalat.MODEL_TIDAK_ADA, gagal(klien().kirimSinkron(kunci, nama, "{}")).jenis)
        }
        assertEquals(0, jumlahPermintaan.get())
    }

    @Test
    fun pengalihanTidakDiikutiSehinggaKunciTidakBocorKeAlamatLain() {
        kode = 302
        balasan = ""
        headerLokasi = "http://127.0.0.1:1/penjahat"
        val g = gagal(klien().kirimSinkron(kunci, "m", "{}"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
        assertEquals(1, jumlahPermintaan.get())
    }

    @Test
    fun balasanRaksasaDipotongDanTidakMenghabiskanMemori() {
        balasan = "a".repeat(3_000_000)
        val g = gagal(klien().kirimSinkron(kunci, "m", "{}"))
        assertEquals(JenisGalat.TIDAK_DIKENAL, g.jenis)
    }

    @Test
    fun hanyaHttpsDiterimaKecualiLoopbackUntukUji() {
        KlienGemini("https://generativelanguage.googleapis.com/v1beta")
        for (alamat in listOf("http://example.com", "ftp://x", "http://localhost:80", "")) {
            try {
                KlienGemini(alamat)
                fail("Seharusnya ditolak: $alamat")
            } catch (e: IllegalArgumentException) {
                // diharapkan
            }
        }
    }

    @Test
    fun versiSuspendBekerjaDariCoroutine() = runBlocking {
        val hasil = klien().kirim(kunci, "gemini-uji", "{}")
        assertEquals(HasilGemini.Berhasil("[senyum] Halo!"), hasil)
    }
}
