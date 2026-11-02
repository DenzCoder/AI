package id.asisten.app.domain.pengaturan

enum class ModeTema { SISTEM, TERANG, GELAP }

/** Accent colors as ARGB longs so the domain layer stays free of Compose types. */
enum class Aksen(val argb: Long) {
    JINGGA(0xFFF28C38),
    UNGU(0xFF7E57C2),
    HIJAU(0xFF2E9E6B),
    BIRU(0xFF2F80ED),
}

enum class UkuranTeks(val skala: Float) {
    KECIL(0.9f),
    NORMAL(1f),
    BESAR(1.15f),
}

data class Pengaturan(
    val modeTema: ModeTema = ModeTema.SISTEM,
    val aksen: Aksen = Aksen.JINGGA,
    val ukuranTeks: UkuranTeks = UkuranTeks.NORMAL,
    val animasiAktif: Boolean = true,
)
