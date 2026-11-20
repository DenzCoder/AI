package id.asisten.app.data.ai

import id.asisten.app.domain.hanako.HasilAi
import id.asisten.app.domain.hanako.JenisGalat
import id.asisten.app.domain.hanako.NamaModel
import id.asisten.app.domain.hanako.PenyediaAi
import id.asisten.app.domain.hanako.UraiResponsAi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calls Gemini (`generateContent`), Groq and DeepSeek (OpenAI-style `chat/completions`) with plain
 * `HttpURLConnection` (no extra library).
 *
 * Safety rules:
 * - the API key travels only in a header (`x-goog-api-key` for Gemini, `Authorization: Bearer` for the others),
 *   never in the URL, and is never logged;
 * - every provider has one fixed base address ([PenyediaAi.urlDasar]), so a key can only go to its own provider;
 * - for Gemini the model name becomes part of the URL path, so it must match [NamaModel.sah];
 * - redirects are not followed (a redirect must never carry the key to another host);
 * - the connection must be HTTPS (plain http only for a loopback test server);
 * - responses are read up to [MAKS_BYTE] bytes, so a broken server cannot exhaust memory.
 *
 * [alamat] replaces a provider's base address; it exists so the tests can point at a loopback server.
 */
class KlienAi(
    private val alamat: Map<PenyediaAi, String> = emptyMap(),
    private val batasSambungMs: Int = 10_000,
    private val batasBacaMs: Int = 45_000,
) {
    init {
        for (url in alamat.values) {
            require(url.startsWith("https://") || url.startsWith("http://127.0.0.1")) { "Alamat harus HTTPS" }
        }
    }

    private fun alamatPermintaan(penyedia: PenyediaAi, model: String): String {
        val dasar = alamat[penyedia] ?: penyedia.urlDasar
        return if (penyedia.gayaOpenAi) "$dasar/chat/completions" else "$dasar/models/$model:generateContent"
    }

    suspend fun kirim(penyedia: PenyediaAi, kunci: String, model: String, badan: String): HasilAi =
        withContext(Dispatchers.IO) { kirimSinkron(penyedia, kunci, model, badan) }

    /** Blocking version (public so the JVM tests can call it). Never throws: every failure becomes a [HasilAi.Gagal]. */
    fun kirimSinkron(penyedia: PenyediaAi, kunci: String, model: String, badan: String): HasilAi {
        if (kunci.isBlank()) return HasilAi.Gagal(JenisGalat.TANPA_KUNCI)
        if (!NamaModel.sah(model, penyedia)) return HasilAi.Gagal(JenisGalat.MODEL_TIDAK_ADA, "Nama model tidak valid")

        var koneksi: HttpURLConnection? = null
        return try {
            koneksi = (URI(alamatPermintaan(penyedia, model)).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = batasSambungMs
                readTimeout = batasBacaMs
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (penyedia.gayaOpenAi) setRequestProperty("Authorization", "Bearer $kunci")
                else setRequestProperty("x-goog-api-key", kunci)
            }
            koneksi.outputStream.use { it.write(badan.toByteArray(Charsets.UTF_8)) }
            val kode = koneksi.responseCode
            val aliran = if (kode in 200..299) koneksi.inputStream else koneksi.errorStream
            val teks = aliran?.use { baca(it) } ?: ""
            when (val hasil = UraiResponsAi.urai(penyedia, kode, teks)) {
                is HasilAi.Gagal -> hasil.copy(rinci = hasil.rinci.replace(kunci, "***"))
                is HasilAi.Berhasil -> hasil
            }
        } catch (e: SocketTimeoutException) {
            HasilAi.Gagal(JenisGalat.WAKTU_HABIS)
        } catch (e: UnknownHostException) {
            HasilAi.Gagal(JenisGalat.JARINGAN)
        } catch (e: ConnectException) {
            HasilAi.Gagal(JenisGalat.JARINGAN)
        } catch (e: SSLException) {
            HasilAi.Gagal(JenisGalat.JARINGAN, "Sambungan aman gagal")
        } catch (e: IOException) {
            HasilAi.Gagal(JenisGalat.JARINGAN)
        } catch (e: Exception) {
            HasilAi.Gagal(JenisGalat.TIDAK_DIKENAL, e.javaClass.simpleName)
        } finally {
            koneksi?.disconnect()
        }
    }

    private fun baca(aliran: InputStream): String {
        val keluar = ByteArrayOutputStream()
        val penyangga = ByteArray(8 * 1024)
        while (keluar.size() < MAKS_BYTE) {
            val n = aliran.read(penyangga)
            if (n < 0) break
            keluar.write(penyangga, 0, n)
        }
        return keluar.toString(Charsets.UTF_8.name())
    }

    companion object {
        const val MAKS_BYTE = 1_000_000
    }
}
