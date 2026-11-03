package id.asisten.app.domain.aktivitas

import java.time.LocalDate

/** Marks a task done or open. The completion time is recorded only while it is done. */
fun Tugas.denganSelesai(selesai: Boolean, sekarang: Long): Tugas =
    copy(selesai = selesai, selesaiPada = if (selesai) sekarang else null)

fun Tujuan.denganSelesai(selesai: Boolean, sekarang: Long): Tujuan =
    copy(selesai = selesai, selesaiPada = if (selesai) sekarang else null)

object FormatAktivitas {
    /** Short due-date text for a task row. */
    fun tenggat(tugas: Tugas, hariIni: LocalDate): String {
        val batas = tugas.tenggat ?: return "Tanpa tenggat"
        val tanggal = id.asisten.app.domain.waktu.Waktu.formatTanggal(batas)
        return when (tugas.statusTenggat(hariIni)) {
            StatusTenggat.LEWAT -> "Terlambat ($tanggal)"
            StatusTenggat.HARI_INI -> "Hari ini"
            StatusTenggat.BESOK -> "Besok ($tanggal)"
            StatusTenggat.MENDATANG -> tanggal
            StatusTenggat.TANPA -> "Tanpa tenggat"
        }
    }

    fun persen(kemajuan: Float): Int = (kemajuan.coerceIn(0f, 1f) * 100f + 0.5f).toInt()
}
