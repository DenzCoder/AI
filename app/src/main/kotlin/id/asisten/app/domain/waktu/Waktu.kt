package id.asisten.app.domain.waktu

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

/** Indonesian day names, independent of the device locale. */
object NamaHari {
    private val nama = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    fun untuk(hari: DayOfWeek): String = nama[hari.value - 1]
    fun singkat(hari: DayOfWeek): String = untuk(hari).take(3)
}

object NamaBulan {
    private val nama = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )
    fun untuk(bulan: Month): String = nama[bulan.value - 1]
}

object Waktu {
    private val POLA_JAM = Regex("^\\s*(\\d{1,2})[:.](\\d{2})\\s*$")
    private val POLA_TANGGAL = Regex("^\\s*(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})\\s*$")

    /** Accepts "H:mm", "HH:mm", "H.mm" and "HH.mm". Returns null for anything invalid. */
    fun parseJam(teks: String): LocalTime? {
        val cocok = POLA_JAM.matchEntire(teks) ?: return null
        val jam = cocok.groupValues[1].toInt()
        val menit = cocok.groupValues[2].toInt()
        if (jam !in 0..23 || menit !in 0..59) return null
        return LocalTime.of(jam, menit)
    }

    fun formatJam(waktu: LocalTime): String =
        waktu.hour.toString().padStart(2, '0') + ":" + waktu.minute.toString().padStart(2, '0')

    /** Accepts "d-M-yyyy" or "d/M/yyyy". Returns null for invalid dates such as 31-02-2026. */
    fun parseTanggal(teks: String): LocalDate? {
        val cocok = POLA_TANGGAL.matchEntire(teks) ?: return null
        val (h, b, t) = cocok.destructured
        return runCatching { LocalDate.of(t.toInt(), b.toInt(), h.toInt()) }.getOrNull()
    }

    fun formatTanggal(tanggal: LocalDate): String =
        tanggal.dayOfMonth.toString().padStart(2, '0') + "-" +
            tanggal.monthValue.toString().padStart(2, '0') + "-" + tanggal.year

    fun tanggalPanjang(tanggal: LocalDate): String =
        "${NamaHari.untuk(tanggal.dayOfWeek)}, ${tanggal.dayOfMonth} ${NamaBulan.untuk(tanggal.month)} ${tanggal.year}"

    fun rentang(mulai: LocalTime, selesai: LocalTime): String = "${formatJam(mulai)}–${formatJam(selesai)}"
}
