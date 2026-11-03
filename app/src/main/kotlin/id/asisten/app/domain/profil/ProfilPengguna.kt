package id.asisten.app.domain.profil

data class ProfilPengguna(
    val nama: String = "",
    val onboardingSelesai: Boolean = false,
    val memoriAktif: Boolean = true,
)

object NamaPengguna {
    const val MAKS = 30

    /** Trims, collapses inner whitespace and limits the length. */
    fun bersihkan(mentah: String): String =
        mentah.trim().replace(Regex("\\s+"), " ").take(MAKS).trim()

    fun valid(mentah: String): Boolean = bersihkan(mentah).isNotEmpty()
}
