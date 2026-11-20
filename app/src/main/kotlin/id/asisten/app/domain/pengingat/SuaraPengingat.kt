package id.asisten.app.domain.pengingat

/** A tone that ships inside the app. [kode] is stored on the phone, so it must never change. */
data class NadaAplikasi(val kode: String, val nama: String, val keterangan: String)

object DaftarNada {
    val SEMUA: List<NadaAplikasi> = listOf(
        NadaAplikasi("lonceng", "Lonceng", "Denting lonceng yang jernih."),
        NadaAplikasi("kicau", "Kicau", "Tiga kicauan burung yang pendek."),
        NadaAplikasi("kotak_musik", "Kotak musik", "Melodi kecil yang manis."),
        NadaAplikasi("bel_sekolah", "Bel sekolah", "Dering bel yang panjang."),
        NadaAplikasi("alarm_digital", "Alarm digital", "Bip-bip yang tegas."),
    )

    fun cari(kode: String?): NadaAplikasi? = SEMUA.firstOrNull { it.kode == kode }
}

/** What the reminder notification plays. */
sealed interface SuaraPengingat {
    /** The phone's own default notification sound (what the app used before this setting existed). */
    data object Bawaan : SuaraPengingat

    /** One of [DaftarNada]. */
    data class Aplikasi(val kode: String) : SuaraPengingat

    /** A tone picked in the system ringtone picker. The file belongs to the system: the app never deletes it. */
    data class Sistem(val uri: String, val nama: String) : SuaraPengingat

    /** Music the user picked from the phone. The app keeps its own copy in the media store ([uri]) and may delete it. */
    data class Berkas(val uri: String, val nama: String) : SuaraPengingat
}

/** The sound and whether it should follow the alarm volume instead of the notification volume. */
data class PengaturanSuaraPengingat(
    val suara: SuaraPengingat = SuaraPengingat.Bawaan,
    val volumeAlarm: Boolean = false,
)

/**
 * Stores a [SuaraPengingat] as one line of text. Anything unreadable becomes [SuaraPengingat.Bawaan], never a crash:
 * a wrong sound is better than no reminder.
 */
object SuaraPengingatCodec {
    private const val PEMISAH = "|"
    const val MAKS_URI = 512
    const val MAKS_NAMA = 80

    fun uriSah(uri: String): Boolean =
        uri.startsWith("content://") && uri.length <= MAKS_URI &&
            uri.none { it.isWhitespace() || it.isISOControl() || it == '|' }

    /** Control characters become spaces, runs of spaces collapse, the length is limited. */
    fun bersihkanNama(mentah: String): String {
        val rapi = mentah.map { if (it.isISOControl()) ' ' else it }.joinToString("")
            .replace(Regex("\\s+"), " ").trim().take(MAKS_NAMA).trim()
        return rapi.ifEmpty { "Tanpa nama" }
    }

    /** Null when [uri] is not a content address the notification system can read. */
    fun sistem(uri: String, nama: String): SuaraPengingat.Sistem? =
        if (uriSah(uri)) SuaraPengingat.Sistem(uri, bersihkanNama(nama)) else null

    fun berkas(uri: String, nama: String): SuaraPengingat.Berkas? =
        if (uriSah(uri)) SuaraPengingat.Berkas(uri, bersihkanNama(nama)) else null

    fun encode(suara: SuaraPengingat): String = when (suara) {
        SuaraPengingat.Bawaan -> "bawaan"
        is SuaraPengingat.Aplikasi -> "app$PEMISAH${suara.kode}"
        is SuaraPengingat.Sistem -> "sistem$PEMISAH${suara.uri}$PEMISAH${suara.nama}"
        is SuaraPengingat.Berkas -> "berkas$PEMISAH${suara.uri}$PEMISAH${suara.nama}"
    }

    fun decode(teks: String?): SuaraPengingat {
        if (teks.isNullOrEmpty()) return SuaraPengingat.Bawaan
        val bagian = teks.split(PEMISAH, limit = 3)
        return when (bagian[0]) {
            "app" -> bagian.getOrNull(1)?.let { DaftarNada.cari(it) }?.let { SuaraPengingat.Aplikasi(it.kode) }
            "sistem" -> if (bagian.size == 3) sistem(bagian[1], bagian[2]) else null
            "berkas" -> if (bagian.size == 3) berkas(bagian[1], bagian[2]) else null
            else -> null
        } ?: SuaraPengingat.Bawaan
    }
}

/**
 * Android fixes a notification channel's sound when the channel is created, so every different sound needs its own
 * channel. The id is derived from the choice; channels of earlier choices are removed ([usang]).
 */
object SaluranSuara {
    /** The original channel: the default sound at notification volume. Kept so existing installs keep their settings. */
    const val ID_BAWAAN = "pengingat"
    const val AWALAN = "pengingat_"

    /** The channel without sound (quiet hours, Fokus, Istirahat). It does not depend on the chosen sound. */
    const val ID_SENYAP = "pengingat_senyap"

    fun id(p: PengaturanSuaraPengingat): String =
        if (p.suara == SuaraPengingat.Bawaan && !p.volumeAlarm) ID_BAWAAN
        else AWALAN + hashFnv64("${SuaraPengingatCodec.encode(p.suara)}#${p.volumeAlarm}")

    /** The name Android shows in the phone's notification settings. */
    fun nama(p: PengaturanSuaraPengingat): String =
        if (id(p) == ID_BAWAAN) "Pengingat" else "Pengingat (${LabelSuaraPengingat.nama(p.suara)})"

