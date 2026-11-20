package id.asisten.app.domain.hanako

/** Why a request to an AI provider produced no answer. Each kind has an Indonesian message in [LabelHanako.galat]. */
enum class JenisGalat {
    TANPA_KUNCI,
    KUNCI_TIDAK_VALID,
    AKSES_DITOLAK,
    KUOTA,

    /** The account has no balance left (DeepSeek answers HTTP 402). Retrying cannot help. */
    SALDO_HABIS,
    MODEL_TIDAK_ADA,
    PERMINTAAN_DITOLAK,
    DIBLOKIR,
    KOSONG,
    SERVER,
    JARINGAN,
    WAKTU_HABIS,
    TIDAK_DIKENAL,
}

sealed interface HasilAi {
    /**
     * [teks] may be empty when the model only asked for changes ([panggilan]). A reply always has text or calls.
     * The calls are proposals; nothing in them has been done.
     */
    data class Berhasil(val teks: String, val panggilan: List<PanggilanFungsi> = emptyList()) : HasilAi

    /** [rinci] is the (shortened) message the server sent, shown in small print for troubleshooting. */
    data class Gagal(val jenis: JenisGalat, val rinci: String = "") : HasilAi
}
