package id.asisten.app.data.ai

import id.asisten.app.domain.hanako.HasilGemini
import id.asisten.app.domain.hanako.JenisGalat
import id.asisten.app.domain.hanako.NamaModel
import id.asisten.app.domain.hanako.UraiResponsGemini
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
 * Calls the Gemini `generateContent` REST endpoint with plain `HttpURLConnection` (no extra library).
 *
 * Safety rules:
 * - the API key travels only in the `x-goog-api-key` header, never in the URL, and is never logged;
 * - the model name must match [NamaModel.sah] because it becomes part of the URL path;
 * - redirects are not followed (a redirect must never carry the key to another host);
 * - the connection must be HTTPS (plain http only for a loopback test server);
 * - responses are read up to [MAKS_BYTE] bytes, so a broken server cannot exhaust memory.
 */
class KlienGemini(
    private val urlDasar: String = URL_GOOGLE,
    private val batasSambungMs: Int = 10_000,
    private val batasBacaMs: Int = 45_000,
) {
    init {
        require(urlDasar.startsWith("https://") || urlDasar.startsWith("http://127.0.0.1")) {
            "Alamat harus HTTPS"
        }
    }

    suspend fun kirim(kunci: String, model: String, badan: String): HasilGemini =
        withContext(Dispatchers.IO) { kirimSinkron(kunci, model, badan) }

    /** Blocking version (public so the JVM tests can call it). Never throws: every failure becomes a [HasilGemini.Gagal]. */
    fun kirimSinkron(kunci: String, model: String, badan: String): HasilGemini {
        if (kunci.isBlank()) return HasilGemini.Gagal(JenisGalat.TANPA_KUNCI)
        if (!NamaModel.sah(model)) return HasilGemini.Gagal(JenisGalat.MODEL_TIDAK_ADA, "Nama model tidak valid")

        var koneksi: HttpURLConnection? = null
        return try {
            koneksi = (URI("$urlDasar/models/$model:generateContent").toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = batasSambungMs
                readTimeout = batasBacaMs
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-goog-api-key", kunci)
            }
            koneksi.outputStream.use { it.write(badan.toByteArray(Charsets.UTF_8)) }
            val kode = koneksi.responseCode
            val aliran = if (kode in 200..299) koneksi.inputStream else koneksi.errorStream
            val teks = aliran?.use { baca(it) } ?: ""
            when (val hasil = UraiResponsGemini.urai(kode, teks)) {
                is HasilGemini.Gagal -> hasil.copy(rinci = hasil.rinci.replace(kunci, "***"))
                is HasilGemini.Berhasil -> hasil
            }
        } catch (e: SocketTimeoutException) {
            HasilGemini.Gagal(JenisGalat.WAKTU_HABIS)
        } catch (e: UnknownHostException) {
            HasilGemini.Gagal(JenisGalat.JARINGAN)
        } catch (e: ConnectException) {
            HasilGemini.Gagal(JenisGalat.JARINGAN)
        } catch (e: SSLException) {
            HasilGemini.Gagal(JenisGalat.JARINGAN, "Sambungan aman gagal")
        } catch (e: IOException) {
            HasilGemini.Gagal(JenisGalat.JARINGAN)
        } catch (e: Exception) {
            HasilGemini.Gagal(JenisGalat.TIDAK_DIKENAL, e.javaClass.simpleName)
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
        const val URL_GOOGLE = "https://generativelanguage.googleapis.com/v1beta"
        const val MAKS_BYTE = 1_000_000
    }
}