    /** True for a reminder channel from an earlier sound choice, which can be deleted. */
    fun usang(idAda: String, idSekarang: String): Boolean =
        idAda != idSekarang && idAda != ID_SENYAP && (idAda == ID_BAWAAN || idAda.startsWith(AWALAN))

    /** FNV-1a, 64 bit, as 16 hex digits: stable across runs and devices (String.hashCode is too short for ids). Public so it can be tested against the published vectors. */
    fun hashFnv64(teks: String): String {
        var h = -3750763034362895579L // 0xcbf29ce484222325
        for (b in teks.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toLong() and 0xff)
            h *= 1099511628211L
        }
        return java.lang.Long.toHexString(h).padStart(16, '0')
    }
}

object LabelSuaraPengingat {
    fun nama(suara: SuaraPengingat): String = when (suara) {
        SuaraPengingat.Bawaan -> "Bawaan ponsel"
        is SuaraPengingat.Aplikasi -> DaftarNada.cari(suara.kode)?.nama ?: "Bawaan ponsel"
        is SuaraPengingat.Sistem -> suara.nama
        is SuaraPengingat.Berkas -> suara.nama
    }

    /** One line for the settings screen, for example "Lonceng · volume alarm". */
    fun ringkas(p: PengaturanSuaraPengingat): String =
        nama(p.suara) + if (p.volumeAlarm) " · volume alarm" else ""

    /** What the user reads when a picked music file is refused. */
    fun tolak(alasan: AlasanTolakBerkas): String = when (alasan) {
        AlasanTolakBerkas.BUKAN_AUDIO -> "Berkas itu bukan audio. Pilih berkas musik seperti mp3, m4a, wav, atau ogg."
        AlasanTolakBerkas.KOSONG -> "Berkas itu kosong. Pilih berkas lain."
        AlasanTolakBerkas.TERLALU_BESAR -> "Berkas terlalu besar (maksimal 15 MB). Pilih yang lebih kecil atau potong lagunya dulu."
    }

    const val GAGAL_SALIN = "Berkas itu tidak bisa disalin. Coba pilih berkas lain."
    const val GAGAL_NADA = "Nada itu tidak bisa dipakai. Pilih nada lain."
    const val GAGAL_PEMILIH = "Pemilih berkas tidak ditemukan di ponsel ini."
    const val GAGAL_PUTAR = "Suara ini tidak bisa diputar. Filenya mungkin sudah dihapus."

    /** Short kind of sound, so the user can tell a system tone from their own music. */
    fun jenis(suara: SuaraPengingat): String = when (suara) {
        SuaraPengingat.Bawaan -> "Suara notifikasi bawaan"
        is SuaraPengingat.Aplikasi -> "Nada dari aplikasi"
        is SuaraPengingat.Sistem -> "Nada sistem"
        is SuaraPengingat.Berkas -> "Musik dari ponselmu"
    }
}

/** Why a picked music file was refused, before anything is copied. */
enum class AlasanTolakBerkas { BUKAN_AUDIO, KOSONG, TERLALU_BESAR }

object BatasBerkasNada {
    /** Copying a whole album into the media store is not what this is for. */
    const val MAKS_BYTE = 15L * 1024 * 1024

    private val EKSTENSI_MIME = mapOf(
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "wav" to "audio/x-wav",
        "ogg" to "audio/ogg",
        "oga" to "audio/ogg",
        "opus" to "audio/ogg",
        "flac" to "audio/flac",
        "amr" to "audio/amr",
        "mid" to "audio/midi",
        "midi" to "audio/midi",
    )

    private fun ekstensi(nama: String): String = nama.substringAfterLast('.', "").lowercase()

    /**
     * The media type to store the copy under: the one the file provider reported when it is an audio type, otherwise
     * a guess from the extension (some providers say `application/octet-stream` for an mp3). Null means not audio.
     */
    fun mimeUntukSimpan(mime: String?, nama: String): String? {
        val m = mime?.lowercase()?.substringBefore(';')?.trim()
        if (m != null && m.startsWith("audio/")) return m
        if (m == "application/ogg") return "audio/ogg"
        if (m == null || m.isEmpty() || m == "application/octet-stream") return EKSTENSI_MIME[ekstensi(nama)]
        return null
    }

    /** [ukuran] is null when the provider does not say; the copy then counts the bytes itself. */
    fun periksa(mime: String?, nama: String, ukuran: Long?): AlasanTolakBerkas? = when {
        mimeUntukSimpan(mime, nama) == null -> AlasanTolakBerkas.BUKAN_AUDIO
        ukuran != null && ukuran <= 0L -> AlasanTolakBerkas.KOSONG
        ukuran != null && ukuran > MAKS_BYTE -> AlasanTolakBerkas.TERLALU_BESAR
        else -> null
    }

    /** A file name safe to hand to the media store: no folders, no control characters, a sensible length. */
    fun namaBerkasAman(nama: String): String {
        val akhir = nama.substringAfterLast('/').substringAfterLast('\\')
        val bersih = akhir.filter { !it.isISOControl() && it !in "<>:\"|?*" }.trim().trim('.')
        if (bersih.isEmpty()) return "nada"
        if (bersih.length <= 80) return bersih
        val ext = ekstensi(bersih).take(8)
        return if (ext.isEmpty()) bersih.take(80) else bersih.take(80 - ext.length - 1) + "." + ext
    }

    /** The title shown in the app: the file name without its extension. */
    fun judulDariNama(nama: String): String {
        val aman = namaBerkasAman(nama)
        val tanpaEkstensi = if (aman.contains('.')) aman.substringBeforeLast('.') else aman
        return SuaraPengingatCodec.bersihkanNama(tanpaEkstensi)
    }
}
