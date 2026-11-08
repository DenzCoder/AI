package id.asisten.app.domain.suara

import kotlin.math.abs

/** Voice settings: whether replies are read aloud by themselves, and how the voice sounds. */
data class PengaturanSuara(
    val bacaOtomatis: Boolean = false,
    val kecepatan: Float = 1.0f,
    val nada: Float = 1.0f,
) {
    /** Stored values can be missing or odd (NaN, out of range): bring them back to what the engine accepts. */
    fun normalisasi(): PengaturanSuara = copy(kecepatan = jepit(kecepatan), nada = jepit(nada))

    private fun jepit(nilai: Float): Float =
        if (nilai.isNaN() || nilai.isInfinite()) 1.0f else nilai.coerceIn(MIN, MAKS)

    companion object {
        const val MIN = 0.5f
        const val MAKS = 2.0f
    }
}

object PilihanSuara {
    val KECEPATAN = listOf(0.8f to "Lambat", 1.0f to "Normal", 1.25f to "Cepat")
    val NADA = listOf(0.85f to "Rendah", 1.0f to "Normal", 1.15f to "Tinggi")

    /** The option closest to [nilai], so a chip is highlighted even when the stored value is not exactly one of them. */
    fun terdekat(nilai: Float, pilihan: List<Pair<Float, String>>): Float =
        pilihan.minByOrNull { abs(it.first - nilai) }!!.first
}

/** The state of the phone's text-to-speech for Indonesian. */
enum class StatusSuara {
    SIAP,
    BAHASA_TIDAK_ADA,
    DATA_BAHASA_KURANG,
    MESIN_GAGAL,
    ;

    companion object {
        // The values of TextToSpeech.LANG_*; copied here so this logic can be tested without Android.
        const val LANG_MISSING_DATA = -1
        const val LANG_NOT_SUPPORTED = -2

        /** [kodeBahasa] is the result of `TextToSpeech.isLanguageAvailable(Indonesian)`. */
        fun dari(inisialisasiBerhasil: Boolean, kodeBahasa: Int): StatusSuara = when {
            !inisialisasiBerhasil -> MESIN_GAGAL
            kodeBahasa == LANG_MISSING_DATA -> DATA_BAHASA_KURANG
            kodeBahasa >= 0 -> SIAP
            else -> BAHASA_TIDAK_ADA
        }
    }
}

object LabelSuara {
    fun status(s: StatusSuara): String = when (s) {
        StatusSuara.SIAP -> "Suara bahasa Indonesia siap dipakai."
        StatusSuara.BAHASA_TIDAK_ADA -> "Mesin suara di ponsel ini tidak mendukung bahasa Indonesia."
        StatusSuara.DATA_BAHASA_KURANG -> "Data suara bahasa Indonesia belum terpasang di ponsel ini."
        StatusSuara.MESIN_GAGAL -> "Mesin suara di ponsel ini tidak bisa dijalankan."
    }

    /** Only missing voice data can be fixed from the app, by opening the system's install screen. */
    fun bisaPasangData(s: StatusSuara): Boolean = s == StatusSuara.DATA_BAHASA_KURANG

    const val DIKTEAN_TIDAK_ADA =
        "Pengenalan suara tidak tersedia di ponsel ini. Biasanya butuh aplikasi Google dan koneksi internet."
    const val DIKTEAN_KOSONG = "Suaramu tidak terdengar jelas. Coba lagi."
}

/** Puts dictated text into the chat input field. */
object PenyisipDiktean {
    const val MAKS_PANJANG = 2000

    /** The best guess of the recognizer: the first non-blank result. */
    fun pilihHasil(hasil: List<String>?): String? = hasil?.firstOrNull { it.isNotBlank() }?.trim()

    /**
     * Appends [diktean] to [lama] with a single space between them. When the field is empty or the old text ends a
     * sentence, the first letter is capitalised, since recognizers usually return all-lowercase text.
     */
    fun gabung(lama: String, diktean: String): String {
        val baru = diktean.trim()
        if (baru.isEmpty()) return lama
        val awalKalimat = lama.isBlank() || lama.trimEnd().last() in ".!?"
        val teks = if (awalKalimat) baru.replaceFirstChar { it.uppercase() } else baru
        val gabungan = if (lama.isBlank()) teks else lama.trimEnd() + " " + teks
        return gabungan.take(MAKS_PANJANG)
    }
}
