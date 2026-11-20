package id.asisten.app.domain.hanako

/** Builds the JSON body of a `generateContent` request. */
object PermintaanGemini {
    const val SUHU = PermintaanAi.SUHU
    const val SUHU_ALAT = PermintaanAi.SUHU_ALAT
    const val MAKS_TOKEN = PermintaanAi.MAKS_TOKEN

    /**
     * [alat] adds the function declarations of [DeklarasiAksi], so the model can propose changes to the app.
     * Plain chat tests (like the connection test) leave it off.
     */
    fun badan(sistem: String, riwayat: List<PesanChat>, maksToken: Int = MAKS_TOKEN, alat: Boolean = false): String {
        val isi = RiwayatChat.untukModel(riwayat).joinToString(",") { p ->
            val peran = if (p.peran == PeranChat.PENGGUNA) "user" else "model"
            """{"role":"$peran","parts":[{"text":${MiniJson.kutip(p.isi)}}]}"""
        }
        return """{"systemInstruction":{"parts":[{"text":${MiniJson.kutip(sistem)}}]},""" +
            """"contents":[$isi],""" +
            (if (alat) """"tools":${DeklarasiAksi.json()},""" else "") +
            """"generationConfig":{"temperature":${if (alat) SUHU_ALAT else SUHU},"maxOutputTokens":$maksToken}}"""
    }
}

/** Turns an HTTP status and body from `generateContent` into a [HasilAi]. Pure, so it is unit-tested. */
object UraiResponsGemini {
    private const val MAKS_RINCI = 200
    private val ALASAN_DIBLOKIR = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "IMAGE_SAFETY", "RECITATION")

    fun urai(kode: Int, badan: String): HasilAi {
        val json = MiniJson.uraiAtauNull(badan)
        return if (kode in 200..299) berhasil(json) else gagal(kode, json, badan)
    }

    private fun berhasil(json: Any?): HasilAi {
        if (json !is Map<*, *>) return HasilAi.Gagal(JenisGalat.TIDAK_DIKENAL, "Balasan server bukan JSON")
        val alasanBlokir = MiniJson.ambil(json, "promptFeedback", "blockReason") as? String
        if (alasanBlokir != null) return HasilAi.Gagal(JenisGalat.DIBLOKIR, alasanBlokir)

        val bagian = MiniJson.ambil(json, "candidates", 0, "content", "parts") as? List<*> ?: emptyList<Any?>()
        val teks = bagian.mapNotNull { b ->
            val peta = b as? Map<*, *> ?: return@mapNotNull null
            if (peta["thought"] == true) null else peta["text"] as? String
        }.joinToString("").trim()
        val panggilan = bagian.mapNotNull { b ->
            val panggil = (b as? Map<*, *>)?.get("functionCall") as? Map<*, *> ?: return@mapNotNull null
            val nama = panggil["name"] as? String ?: return@mapNotNull null
            val args = (panggil["args"] as? Map<*, *>)
                ?.entries?.filter { it.key is String }?.associate { it.key as String to it.value }
                ?: emptyMap()
            PanggilanFungsi(nama, args)
        }
        if (teks.isNotEmpty() || panggilan.isNotEmpty()) return HasilAi.Berhasil(teks, panggilan)

        val selesai = MiniJson.ambil(json, "candidates", 0, "finishReason") as? String
        return if (selesai != null && selesai in ALASAN_DIBLOKIR) {
            HasilAi.Gagal(JenisGalat.DIBLOKIR, selesai)
        } else {
            HasilAi.Gagal(JenisGalat.KOSONG, selesai ?: "")
        }
    }

    private fun gagal(kode: Int, json: Any?, mentah: String): HasilAi {
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
        return HasilAi.Gagal(jenis, rinci)
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
