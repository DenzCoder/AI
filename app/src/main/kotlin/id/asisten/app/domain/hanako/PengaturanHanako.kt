package id.asisten.app.domain.hanako

/**
 * Hanako chat settings. The API key itself is never part of this object: only whether one is stored.
 * [onlineAktif] is the user's switch; the chat only goes online when a key exists too ([online]).
 */
data class PengaturanHanako(
    val onlineAktif: Boolean = false,
    val bagikanData: Boolean = true,
    val model: String = NamaModel.BAWAAN,
    val adaKunci: Boolean = false,
) {
    val online: Boolean get() = onlineAktif && adaKunci
}

object NamaModel {
    /**
     * An alias that Google points at its current Flash model, so the app does not break when a specific model is
     * retired. It could not be checked against Google's own documentation while writing this (only third-party
     * pages were reachable), which is why the name can be edited in the settings.
     */
    const val BAWAAN = "gemini-flash-latest"

    private val POLA = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")

    /** Trims, accepts a pasted `models/<name>` and cuts to 64 characters. */
    fun bersihkan(mentah: String): String = mentah.trim().removePrefix("models/").trim().take(64)

    /** Only letters, digits, dot, dash and underscore: the name goes into the request URL path. */
    fun sah(nama: String): Boolean = POLA.matches(nama)
}

object KunciGemini {
    const val MIN_PANJANG = 20
    const val MAKS_PANJANG = 256
    private val POLA = Regex("^[A-Za-z0-9_.~-]+$")

    /** A pasted key often carries spaces, line breaks or quotes: remove them. */
    fun bersihkan(mentah: String): String =
        mentah.filter { !it.isWhitespace() && it != '"' && it != '\'' }.take(MAKS_PANJANG + 1)

    /** A plausibility check only (Google decides whether the key really works). */
    fun masukAkal(kunci: String): Boolean =
        kunci.length in MIN_PANJANG..MAKS_PANJANG && POLA.matches(kunci)
}
