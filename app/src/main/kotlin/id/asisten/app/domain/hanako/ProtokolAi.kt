package id.asisten.app.domain.hanako

/** Settings shared by every provider's request, plus the one entry point that picks the right protocol. */
object PermintaanAi {
    const val SUHU = 0.8

    /** Lower when the model may call functions: dates and ids must be exact, not creative. */
    const val SUHU_ALAT = 0.4

    /**
     * Thinking models spend part of this budget on reasoning before they write the answer, so it is generous.
     * The system prompt asks for short replies, so answers stay short anyway.
     */
    const val MAKS_TOKEN = 2048

    /**
     * Builds the JSON body for [penyedia]. [model] only matters for the OpenAI-style services (Gemini carries the
     * model in the URL). [alat] adds the function declarations so the model can propose changes to the app.
     */
    fun badan(
        penyedia: PenyediaAi,
        model: String,
        sistem: String,
        riwayat: List<PesanChat>,
        maksToken: Int = MAKS_TOKEN,
        alat: Boolean = false,
    ): String =
        if (penyedia.gayaOpenAi) PermintaanOpenAi.badan(penyedia, model, sistem, riwayat, maksToken, alat)
        else PermintaanGemini.badan(sistem, riwayat, maksToken, alat)
}

/** Reads a provider's HTTP answer. Pure, so it is unit-tested without any network. */
object UraiResponsAi {
    fun urai(penyedia: PenyediaAi, kode: Int, badan: String): HasilAi =
        if (penyedia.gayaOpenAi) UraiResponsOpenAi.urai(kode, badan) else UraiResponsGemini.urai(kode, badan)
}

/** Request body of an OpenAI-style `chat/completions` call, as used by Groq and DeepSeek. */
object PermintaanOpenAi {

    fun badan(
        penyedia: PenyediaAi,
        model: String,
        sistem: String,
        riwayat: List<PesanChat>,
        maksToken: Int = PermintaanAi.MAKS_TOKEN,
        alat: Boolean = false,
    ): String {
        require(penyedia.gayaOpenAi) { "${penyedia.nama} tidak memakai format ini" }
        val pesan = buildList<Any?> {
            add(mapOf("role" to "system", "content" to sistem))
            for (p in RiwayatChat.untukModel(riwayat)) {
                add(mapOf("role" to if (p.peran == PeranChat.PENGGUNA) "user" else "assistant", "content" to p.isi))
            }
        }
        val badan = linkedMapOf<String, Any?>(
            "model" to model,
            "messages" to pesan,
            "temperature" to if (alat) PermintaanAi.SUHU_ALAT else PermintaanAi.SUHU,
            // Groq renamed this field; DeepSeek still uses the classic one.
            (if (penyedia == PenyediaAi.GROQ) "max_completion_tokens" else "max_tokens") to maksToken,
            "stream" to false,
        )
        // DeepSeek reasons by default, which is slow and spends the token budget before any answer appears.
        if (penyedia == PenyediaAi.DEEPSEEK) badan["thinking"] = mapOf("type" to "disabled")
        if (alat) badan["tools"] = DeklarasiAksi.alatOpenAi
        return MiniJson.tulis(badan)
    }
}

/** Turns an HTTP status and body from `chat/completions` into a [HasilAi]. */
object UraiResponsOpenAi {
    private const val MAKS_RINCI = 200
    private val BLOK_PIKIR = Regex("<think>.*?</think>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    fun urai(kode: Int, badan: String): HasilAi {
        val json = MiniJson.uraiAtauNull(badan)
        return if (kode in 200..299) berhasil(kode, json) else gagal(kode, json)
    }

    /** Some models print their reasoning inside `<think>` tags in the content; the user should not see it. */
    private fun bersihkanPikiran(teks: String): String {
        val tanpaBlok = BLOK_PIKIR.replace(teks, "")
        val buka = tanpaBlok.indexOf("<think>", ignoreCase = true)
        return (if (buka >= 0) tanpaBlok.substring(0, buka) else tanpaBlok).trim()
    }

