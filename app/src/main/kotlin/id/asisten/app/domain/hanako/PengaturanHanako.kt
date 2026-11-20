package id.asisten.app.domain.hanako

/**
 * Hanako chat settings. The API key itself is never part of this object: only whether one is stored.
 * [onlineAktif] is the user's switch; the chat only goes online when a key exists too ([online]).
 * [penyedia] is the AI service in use; [model] and [adaKunci] belong to that service.
 */
data class PengaturanHanako(
    val onlineAktif: Boolean = false,
    val bagikanData: Boolean = true,
    val penyedia: PenyediaAi = PenyediaAi.GEMINI,
    val model: String = penyedia.modelBawaan,
    val adaKunci: Boolean = false,
    /** Lets Hanako propose changes to the app (the user still approves each one). Only matters in online mode. */
    val aksiAktif: Boolean = true,
    /** Every service that has a stored key, so the settings can show which ones are ready to switch to. */
    val penyediaBerkunci: Set<PenyediaAi> = emptySet(),
) {
    val online: Boolean get() = onlineAktif && adaKunci

    /** True when requests carry the function declarations, so Hanako can propose changes. */
    val bisaMengusulkan: Boolean get() = online && aksiAktif
}

object NamaModel {
    /**
     * An alias that Google points at its current Flash model, so the app does not break when a specific model is
     * retired. It could not be checked against Google's own documentation while writing this (only third-party
     * pages were reachable), which is why the name can be edited in the settings.
     */
    const val BAWAAN = "gemini-flash-latest"

    /** Gemini puts the name in the URL path, so it gets the strictest pattern. */
    private val POLA_GEMINI = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")

    /** Groq and DeepSeek send the name inside the JSON body, where a slash (`openai/gpt-oss-20b`) is normal. */
    private val POLA_BODI = Regex("^[A-Za-z0-9][A-Za-z0-9._/-]{0,63}$")

    /** Trims and cuts to 64 characters; for Gemini it also accepts a pasted `models/<name>`. */
    fun bersihkan(mentah: String, penyedia: PenyediaAi = PenyediaAi.GEMINI): String {
        val rapi = mentah.trim()
        val tanpaAwalan = if (penyedia == PenyediaAi.GEMINI) rapi.removePrefix("models/").trim() else rapi
        return tanpaAwalan.take(64)
    }

    fun sah(nama: String, penyedia: PenyediaAi = PenyediaAi.GEMINI): Boolean =
        (if (penyedia == PenyediaAi.GEMINI) POLA_GEMINI else POLA_BODI).matches(nama)
}

object KunciAi {
    const val MIN_PANJANG = 20
    const val MAKS_PANJANG = 256
    private val POLA = Regex("^[A-Za-z0-9_.~-]+$")

    /** A pasted key often carries spaces, line breaks or quotes: remove them. */
    fun bersihkan(mentah: String): String =
        mentah.filter { !it.isWhitespace() && it != '"' && it != '\'' }.take(MAKS_PANJANG + 1)

    /** A plausibility check only (the provider decides whether the key really works). */
    fun masukAkal(kunci: String): Boolean =
        kunci.length in MIN_PANJANG..MAKS_PANJANG && POLA.matches(kunci)
}
