package id.asisten.app.domain.jadwal

import id.asisten.app.domain.waktu.NamaBulan
import id.asisten.app.domain.waktu.Waktu
import java.time.LocalDate
import java.time.YearMonth

enum class ModeKalender { HARI, MINGGU, BULAN }

object NavigasiKalender {

    /** Moves [tanggal] by one day, week or month depending on [mode]. [arah] is -1 (back) or +1 (forward). */
    fun geser(tanggal: LocalDate, mode: ModeKalender, arah: Int): LocalDate = when (mode) {
        ModeKalender.HARI -> tanggal.plusDays(arah.toLong())
        ModeKalender.MINGGU -> tanggal.plusWeeks(arah.toLong())
        ModeKalender.BULAN -> tanggal.plusMonths(arah.toLong())
    }

    fun label(tanggal: LocalDate, mode: ModeKalender): String = when (mode) {
        ModeKalender.HARI -> Waktu.tanggalPanjang(tanggal)
        ModeKalender.MINGGU -> {
            val minggu = Kalender.hariDalamMinggu(tanggal)
            val awal = minggu.first()
            val akhir = minggu.last()
            "${awal.dayOfMonth} ${NamaBulan.untuk(awal.month)} – ${akhir.dayOfMonth} ${NamaBulan.untuk(akhir.month)} ${akhir.year}"
        }
        ModeKalender.BULAN -> {
            val bulan = YearMonth.from(tanggal)
            "${NamaBulan.untuk(bulan.month)} ${bulan.year}"
        }
    }
}
