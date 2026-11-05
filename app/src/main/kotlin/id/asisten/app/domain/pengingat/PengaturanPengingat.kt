package id.asisten.app.domain.pengingat

/** NORMAL = reminders behave as configured. FOKUS and ISTIRAHAT are timed and expire by themselves. */
enum class ModePengingat { NORMAL, FOKUS, ISTIRAHAT }

/**
 * Reminder settings. All times of day are minutes since midnight, durations are minutes.
 * Values read from storage go through [normalisasi] so corrupt or out-of-range data can never break scheduling.
 */
data class PengaturanPengingat(
    val aktif: Boolean = true,
    /** Minutes before an activity starts (0 = exactly at the start). */
    val menitSebelumKegiatan: Int = 10,
    val tugasAktif: Boolean = true,
    /** Time of day of the reminder on the due date. */
    val menitTugasHariH: Int = 7 * 60,
    /** Time of day of the reminder on the day before the due date. */
    val menitTugasSebelum: Int = 18 * 60,
    val sekolahAktif: Boolean = true,
    /** Minutes before school entry. */
    val menitSebelumSekolah: Int = 60,
    val tundaMenit: Int = 10,
    val jamTenangAktif: Boolean = false,
    val jamTenangMulai: Int = 22 * 60,
    val jamTenangSelesai: Int = 6 * 60,
    val mode: ModePengingat = ModePengingat.NORMAL,
    /** Epoch millis when [mode] ends. */
    val modeSampai: Long = 0L,
) {
    /** The mode that is really active now: a timed mode that has expired counts as NORMAL. */
    fun modeEfektif(sekarang: Long): ModePengingat =
        if (mode != ModePengingat.NORMAL && sekarang < modeSampai) mode else ModePengingat.NORMAL

    /** Whole minutes left in the active mode, rounded up; 0 when no mode is active. */
    fun sisaModeMenit(sekarang: Long): Long =
        if (modeEfektif(sekarang) == ModePengingat.NORMAL) 0L else (modeSampai - sekarang + 59_999L) / 60_000L

    fun mulaiMode(baru: ModePengingat, durasiMenit: Int, sekarang: Long): PengaturanPengingat =
        if (baru == ModePengingat.NORMAL || durasiMenit <= 0) akhiriMode()
        else copy(mode = baru, modeSampai = sekarang + durasiMenit * 60_000L)

    fun akhiriMode(): PengaturanPengingat = copy(mode = ModePengingat.NORMAL, modeSampai = 0L)

    fun normalisasi(): PengaturanPengingat = copy(
        menitSebelumKegiatan = menitSebelumKegiatan.coerceIn(0, BATAS_SEBELUM),
        menitTugasHariH = menitTugasHariH.coerceIn(0, MENIT_SEHARI - 1),
        menitTugasSebelum = menitTugasSebelum.coerceIn(0, MENIT_SEHARI - 1),
        menitSebelumSekolah = menitSebelumSekolah.coerceIn(0, BATAS_SEBELUM),
        tundaMenit = tundaMenit.coerceIn(1, 120),
        jamTenangMulai = jamTenangMulai.coerceIn(0, MENIT_SEHARI - 1),
        jamTenangSelesai = jamTenangSelesai.coerceIn(0, MENIT_SEHARI - 1),
        modeSampai = if (mode == ModePengingat.NORMAL) 0L else modeSampai.coerceAtLeast(0L),
    )

    companion object {
        const val MENIT_SEHARI = 24 * 60

        /** Largest lead time; the schedule logic relies on reminders never being more than a day early. */
        const val BATAS_SEBELUM = 12 * 60
    }
}

/** The choices offered in the UI (every value is within the ranges enforced by [PengaturanPengingat.normalisasi]). */
object PilihanPengingat {
    val SEBELUM_KEGIATAN = listOf(0, 5, 10, 15, 30, 60)
    val SEBELUM_SEKOLAH = listOf(30, 45, 60, 90)
    val TUNDA = listOf(5, 10, 15, 30)
    val DURASI_FOKUS = listOf(25, 45, 60, 90)
    val DURASI_ISTIRAHAT = listOf(15, 30, 60, 120)
}

/** Helpers for the time-of-day stepper in the settings screen. */
object GeserMenit {
    private const val LANGKAH = 30

    /** Moves [nilai] by [delta] minutes and wraps around midnight. */
    fun geser(nilai: Int, delta: Int): Int {
        val sehari = PengaturanPengingat.MENIT_SEHARI
        return ((nilai + delta) % sehari + sehari) % sehari
    }

    fun maju(nilai: Int): Int = geser(nilai, LANGKAH)

    fun mundur(nilai: Int): Int = geser(nilai, -LANGKAH)

    /** "HH:mm" */
    fun format(menit: Int): String {
        val m = menit.coerceIn(0, PengaturanPengingat.MENIT_SEHARI - 1)
        return (m / 60).toString().padStart(2, '0') + ":" + (m % 60).toString().padStart(2, '0')
    }
}