    private fun berhasil(kode: Int, json: Any?): HasilAi {
        if (json !is Map<*, *>) return HasilAi.Gagal(JenisGalat.TIDAK_DIKENAL, "Balasan server bukan JSON")
        if (json["choices"] == null && json["error"] != null) return gagal(kode, json)

        val pesan = MiniJson.ambil(json, "choices", 0, "message") as? Map<*, *>
        val teks = bersihkanPikiran(pesan?.get("content") as? String ?: "")

        var adaPanggilan = 0
        val panggilan = ArrayList<PanggilanFungsi>()
        for (p in (pesan?.get("tool_calls") as? List<*>) ?: emptyList<Any?>()) {
            adaPanggilan++
            val fungsi = (p as? Map<*, *>)?.get("function") as? Map<*, *> ?: continue
            val nama = fungsi["name"] as? String ?: continue
            val args = uraiArgumen(fungsi["arguments"]) ?: continue
            panggilan.add(PanggilanFungsi(nama, args))
        }
        if (teks.isNotEmpty() || panggilan.isNotEmpty()) return HasilAi.Berhasil(teks, panggilan)

        val selesai = MiniJson.ambil(json, "choices", 0, "finish_reason") as? String
        return when {
            selesai == "content_filter" -> HasilAi.Gagal(JenisGalat.DIBLOKIR, selesai)
            // The model tried to call something but the arguments were not a JSON object: nothing safe to propose.
            adaPanggilan > 0 -> HasilAi.Gagal(JenisGalat.KOSONG, "Usulan perubahan rusak")
            else -> HasilAi.Gagal(JenisGalat.KOSONG, selesai ?: "")
        }
    }

    /**
     * `arguments` is a JSON *string* in this protocol. An empty string means "no arguments"; text that is not a JSON
     * object returns null so the call is dropped instead of being read as "no arguments".
     */
    private fun uraiArgumen(mentah: Any?): Map<String, Any?>? {
        val peta: Map<*, *> = when (mentah) {
            null -> return emptyMap()
            is Map<*, *> -> mentah
            is String -> if (mentah.isBlank()) return emptyMap() else MiniJson.uraiAtauNull(mentah) as? Map<*, *> ?: return null
            else -> return null
        }
        return peta.entries.filter { it.key is String }.associate { it.key as String to it.value }
    }

    private fun gagal(kode: Int, json: Any?): HasilAi {
        val galat = MiniJson.ambil(json, "error")
        val pesan = ((if (galat is String) galat else MiniJson.ambil(galat, "message") as? String) ?: "").take(MAKS_RINCI)
        val sandi = ((MiniJson.ambil(galat, "code") as? String) ?: (MiniJson.ambil(galat, "type") as? String) ?: "").lowercase()
        val rinci = if (pesan.isNotEmpty()) pesan else "HTTP $kode"
        val kecil = pesan.lowercase()

        val modelHilang = kode == 404 || sandi == "model_not_found" || sandi == "model_decommissioned" ||
            (kecil.contains("model") && (kecil.contains("not exist") || kecil.contains("does not exist") ||
                kecil.contains("not found") || kecil.contains("decommissioned")))
        val jenis = when {
            kode == 401 || sandi == "invalid_api_key" || kecil.contains("invalid api key") -> JenisGalat.KUNCI_TIDAK_VALID
            kode == 402 || kecil.contains("insufficient balance") -> JenisGalat.SALDO_HABIS
            kode == 429 -> JenisGalat.KUOTA
            kode == 403 -> JenisGalat.AKSES_DITOLAK
            modelHilang -> JenisGalat.MODEL_TIDAK_ADA
            kode == 400 || kode == 413 || kode == 422 -> JenisGalat.PERMINTAAN_DITOLAK
            kode in 500..599 -> JenisGalat.SERVER
            else -> JenisGalat.TIDAK_DIKENAL
        }
        return HasilAi.Gagal(jenis, rinci)
    }
}
