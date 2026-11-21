package id.asisten.app.domain.suara

/**
 * One voice reported by the phone's speech engine. [id] is the engine's own name for it (for example
 * `id-id-x-idc-network`), [kualitas] is Android's `Voice.getQuality()` (100 very low .. 500 very high).
 * Android does not say whether a voice is female or male, so the app never claims it.
 */
data class InfoVoice(val id: String, val kualitas: Int, val perluInternet: Boolean)

object DaftarVoice {
    /** Java reports Indonesian as "in" on older Android versions and "id" on newer ones. */
    fun bahasaIndonesia(kodeBahasa: String?): Boolean = kodeBahasa == "id" || kodeBahasa == "in"

    /**
     * Sorted by [InfoVoice.id] and without repeats, so "Suara 2" stays the same voice every time the screen opens
     * (engines return voices in no fixed order).
     */
    fun susun(mentah: Collection<InfoVoice>): List<InfoVoice> =
        mentah.filter { it.id.isNotBlank() }.distinctBy { it.id }.sortedBy { it.id }

    /** The stored voice if the engine still has it, otherwise null (use the default voice). */
    fun yangDipilih(daftar: List<InfoVoice>, tersimpan: String?): InfoVoice? =
        if (tersimpan == null) null else daftar.firstOrNull { it.id == tersimpan }

    fun nama(urutan: Int): String = "Suara $urutan"

    fun keterangan(v: InfoVoice): String {
        val mutu = when {
            v.kualitas >= 400 -> "kualitas tinggi"
            v.kualitas >= 300 -> "kualitas normal"
            else -> "kualitas sederhana"
        }
        return mutu + if (v.perluInternet) ", butuh internet" else ", bisa tanpa internet"
    }
}
