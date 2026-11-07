package id.asisten.app.domain.hanako

/** Why a Gemini request produced no answer. Each kind has an Indonesian message in [LabelHanako.galat]. */
enum class JenisGalat {
    TANPA_KUNCI,
    KUNCI_TIDAK_VALID,
    AKSES_DITOLAK,
    KUOTA,
    MODEL_TIDAK_ADA,
    PERMINTAAN_DITOLAK,
    DIBLOKIR,
    KOSONG,
    SERVER,
    JARINGAN,
    WAKTU_HABIS,
    TIDAK_DIKENAL,
}

sealed interface HasilGemini {
    data class Berhasil(val teks: String) : HasilGemini

    /** [rinci] is the (shortened) message the server sent, shown in small print for troubleshooting. */
    data class Gagal(val jenis: JenisGalat, val rinci: String = "") : HasilGemini
}

/** Builds the JSON body of a `generateContent` request. */
object PermintaanGemini {
    const val SUHU = 0.8

    /**
     * Thinking models spend part of this budget on reasoning before they write the answer, so it is generous.
     * The system prompt asks for short replies, so answers stay short anyway.
     */
    const val MAKS_TOKEN = 2048

    fun badan(sistem: String, riwayat: List<PesanChat>, maksToken: Int = MAKS_TOKEN): String {
        val isi = RiwayatChat.untukModel(riwayat).joinToString(",") { p ->
            val peran = if (p.peran == PeranChat.PENGGUNA) "user" else "model"
            """{"role":"$peran","parts":[{"text":${MiniJson.kutip(p.isi)}}]}"""
        }
        return """{"systemInstruction":{"parts":[{"text":${MiniJson.kutip(sistem)}}]},""" +
            """"contents":[$isi],""" +
            """"generationConfig":{"temperature":$SUHU,"maxOutputTokens":$maksToken}}"""
    }
}

/** Turns an HTTP status and body from `generateContent` into a [HasilGemini]. Pure, so it is unit-tested. */
object UraiResponsGemini {
    private const val MAKS_RINCI = 200
    private val ALASAN_DIBLOKIR = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "IMAGE_SAFETY", "RECITATION")

    fun urai(kode: Int, badan: String): HasilGemini {
        val json = MiniJson.uraiAtauNull(badan)
        return if (kode in 200..299) berhasil(json) else gagal(kode, json, badan)
    }

    private fun berhasil(json: Any?): HasilGemini {
        if (json !is Map<*, *>) return HasilGemini.Gagal(JenisGalat.TIDAK_DIKENAL, "Balasan server bukan JSON")
        val alasanBlokir = MiniJson.ambil(json, "promptFeedback", "blockReason") as? String
        if (alasanBlokir != null) return HasilGemini.Gagal(JenisGalat.DIBLOKIR, alasanBlokir)

        val bagian = MiniJson.ambil(json, "candidates", 0, "content", "parts") as? List<*> ?: emptyList<Any?>()
        val teks = bagian.mapNotNull { b ->
            val peta = b as? Map<*, *> ?: return@mapNotNull null
            if (peta["thought"] == true) null else peta["text"] as? String
        }.joinToString("").trim()
        if (teks.isNotEmpty()) return HasilGemini.Berhasil(teks)

        val selesai = MiniJson.ambil(json, "candidates", 0, "finishReason") as? String
        return if (selesai != null && selesai in ALASAN_DIBLOKIR) {
            HasilGemini.Gagal(JenisGalat.DIBLOKIR, selesai)
        } else {
            HasilGemini.Gagal(JenisGalat.KOSONG, selesai ?: "")
        }
    }

    private fun gagal(kode: Int, json: Any?, mentah: String): HasilGemini {
        val status = MiniJson.ambil(json, "error", "status") as? String ?: ""
        val pesan = (MiniJson.ambil(json, "error", "message") as? String ?: "").take(MAKS_RINCI)
        val rinci = if (pesan.isNotEmpty()) pesan else "HTTP $kode"
        val kuncinyaSalah = mentah.contains("API_KEY_INVALID", ignoreCase = true) ||
            mentah.contains("API key not valid", ignoreCase = true) ||
            mentah.contains("API key expired", ignoreCase = true)
        val jenis = when {
            kuncinyaSalah -> JenisGalat.KUNCI_TIDAK_VALID
            kode == 429 || status == "RESOURCE_EXHAUSTED" -> JenisGalat.KUOTA
            kode == 401 || kode == 403 || status == "PERMISSION_DENIED" || status == "UNAUTHENTICATED" -> JenisGalat.AKSES_DITOLAK
            kode == 404 || status == "NOT_FOUND" -> JenisGalat.MODEL_TIDAK_ADA
            kode == 400 -> JenisGalat.PERMINTAAN_DITOLAK
            kode in 500..599 -> JenisGalat.SERVER
            else -> JenisGalat.TIDAK_DIKENAL
        }
        return HasilGemini.Gagal(jenis, rinci)
    }
}

/** A reply split into its text and the mood marker the model was asked to put in front of it. */
data class BalasanUrai(val teks: String, val ekspresi: EkspresiHanako?)

object UraiBalasan {
    private val PENANDA = Regex("^\\s*\\[([A-Za-z]+)]\\s*")

    /**
     * The system prompt asks the model to start every reply with a marker such as `[senyum]`. If the reply starts
     * with a known marker it is removed and returned as the expression; otherwise the text is returned untouched
     * with a null expression (the caller then uses a neutral face rather than guessing a mood).
     */
    fun urai(mentah: String): BalasanUrai {
        val cocok = PENANDA.find(mentah)
        if (cocok != null) {
            val ekspresi = EkspresiHanako.entries.firstOrNull { it.name.equals(cocok.groupValues[1], ignoreCase = true) }
            if (ekspresi != null) return BalasanUrai(mentah.substring(cocok.range.last + 1).trim(), ekspresi)
        }
        return BalasanUrai(mentah.trim(), null)
    }
}
