package id.asisten.app.domain.memori

/** The four memory categories required by the product spec. */
enum class KategoriMemori { PREFERENSI, KEBIASAAN, TUJUAN, KONTEKS }

/**
 * One remembered fact. Everything stored today is typed in by the user (so it counts as confirmed);
 * automatic learning with "guess" status arrives in Phase 9 together with the columns it needs.
 */
data class ItemMemori(
    val id: Long = 0,
    val kategori: KategoriMemori,
    val isi: String,
    val sensitif: Boolean = false,
    val dibuat: Long = 0,
)

object ValidasiMemori {
    const val MAKS = 500

    /** Trims surrounding whitespace and limits the length. An empty result means "nothing to save". */
    fun bersihkan(mentah: String): String = mentah.trim().take(MAKS).trim()
}

object KonteksAi {
    /**
     * Picks the memories that may be handed to the AI.
     * - Nothing at all when memory is switched off, even if sensitive data is explicitly requested.
     * - Sensitive memories are left out of ordinary AI context unless [sertakanSensitif] is true.
     */
    fun pilih(
        daftar: List<ItemMemori>,
        memoriAktif: Boolean,
        sertakanSensitif: Boolean = false,
    ): List<ItemMemori> {
        if (!memoriAktif) return emptyList()
        return daftar.filter { sertakanSensitif || !it.sensitif }
    }
}
